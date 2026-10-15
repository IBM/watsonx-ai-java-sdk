/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

/**
 * Represents the response from a generated text detection request.
 */
public final class DetectionGeneratedResponse extends BaseDetectionResponse {
    private final String detectorId;

    public DetectionGeneratedResponse(String detectorId, String detectionType, String detection, double score) {
        super(null, detectionType, detection, score);
        this.detectorId = detectorId;
    }

    /**
     * Returns the id of the detector that produced this result.
     *
     * @return the detector id
     */
    public String detectorId() {
        return detectorId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((detectorId == null) ? 0 : detectorId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        DetectionGeneratedResponse other = (DetectionGeneratedResponse) obj;
        if (detectorId == null) {
            if (other.detectorId != null)
                return false;
        } else if (!detectorId.equals(other.detectorId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DetectionGeneratedResponse [detectorId=" + detectorId + ", detectionType=" + detectionType + ", detection=" + detection
            + ", score=" + score + "]";
    }
}
