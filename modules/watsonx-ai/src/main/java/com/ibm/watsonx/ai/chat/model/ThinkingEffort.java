/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

/**
 * Represents the level of computational effort that the model should apply when performing thinking tasks.
 */
public enum ThinkingEffort {

    /**
     * Minimal thinking effort.
     */
    LOW("low"),

    /**
     * Moderate thinking effort.
     */
    MEDIUM("medium"),

    /**
     * Maximum thinking effort.
     */
    HIGH("high");

    private final String value;

    private ThinkingEffort(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    };
}
