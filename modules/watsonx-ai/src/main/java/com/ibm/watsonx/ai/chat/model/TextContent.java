/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;

/**
 * Represents a user-supplied text content used in chat interactions.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * TextContent.of("Hello, how can I help you?");
 * }</pre>
 *
 * @param type the type identifier for this content, always set to {@code text}
 * @param text the textual content provided by the user
 */
public record TextContent(String type, String text) implements UserContent {

    public static final String TYPE = "text";

    public TextContent {
        type = TYPE;
        requireNonNull(text);
    }

    /**
     * Create a new {@code TextContent} instance.
     *
     * @param text the text content to include
     * @return a new {@code TextContent} instance
     */
    public static TextContent of(String text) {
        return new TextContent(TYPE, text);
    }

    @Override
    public String toString() {
        return text;
    }
}
