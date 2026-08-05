/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import com.ibm.watsonx.ai.chat.model.FinishReason;

/**
 * Exception thrown when a successful chat request returns a response without any usable output.
 */
public final class EmptyChatResponseException extends RuntimeException {

    /** Value returned by {@link #index()} when the response contains no choices at all. */
    public static final int NO_CHOICE = -1;

    private final ChatResponse response;
    private final FinishReason finishReason;
    private final int index;

    /**
     * Constructs a new {@code EmptyChatResponseException}.
     *
     * @param message the detail message explaining the exception
     * @param response the chat response that produced no usable output
     * @param finishReason the finish reason of the empty choice
     * @param index the index of the empty choice, or {@link #NO_CHOICE} if the response contains no choices
     */
    public EmptyChatResponseException(String message, ChatResponse response, FinishReason finishReason, int index) {
        super(message);
        this.response = response;
        this.finishReason = finishReason;
        this.index = index;
    }

    /**
     * Returns the chat response that produced no usable output.
     *
     * @return the {@link ChatResponse}
     */
    public ChatResponse response() {
        return response;
    }

    /**
     * Returns the finish reason of the empty choice.
     *
     * @return the {@link FinishReason} of the empty choice, or {@link FinishReason#INCOMPLETE} if the response contains no choices
     */
    public FinishReason finishReason() {
        return finishReason;
    }

    /**
     * Returns the index of the empty choice.
     *
     * @return the index of the empty choice, or {@link #NO_CHOICE} if the response contains no choices
     */
    public int index() {
        return index;
    }
}
