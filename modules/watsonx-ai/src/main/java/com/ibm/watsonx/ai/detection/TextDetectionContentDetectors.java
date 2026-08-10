/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.Map;

/**
 * Represents a request for text content detection.
 */
public final class TextDetectionContentDetectors extends BaseDetectionRequest {
    private final String input;

    public TextDetectionContentDetectors(String input, Map<String, Map<String, Object>> detectors, String projectId, String spaceId) {
        super(detectors, projectId, spaceId);
        this.input = input;
    }

    /**
     * Returns the input text to analyze.
     *
     * @return the input text
     */
    public String input() {
        return input;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((input == null) ? 0 : input.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        TextDetectionContentDetectors other = (TextDetectionContentDetectors) obj;
        if (input == null) {
            if (other.input != null)
                return false;
        } else if (!input.equals(other.input))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TextDetectionContentDetectors [detectors=" + detectors() + ", projectId=" + projectId() + ", spaceId=" + spaceId() + ", input="
            + input + "]";
    }
}
