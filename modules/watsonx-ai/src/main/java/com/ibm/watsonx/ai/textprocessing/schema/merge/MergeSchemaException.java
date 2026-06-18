/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

/**
 * Exception thrown when an error occurs during merge schema operations.
 */
public class MergeSchemaException extends RuntimeException {

    private final String code;

    /**
     * Constructs a new MergeSchemaException with the specified error code and message.
     *
     * @param code the error code
     * @param message the error message
     */
    public MergeSchemaException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Constructs a new MergeSchemaException with the specified error code, message, and cause.
     *
     * @param code the error code
     * @param message the error message
     * @param cause the cause of the exception
     */
    public MergeSchemaException(String code, String message, Throwable cause) {
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
        return "MergeSchemaException [code=%s, message=%s]".formatted(code, getMessage());
    }
}
