/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.util;

import static java.util.Objects.requireNonNull;
import java.util.Optional;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;

/**
 * This class tracks the parsing state of a streamed chat response, determining whether the parser is in a {@code reasoning} or {@code response} state
 * or outside any known tags. It processes incoming text chunks, identifies opening and closing XML-like tags, and updates the current state
 * accordingly.
 */
public class StreamingStateTracker {

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
    private final StringBuilder tagBuffer = new StringBuilder();
    private final StringBuilder textBuffer = new StringBuilder();
    private State currentState = State.START;
    private TagParseState parseState = TagParseState.CONTENT;

    /**
     * Creates a new {@code StreamingStateTracker} with the specified tag definitions.
     *
     * @param extractionTags the tag names to be recognized
     */
    public StreamingStateTracker(ExtractionTags extractionTags) {
        requireNonNull(extractionTags, "extractionTags cannot be null");
        this.extractionTags = extractionTags;
        this.THINKING_START_TAG = "<" + extractionTags.think() + ">";
        this.THINKING_CLOSE_TAG = "</" + extractionTags.think() + ">";
        this.RESPONSE_START_TAG = "<" + extractionTags.response() + ">";
        this.RESPONSE_CLOSE_TAG = "</" + extractionTags.response() + ">";
    }

    /**
     * Processes the next streamed text chunk and updates the parsing state.
     * <p>
     * The parser detects partial or complete XML-like tags and determines whether the current position is inside a {@code reasoning} block, or
     * {@code response} block.
     *
     * @param chunk the incoming streamed text
     * @return a {@link Result} containing the updated state
     */
    public Result update(String chunk) {
        if (chunk.isEmpty())
            return new Result(currentState, Optional.empty());

        chunk = decodeUnicodeSymbols(chunk);
        if (currentState == State.NO_THINKING)
            return new Result(currentState, Optional.of(chunk));

        char[] chars = chunk.toCharArray();
        for (char c : chars) {
            switch(parseState) {
                case CONTENT -> {
                    if (c == '<') {
                        parseState = TagParseState.OPEN_TAG_START;
                        tagBuffer.setLength(0);
                        tagBuffer.append(c);
                    } else {
                        textBuffer.append(c);
                    }
                }
                case OPEN_TAG_START -> {
                    if (c == '/') {
                        parseState = TagParseState.CLOSE_TAG_START;
                        tagBuffer.append(c);
                    } else {
                        parseState = TagParseState.TAG_NAME;
                        tagBuffer.append(c);
                    }
                }
                case CLOSE_TAG_START, TAG_NAME -> {
                    tagBuffer.append(c);

                    // Verify whether the current partial tag matches the start of any known tags
                    boolean matchesAnyPrefix = THINKING_START_TAG.startsWith(tagBuffer.toString()) ||
                        THINKING_CLOSE_TAG.startsWith(tagBuffer.toString()) ||
                        RESPONSE_START_TAG.startsWith(tagBuffer.toString()) ||
                        RESPONSE_CLOSE_TAG.startsWith(tagBuffer.toString());

                    // If the tag cannot possibly match any known tag, treat it as plain text
                    if (!matchesAnyPrefix) {

                        if (currentState == State.START)
                            currentState = State.NO_THINKING;

                        parseState = TagParseState.CONTENT;
                        textBuffer.append(tagBuffer);
                        tagBuffer.setLength(0);
                        break;
                    }

                    // If tag is complete ('>'), process it
                    if (c == '>') {
                        handleCompleteTag(tagBuffer.toString());
                        tagBuffer.setLength(0);
                        parseState = TagParseState.CONTENT;
                    }
                }
            }
        }

        String partialTag = tagBuffer.toString();
        if (currentState == State.START && (partialTag.isEmpty() || partialTag.startsWith(THINKING_START_TAG)))
            currentState = State.NO_THINKING;

        String textOut = textBuffer.toString();
        textBuffer.setLength(0);
        return new Result(currentState, textOut.isEmpty() ? Optional.empty() : Optional.of(textOut));
    }

    /**
     * Handles a fully parsed tag by updating the {@link State}.
     *
     * @param tag the complete tag string
     */
    private void handleCompleteTag(String tag) {
        if (tag.equals(THINKING_START_TAG)) {
            currentState = State.THINKING;
        } else if (tag.equals(THINKING_CLOSE_TAG)) {
            if ("root".equals(extractionTags.response())) {
                currentState = State.RESPONSE;
            } else {
                currentState = State.UNKNOWN;
            }
        } else if (tag.equals(RESPONSE_START_TAG)) {
            currentState = State.RESPONSE;
        } else if (tag.equals(RESPONSE_CLOSE_TAG)) {
            currentState = State.UNKNOWN;
        }
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

    /**
     * Internal parser state for incremental tag detection.
     */
    private enum TagParseState {
        /** Currently parsing the content of a tag */
        CONTENT,

        /** Saw a {@code <} character, awaiting tag name or slash. */
        OPEN_TAG_START,

        /** Saw a {@code </}, awaiting tag name for closing tag. */
        CLOSE_TAG_START,

        /** Reading characters of a tag name (after initial {@code <} or {@code </}). */
        TAG_NAME
    }
}
