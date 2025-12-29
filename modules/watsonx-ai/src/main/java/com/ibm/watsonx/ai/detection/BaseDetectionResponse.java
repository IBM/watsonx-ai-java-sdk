/*
 * Copyright 2025 IBM Corporation
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

    /**
     * Returns the detected text.
     *
     * @return the text
     */
    public String text() {
        return text;
    }

    /**
     * Returns the type of detection performed.
     *
     * @return the detection type
     */
    public String detectionType() {
        return detectionType;
    }

    /**
     * Returns the detection result.
     *
     * @return the detection result
     */
    public String detection() {
        return detection;
    }

    /**
     * Returns the confidence score of the detection.
     *
     * @return the score
     */
    public double score() {
        return score;
    }
}
