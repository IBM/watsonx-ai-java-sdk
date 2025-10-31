/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.Map;

/**
 * Represents a request for text content detection.
 */
final class TextDetectionContentDetectors extends BaseDetectionRequest {
    private final String input;

    public TextDetectionContentDetectors(String input, Map<String, Map<String, Object>> detectors, String projectId, String spaceId) {
        super(detectors, projectId, spaceId);
        this.input = input;
    }

    public String getInput() {
        return input;
    }
}
