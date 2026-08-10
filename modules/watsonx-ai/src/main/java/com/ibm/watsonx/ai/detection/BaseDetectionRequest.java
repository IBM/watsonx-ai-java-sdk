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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((detectors == null) ? 0 : detectors.hashCode());
        result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
        result = prime * result + ((spaceId == null) ? 0 : spaceId.hashCode());
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
        BaseDetectionRequest other = (BaseDetectionRequest) obj;
        if (detectors == null) {
            if (other.detectors != null)
                return false;
        } else if (!detectors.equals(other.detectors))
            return false;
        if (projectId == null) {
            if (other.projectId != null)
                return false;
        } else if (!projectId.equals(other.projectId))
            return false;
        if (spaceId == null) {
            if (other.spaceId != null)
                return false;
        } else if (!spaceId.equals(other.spaceId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BaseDetectionRequest [detectors=" + detectors + ", projectId=" + projectId + ", spaceId=" + spaceId + "]";
    }
}
