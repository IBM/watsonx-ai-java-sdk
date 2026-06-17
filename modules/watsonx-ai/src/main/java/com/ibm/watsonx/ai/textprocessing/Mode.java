/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Enum representing the processing modes available for a text extraction request.
 */
public enum Mode {
    STANDARD("standard"),
    HIGH_QUALITY("high_quality");

    private final String value;

    Mode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
