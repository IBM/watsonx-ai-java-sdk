/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

/**
 * Represents the response for a generic detection request.
 */
public abstract class BaseDetectionResponse {
    protected final String text;
    protected final String detectionType;
    protected final String detection;
    protected final double score;

    protected BaseDetectionResponse(String text, String detectionType, String detection, double score) {
        this.text = text;
        this.detectionType = detectionType;
        this.detection = detection;
        this.score = score;
    }

    public String text() {
        return text;
    }

    public String detectionType() {
        return detectionType;
    }

    public String detection() {
        return detection;
    }

    public double score() {
        return score;
    }
}
