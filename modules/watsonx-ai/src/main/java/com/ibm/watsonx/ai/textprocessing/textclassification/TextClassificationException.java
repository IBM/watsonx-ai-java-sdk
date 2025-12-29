/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

/**
 * Exception thrown when a text classification operation fails.
 */
public final class TextClassificationException extends Exception {

    final String code;

    public TextClassificationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public TextClassificationException(String code, String message) {
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
        return "TextClassificationException [code=" + code + ", message=" + getMessage() + "]";
    }
}
