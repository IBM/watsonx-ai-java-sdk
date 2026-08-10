/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

/**
 * Represents the response for a generic detection request.
 */
public abstract class BaseDetectionResponse {
    /**
     * The detected text.
     */
    protected final String text;
    /**
     * The type of detection performed.
     */
    protected final String detectionType;
    /**
     * The detection result.
     */
    protected final String detection;
    /**
     * The confidence score of the detection.
     */
    protected final double score;

    /**
     * Creates a new detection response.
     *
     * @param text the detected text
     * @param detectionType the type of detection performed
     * @param detection the detection result
     * @param score the confidence score of the detection
     */
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((detectionType == null) ? 0 : detectionType.hashCode());
        result = prime * result + ((detection == null) ? 0 : detection.hashCode());
        temp = Double.doubleToLongBits(score);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BaseDetectionResponse other = (BaseDetectionResponse) obj;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        if (detectionType == null) {
            if (other.detectionType != null)
                return false;
        } else if (!detectionType.equals(other.detectionType))
            return false;
        if (detection == null) {
            if (other.detection != null)
                return false;
        } else if (!detection.equals(other.detection))
            return false;
        if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BaseDetectionResponse [text=" + text + ", detectionType=" + detectionType + ", detection=" + detection + ", score=" + score + "]";
    }
}
