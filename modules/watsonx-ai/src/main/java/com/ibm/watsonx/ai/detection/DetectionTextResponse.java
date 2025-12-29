/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

/**
 * Represents the response from a text content detection request.
 */
public final class DetectionTextResponse extends BaseDetectionResponse {
    private final int start;
    private final int end;

    public DetectionTextResponse(String text, String detectionType, String detection, double score, int start, int end) {
        super(text, detectionType, detection, score);
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the start position of the detected content in the input text.
     *
     * @return the start index (0-based)
     */
    public int start() {
        return start;
    }

    /**
     * Returns the end position of the detected content in the input text.
     *
     * @return the end index (0-based, exclusive)
     */
    public int end() {
        return end;
    }

    @Override
    public String toString() {
        return "DetectionTextResponse [text=" + text + ", start=" + start + ", detectionType=" + detectionType + ", end=" + end + ", detection="
            + detection + ", score=" + score + "]";
    }
}
