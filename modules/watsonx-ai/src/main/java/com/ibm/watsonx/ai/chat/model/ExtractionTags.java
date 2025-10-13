/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents XML-like tag names used to segment an assistant's output into two logical parts:
 * <ul>
 * <li><b>think</b>: the tag that wraps the assistant's internal reasoning</li>
 * <li><b>response</b>: the tag that wraps the final answer (optional)</li>
 * </ul>
 * <p>
 * This class is intended for models that return both reasoning and response content within a single text string. It allows manual extraction of the
 * reasoning and response segments based on the provided tags.
 * <p>
 * <b>Example:</b>
 *
 * <pre>{@code
 * // Explicitly setting both tags
 * ExtractionTags tags = ExtractionTags.of("think", "response");
 *
 * // Setting only the reasoning tag — the response will be considered as all content outside the <think>...</think> section
 * ExtractionTags tagsDefaultResponse = ExtractionTags.of("think");
 * }</pre>
 *
 * @param think the XML-like tag name representing the reasoning section (required, without angle brackets)
 * @param response the XML-like tag name representing the final response section (optional, without angle brackets)
 *
 */
public record ExtractionTags(String think, String response) {

    public ExtractionTags(String think, String response) {
        this.think = stripBrackets(requireNonNull(think));
        this.response = stripBrackets(response);
    }

    public ExtractionTags(String think) {
        this(think, null);
    }

    public static ExtractionTags of(String think, String response) {
        return new ExtractionTags(think, response);
    }

    public static ExtractionTags of(String think) {
        return new ExtractionTags(think);
    }

    /**
     * Extracts the response part from the given content string.
     * <p>
     * If a response tag is defined, the method searches for content enclosed by the response tag.
     * <p>
     * If no response tag is defined, it removes the reasoning section (enclosed in the {@code think} tag) and returns everything outside it as the
     * response.
     *
     * @param content the full structured content to parse
     * @return the extracted response, or {@code null} if no match is found
     */
    public String extractResponse(String content) {

        if (isNull(response))
            return content.replaceAll("<" + think + ">.*?</" + think + ">", "").trim();

        String regex = "(?<=</" + think + ">)\\s*<" + response + ">(.*)</" + response + ">";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * Extracts the reasoning part (content enclosed in the {@code think} tag) from the given content string.
     *
     * @param content the full structured content to parse
     * @return the extracted reasoning, or {@code null} if no match is found
     */
    public String extractThinking(String content) {

        String regex = "<" + think + ">(.*?)</" + think + ">";
        if (nonNull(response))
            regex += ".*<" + response + ">";

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
