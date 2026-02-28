/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import java.util.stream.Stream;

/**
 * Enum representing the possible status of requested outputs for the batch processing.
 */
public enum Status {

    VALIDATING("validating"),
    IN_PROGRESS("in_progress"),
    FINALIZING("finalizing"),
    COMPLETED("completed"),
    FAILED("failed");

    private String value;

    Status(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Status fromValue(String value) {
        return Stream.of(Status.values())
            .filter(status -> status.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown Type value: " + value));
    }
}
