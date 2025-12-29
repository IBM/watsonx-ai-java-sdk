/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import java.util.stream.Stream;

/**
 * Enum representing the possible status of requested outputs for text processing.
 */
public enum Status {

    SUBMITTED("submitted"),
    UPLOADING("uploading"),
    RUNNING("running"),
    DOWNLOADING("downloading"),
    DOWNLOADED("downloaded"),
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
