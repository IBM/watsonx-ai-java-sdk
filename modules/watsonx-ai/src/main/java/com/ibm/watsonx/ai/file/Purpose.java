/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

/**
 * Represents the purpose of a file uploaded to the watsonx.ai Files APIss.
 */
public enum Purpose {

    BATCH("batch");

    private final String value;

    Purpose(String value) {
        this.value = value;
    }

    /**
     * Returns the string value of this purpose.
     *
     * @return the purpose value
     */
    public String value() {
        return value;
    }
}