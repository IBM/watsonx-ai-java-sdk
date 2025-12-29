/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import java.util.Map;

/**
 * Represents a generic detection request.
 */
public abstract class BaseDetectionRequest {
    private final Map<String, Map<String, Object>> detectors;
    private final String projectId;
    private final String spaceId;

    protected BaseDetectionRequest(Map<String, Map<String, Object>> detectors, String projectId, String spaceId) {
        this.detectors = detectors;
        this.projectId = projectId;
        this.spaceId = spaceId;
    }

    /**
     * Returns the detector configurations.
     *
     * @return a map of detector names to their configuration parameters
     */
    public Map<String, Map<String, Object>> detectors() {
        return detectors;
    }

    /**
     * Returns the project identifier.
     *
     * @return the project id
     */
    public String projectId() {
        return projectId;
    }

    /**
     * Returns the space identifier.
     *
     * @return the space id
     */
    public String spaceId() {
        return spaceId;
    }
}
