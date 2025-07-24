/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;

/**
 * Represents a control message authored by the system or model to influence behavior or indicate internal state.
 * <p>
 * Depending on the model, control messages can convey non-verbal signals such as {@code thinking} (e.g., in Granite reasoning models).
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * ControlMessage.of("thinking");
 * }</pre>
 *
 * @param role the role of the message's author, always {@code control}
 * @param content the content of the control message (e.g., {@code thinking})
 * @param name an optional name to differentiate between participants with the same role
 */
public record ControlMessage(String role, String content, String name) implements ChatMessage {

    public static final String ROLE = "control";

    public ControlMessage {
        role = ROLE;
        requireNonNull(content);
    }

    /**
     * Creates a new {@link ControlMessage}.
     *
     * @param content the content of the control message
     * @return a new {@link ControlMessage}
     */
    public static ControlMessage of(String content) {
        return new ControlMessage(ROLE, content, null);
    }
}