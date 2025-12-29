/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Enum representing the OCR modes to be used during text extraction.
 */
public enum OcrMode {
    DISABLED("disabled"),
    ENABLED("enabled"),
    FORCED("forced"),
    AUTO("");

    private final String value;

    OcrMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
