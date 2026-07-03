/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static java.util.Objects.isNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a generic detection request.
 */
public abstract class BaseDetectionRequest {
    private final Map<String, Map<String, Object>> detectors;
    private final String projectId;
    private final String spaceId;

    /**
     * Creates a new detection request with the given detector configurations and target identifiers.
     *
     * @param detectors a map of detector names to their configuration parameters
     * @param projectId the id of the project containing the resource
     * @param spaceId the id of the space containing the resource
     */
    protected BaseDetectionRequest(Map<String, Map<String, Object>> detectors, String projectId, String spaceId) {
        this.detectors = isNull(detectors) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(detectors));
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
