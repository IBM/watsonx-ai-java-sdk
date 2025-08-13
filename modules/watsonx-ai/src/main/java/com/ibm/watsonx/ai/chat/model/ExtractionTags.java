/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;

/**
 * Holds the XML-like tag names used to segment the assistant's output into reasoning and final response parts.
 * <p>
 * Typically used with methods such as {@code toTextByTags(java.util.Set)} to extract content from specific sections (e.g., {@code <think>} and
 * {@code <response>}).
 * <p>
 * If no specific response tag is provided, the default {@code "root"} tag will be used, which corresponds to the text nodes directly under the root
 * element of the parsed XML.
 * <p>
 * Example:
 *
 * <pre>{@code
 * // Explicitly setting both tags
 * ExtractionTags tags = new ExtractionTags("think", "response");
 *
 * // Setting only the reasoning tag — response will default to "root"
 * ExtractionTags tagsDefaultResponse = new ExtractionTags("think");
 * }</pre>
 *
 * @param think the tag name representing the model's reasoning section (without angle brackets)
 * @param response the tag name representing the model's final response section (without angle brackets), defaults to {@code "root"} if not set
 */
public record ExtractionTags(String think, String response) {

    public ExtractionTags(String think, String response) {
        this.think = requireNonNull(think);
        this.response = requireNonNull(response);
    }

    public ExtractionTags(String think) {
        this(think, "root");
    }
}
