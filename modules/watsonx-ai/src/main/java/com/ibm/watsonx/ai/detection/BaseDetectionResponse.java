/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

/**
 * Represents the response for a generic detection request.
 */
abstract class BaseDetectionResponse {
    protected String text;
    protected String detectionType;
    protected String detection;
    protected double score;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDetectionType() {
        return detectionType;
    }

    public void setDetectionType(String detectionType) {
        this.detectionType = detectionType;
    }

    public String getDetection() {
        return detection;
    }

    public void setDetection(String detection) {
        this.detection = detection;
    }

    public double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
