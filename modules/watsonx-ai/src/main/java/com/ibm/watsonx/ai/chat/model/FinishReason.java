/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

/**
 * Represents the reason why a chat or completion generation stopped.
 */
public enum FinishReason {

    /**
     * The model reached a natural stopping point or encountered a provided stop sequence.
     */
    STOP("stop"),

    /**
     * The maximum number of tokens specified in the request was reached.
     */
    LENGTH("length"),

    /**
     * The model decided to call an external tool.
     */
    TOOL_CALLS("tool_calls"),

    /**
     * The generation stopped because the allowed time limit was reached.
     */
    TIME_LIMIT("time_limit"),

    /**
     * The request was cancelled by the client before completion.
     */
    CANCELLED("cancelled"),

    /**
     * An error occurred during the generation process.
     */
    ERROR("error"),

    /**
     * The API response is still in progress.
     */
    INCOMPLETE(null);

    private final String value;

    FinishReason(String value) {
        this.value = value;
    }

    /**
     * Returns the string value used by the API for this finish reason.
     *
     * @return the string value
     */
    public String value() {
        return value;
    }

    /**
     * Maps a string value returned by the API to the corresponding {@link FinishReason} enum.
     *
     * @param value the finish reason value
     * @return the matching {@link FinishReason}
     */
    public static FinishReason fromValue(String value) {
        return value == null
            ? INCOMPLETE
            : switch(value) {
                case "stop" -> STOP;
                case "length" -> LENGTH;
                case "tool_calls" -> TOOL_CALLS;
                case "time_limit" -> TIME_LIMIT;
                case "cancelled" -> CANCELLED;
                case "error" -> ERROR;
                default -> throw new IllegalArgumentException("Unknown finish reason: " + value);
            };
    }
}

