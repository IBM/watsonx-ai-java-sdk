/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;

/**
 * Represents a system message used to set behavior or provide instructions to the assistant before the conversation begins.
 * <p>
 * The system message is authored by the system and typically used for priming the assistant with context or personality.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * SystemMessage.of("You are a helpful assistant.");
 * }</pre>
 *
 * @param role the role of the message's author, always {@code system}
 * @param content the content of the system message
 * @param name an optional name to differentiate between participants with the same role
 */
public final record SystemMessage(String role, String content, String name) implements ChatMessage {

    public static final String ROLE = "system";

    public SystemMessage {
        role = ROLE;
        requireNonNull(content);
    }

    /**
     * Creates a new {@link SystemMessage}.
     *
     * @param content the content of the system message
     * @param name optional participant name
     * @return a new {@link SystemMessage}
     */
    public static SystemMessage of(String content, String name) {
        return new SystemMessage(ROLE, content, name);
    }

    /**
     * Creates a new {@link SystemMessage}.
     *
     * @param content the content of the system message
     * @return a new {@link SystemMessage}
     */
    public static SystemMessage of(String content) {
        return of(content, null);
    }
}