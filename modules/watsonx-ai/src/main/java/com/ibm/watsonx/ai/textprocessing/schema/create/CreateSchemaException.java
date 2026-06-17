/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

/**
 * Exception thrown when an error occurs during create schema operations.
 */
public class CreateSchemaException extends RuntimeException {

    private final String code;

    /**
     * Constructs a new CreateSchemaException with the specified error code and message.
     *
     * @param code the error code
     * @param message the error message
     */
    public CreateSchemaException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Constructs a new CreateSchemaException with the specified error code, message, and cause.
     *
     * @param code the error code
     * @param message the error message
     * @param cause the cause of the exception
     */
    public CreateSchemaException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return the error code
     */
    public String code() {
        return code;
    }

    @Override
    public String toString() {
        return "CreateSchemaException [code=%s, message=%s]".formatted(code, getMessage());
    }
}
