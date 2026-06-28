/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;

/**
 * This class tracks the parsing state of a streamed chat response, determining whether the parser is in a {@code reasoning} or {@code response} state
 * or outside any known tags. It processes incoming text chunks, identifies opening and closing delimiters, and updates the current state accordingly.
 */
public final class StreamingStateTracker {

    /**
     * Represents the output of a single {@code StreamingStateTracker.update(String)} call.
     *
     * @param state the parsing state after processing the chunk
     * @param content the text content found inside of recognized tags, if any
     */
    public record Result(State state, Optional<String> content) {}

    private final String THINKING_START_TAG;
    private final String THINKING_CLOSE_TAG;
    private final String RESPONSE_START_TAG;
    private final String RESPONSE_CLOSE_TAG;
    private final ExtractionTags extractionTags;
    private final StringBuilder tagBuffer;
    private final StringBuilder textBuffer;
    private State currentState = State.START;

    /**
     * Creates a new {@code StreamingStateTracker} with the specified tag definitions.
     *
     * @param extractionTags the tag names to be recognized
     */
    public StreamingStateTracker(ExtractionTags extractionTags) {
        requireNonNull(extractionTags, "extractionTags cannot be null");
        this.extractionTags = extractionTags;
        tagBuffer = new StringBuilder();
        textBuffer = new StringBuilder();
        THINKING_START_TAG = extractionTags.think().opening();
        THINKING_CLOSE_TAG = extractionTags.think().closing();
        RESPONSE_START_TAG = isNull(extractionTags.response()) ? null : extractionTags.response().opening();
        RESPONSE_CLOSE_TAG = isNull(extractionTags.response()) ? null : extractionTags.response().closing();
    }

    /**
     * Processes the next streamed text chunk and updates the parsing state.
     * <p>
     * The parser accumulates characters into a buffer and performs prefix matching against all known delimiters. As soon as the buffer no longer
     * matches any known delimiter prefix, the non-matching portion is flushed to the text output. When an exact delimiter match is found, the state
     * is updated accordingly.
     *
     * @param chunk the incoming streamed text
     * @return a {@link Result} containing the updated state and any content extracted from the current state
     */
    public Result update(String chunk) {
        if (chunk.isEmpty())
            return new Result(currentState, Optional.empty());

        chunk = decodeUnicodeSymbols(chunk);

        if (currentState == State.NO_THINKING)
            return new Result(currentState, Optional.of(chunk));

        for (int i = 0; i < chunk.length(); i++) {
            char c = chunk.charAt(i);
            tagBuffer.append(c);
            String partial = tagBuffer.toString();

            // Check if the buffer exactly matches a known delimiter
            String exactMatch = findExactMatch(partial);
            if (nonNull(exactMatch)) {
                handleCompleteTag(exactMatch);
                tagBuffer.setLength(0);
                continue;
            }

            // Check if the buffer is still a prefix of any known delimiter
            if (isPrefixOfAny(partial)) {
                // Keep accumulating, we might be in the middle of a delimiter
                continue;
            }

            // Buffer no longer matches any delimiter prefix, flush all but the last char
            // The last char could be the start of a new delimiter, so re-evaluate it
            String toFlush = tagBuffer.substring(0, tagBuffer.length() - 1);
            textBuffer.append(toFlush);
            tagBuffer.setLength(0);
            tagBuffer.append(c);

            // If even the single char isn't a prefix of any delimiter, flush it too
            if (!isPrefixOfAny(String.valueOf(c))) {
                textBuffer.append(c);
                tagBuffer.setLength(0);

                // If we're still in START and nothing matched, switch to NO_THINKING
                if (currentState == State.START) {
                    currentState = State.NO_THINKING;
                    // Flush the remaining chunk directly as content
                    if (i + 1 < chunk.length()) {
                        String remaining = chunk.substring(i + 1);
                        String accumulated = textBuffer.toString();
                        textBuffer.setLength(0);
                        return new Result(currentState, Optional.of(accumulated + remaining));
                    }
                }
            }
        }

        // If we are still in START state and the buffer does not match the start of the thinking tag,
        // we can conclude there is no thinking section
        if (currentState == State.START && !isPrefixOfAny(tagBuffer.toString())) {
            currentState = State.NO_THINKING;
        }

        String textOut = textBuffer.toString();
        textBuffer.setLength(0);
        return new Result(currentState, textOut.isEmpty() ? Optional.empty() : Optional.of(textOut));
    }

    /**
     * Handles a fully matched delimiter by transitioning to the appropriate {@link State}.
     *
     * @param tag the complete delimiter string that was matched
     */
    private void handleCompleteTag(String tag) {
        if (tag.equals(THINKING_START_TAG)) {
            currentState = State.THINKING;
        } else if (tag.equals(THINKING_CLOSE_TAG)) {
            currentState = isNull(extractionTags.response()) ? State.RESPONSE : State.UNKNOWN;
        } else if (tag.equals(RESPONSE_START_TAG)) {
            currentState = State.RESPONSE;
        } else if (nonNull(RESPONSE_CLOSE_TAG) && tag.equals(RESPONSE_CLOSE_TAG)) {
            currentState = State.UNKNOWN;
        }
    }

    /**
     * Returns the delimiter that exactly matches the given string in the current state, or {@code null} if none match.
     * <p>
     * Only delimiters that are meaningful in the current state are considered:
     * <ul>
     * <li>{@link State#START} or {@link State#UNKNOWN}: only the thinking start tag</li>
     * <li>{@link State#THINKING}: only the thinking close tag</li>
     * <li>{@link State#RESPONSE}: only the response close tag</li>
     * </ul>
     *
     * @param s the string to check
     * @return the matching delimiter, or {@code null}
     */
    private String findExactMatch(String s) {
        return switch(currentState) {
            case START, UNKNOWN -> {
                if (s.equals(THINKING_START_TAG))
                    yield THINKING_START_TAG;
                if (nonNull(RESPONSE_START_TAG) && s.equals(RESPONSE_START_TAG))
                    yield RESPONSE_START_TAG;
                yield null;
            }
            case THINKING -> s.equals(THINKING_CLOSE_TAG) ? THINKING_CLOSE_TAG : null;
            case RESPONSE -> nonNull(RESPONSE_CLOSE_TAG) && s.equals(RESPONSE_CLOSE_TAG) ? RESPONSE_CLOSE_TAG : null;
            case NO_THINKING -> null;
        };
    }

    /**
     * Returns {@code true} if the given string is a prefix of at least one delimiter meaningful in the current state.
     *
     * @param prefix the string to check
     * @return {@code true} if it is a valid prefix of any relevant delimiter
     */
    private boolean isPrefixOfAny(String prefix) {
        if (prefix == null || prefix.isEmpty())
            return false;
        return switch(currentState) {
            case START, UNKNOWN -> isPrefix(prefix, THINKING_START_TAG) || isPrefix(prefix, RESPONSE_START_TAG);
            case THINKING -> isPrefix(prefix, THINKING_CLOSE_TAG);
            case RESPONSE -> isPrefix(prefix, RESPONSE_CLOSE_TAG);
            case NO_THINKING -> false;
        };
    }

    private boolean isPrefix(String prefix, String candidate) {
        return nonNull(candidate) && candidate.startsWith(prefix);
    }

    private String decodeUnicodeSymbols(String s) {
        return s.replace("\\u003c", "<")
            .replace("\\u003e", ">");
    }

    /**
     * Represents the current parsing state.
     */
    public enum State {

        /** The LLM is not producing any output */
        START,

        /** The LLM is producing reasoning */
        THINKING,

        /** The LLM is producing the final response */
        RESPONSE,

        /** The LLM is producing output, without thinking */
        NO_THINKING,

        UNKNOWN
    }
}