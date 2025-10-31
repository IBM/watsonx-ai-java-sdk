/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

/**
 * Represents the response from a text content detection request.
 */
public final class DetectionTextResponse extends BaseDetectionResponse {
    private int start;
    private int end;

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "DetectionTextResponse [text=" + text + ", start=" + start + ", detectionType=" + detectionType + ", end=" + end + ", detection="
            + detection + ", score=" + score + "]";
    }
}
