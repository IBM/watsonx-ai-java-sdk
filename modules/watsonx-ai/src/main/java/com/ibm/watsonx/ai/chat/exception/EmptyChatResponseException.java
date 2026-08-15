/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.exception;

import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.FinishReason;

/**
 * Exception thrown when a successful chat request returns a response without any usable output.
 */
public final class EmptyChatResponseException extends RuntimeException {

    /**
     * Value returned by {@link #index()} when the response contains no choices at all.
     */
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((response == null) ? 0 : response.hashCode());
        result = prime * result + ((finishReason == null) ? 0 : finishReason.hashCode());
        result = prime * result + index;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EmptyChatResponseException other = (EmptyChatResponseException) obj;
        if (response == null) {
            if (other.response != null)
                return false;
        } else if (!response.equals(other.response))
            return false;
        if (finishReason != other.finishReason)
            return false;
        if (index != other.index)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EmptyChatResponseException [response=" + response + ", finishReason=" + finishReason + ", index=" + index + "]";
    }
}
