/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;

/**
 * Represents a developer message used to set behavior or provide instructions to the assistant, replacing the system message on the models that
 * expect it.
 * <p>
 * Supported only by the Model Gateway chat APIs.
 *
 * @param role the role of the message's author, always {@code developer}
 * @param content the content of the developer message
 * @param name an optional name to differentiate between participants with the same role
 */
public record DeveloperMessage(String role, String content, String name) implements ChatMessage {

    public static final String ROLE = "developer";

    public DeveloperMessage {
        role = ROLE;
        requireNonNull(content);
    }

    /**
     * Creates a new {@link DeveloperMessage}.
     *
     * @param content the content of the developer message
     * @param name optional participant name
     * @return a new {@link DeveloperMessage}
     */
    public static DeveloperMessage of(String content, String name) {
        return new DeveloperMessage(ROLE, content, name);
    }

    /**
     * Creates a new {@link DeveloperMessage}.
     *
     * @param content the content of the developer message
     * @return a new {@link DeveloperMessage}
     */
    public static DeveloperMessage of(String content) {
        return of(content, null);
    }
}
