/*
 * Copyright IBM Corp. 2025 - 2025
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

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    @Override
    public String toString() {
        return "DetectionTextResponse [text=" + text + ", start=" + start + ", detectionType=" + detectionType + ", end=" + end + ", detection="
            + detection + ", score=" + score + "]";
    }
}
