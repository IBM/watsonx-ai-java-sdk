/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.exception;

/**
 * Exception thrown when JSON serialization or deserialization fails.
 */
public final class JsonException extends RuntimeException {

    /**
     * Constructs a new {@code JsonException} with the specified detail message.
     *
     * @param message the detail message explaining the exception
     */
    public JsonException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code JsonException} with the specified detail message and cause.
     *
     * @param message the detail message explaining the exception
     * @param cause the underlying cause of the failure
     */
    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
