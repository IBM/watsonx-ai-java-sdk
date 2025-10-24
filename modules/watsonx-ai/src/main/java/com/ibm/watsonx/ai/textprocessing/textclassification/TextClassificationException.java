/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

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

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "TextClassificationException [code=" + code + ", message=" + getMessage() + "]";
    }
}
