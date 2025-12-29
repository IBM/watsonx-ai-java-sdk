/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

/**
 * Exception thrown when a text extraction operation fails.
 */
public final class TextExtractionException extends Exception {

    final String code;

    public TextExtractionException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public TextExtractionException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return the error code
     */
    public String code() {
        return code;
    }

    @Override
    public String toString() {
        return "TextExtractionException [code=" + code + ", message=" + getMessage() + "]";
    }
}
