/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.Map;

/**
 * Represents a generic detection request.
 */
abstract class BaseDetectionRequest {
    private final Map<String, Map<String, Object>> detectors;
    private final String projectId;
    private final String spaceId;

    protected BaseDetectionRequest(Map<String, Map<String, Object>> detectors, String projectId, String spaceId) {
        this.detectors = detectors;
        this.projectId = projectId;
        this.spaceId = spaceId;
    }

    public Map<String, Map<String, Object>> getDetectors() {
        return detectors;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getSpaceId() {
        return spaceId;
    }
}
