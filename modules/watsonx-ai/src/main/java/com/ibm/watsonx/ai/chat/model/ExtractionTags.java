/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts reasoning and response content from LLM outputs that use custom delimiters or tags.
 * <p>
 * This class supports models that structure their output with distinct sections for internal reasoning (thinking) and final responses. Different LLMs
 * use different tag formats:
 * <ul>
 * <li><b>Granite models</b>: Use XML-like tags {@code <think>...</think>} and {@code <response>...</response>}</li>
 * <li><b>Gemma-4 models</b>: Use custom delimiters {@code <|channel>...<channel|>} for thinking</li>
 * <li><b>Other models</b>: May use their own custom tag formats</li>
 * </ul>
 *
 * @see Think
 * @see Response
 */
public final class ExtractionTags {

    /**
     * Represents the opening and closing delimiters for the thinking/reasoning section.
     *
     * @param opening the opening delimiter/tag for the thinking section
     * @param closing the closing delimiter/tag for the thinking section
     */
    public record Think(String opening, String closing) {}

    /**
     * Represents the opening and closing delimiters for the response section.
     * <p>
     * If not specified, the response is considered to be all content outside the thinking section.
     *
     * @param opening the opening delimiter/tag for the response section
     * @param closing the closing delimiter/tag for the response section
     */
    public record Response(String opening, String closing) {}

    private final Think think;
    private final Response response;

    /**
     * Creates an ExtractionTags instance with custom opening and closing delimiters.
     *
     * @param think the think tag with custom opening and closing delimiters (required)
     * @param response the response tag with custom opening and closing delimiters (optional, can be null)
     */
    public ExtractionTags(Think think, Response response) {
        this.think = requireNonNull(think, "think tag must not be null");
        this.response = response;
    }

    /**
     * Factory method to create an ExtractionTags instance with both thinking and response tags.
     *
     * @param think the think tag with custom opening and closing delimiters (required)
     * @param response the response tag with custom opening and closing delimiters (optional, can be null)
     * @return a new ExtractionTags instance
     */
    public static ExtractionTags of(Think think, Response response) {
        return new ExtractionTags(think, response);
    }

    /**
     * Factory method to create an ExtractionTags instance with only a thinking tag.
     * <p>
     * The response will be considered as all content outside the thinking section.
     *
     * @param think the think tag with custom opening and closing delimiters (required)
     * @return a new ExtractionTags instance
     */
    public static ExtractionTags of(Think think) {
        return of(think, null);
    }

    /**
     * Returns the think tag configuration.
     *
     * @return the Think containing opening and closing delimiters
     */
    public Think think() {
        return think;
    }

    /**
     * Returns the response tag configuration.
     *
     * @return the Response containing opening and closing delimiters, or null if not specified
     */
    public Response response() {
        return response;
    }

    /**
     * Extracts the response part from the given content string.
     *
     * @param content the full structured content to parse
     * @return the extracted response, or {@code null} if no match is found
     */
    public String extractResponse(String content) {

        String regex = "(?<=" + Pattern.quote(think.closing()) + ")\\s*";
        Pattern pattern = isNull(response)
            ? Pattern.compile(regex.concat("(.*)"), Pattern.DOTALL)
            : Pattern.compile(regex.concat(Pattern.quote(response.opening())).concat("(.*)").concat(Pattern.quote(response.closing())),
                Pattern.DOTALL);

        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * Extracts the reasoning part from the given content string.
     *
     * @param content the full structured content to parse
     * @return the extracted reasoning, or {@code null} if no match is found
     */
    public String extractThinking(String content) {

        String regex = Pattern.quote(think.opening()) + "(.*?)" + Pattern.quote(think.closing());
        if (nonNull(response))
            regex += ".*" + Pattern.quote(response.opening());

        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String stripBrackets(String tag) {
        if (isNull(tag))
            return null;

        if (tag.startsWith("<"))
            tag = tag.substring(1);

        if (tag.endsWith(">"))
            tag = tag.substring(0, tag.length() - 1);

        return tag;
    }
}