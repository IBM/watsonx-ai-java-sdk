/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static java.util.Objects.isNull;
import java.util.List;

/**
 * Represents the response from a context-based text detection request.
 */
public final class DetectionContextResponse extends BaseDetectionResponse {
    private final String detectorId;
    private final List<Evidence> evidence;

    public DetectionContextResponse(String detectorId, String detectionType, String detection, double score, List<Evidence> evidence) {
        super(null, detectionType, detection, score);
        this.detectorId = detectorId;
        this.evidence = isNull(evidence) ? null : List.copyOf(evidence);
    }

    /**
     * Returns the id of the detector that produced this result (e.g. {@code granite_guardian_3_2_5b}, {@code context_relevance_125m_v1_en}).
     *
     * @return the detector id
     */
    public String detectorId() {
        return detectorId;
    }

    /**
     * Returns the evidence supporting this detection.
     *
     * @return the list of evidence items
     */
    public List<Evidence> evidence() {
        return evidence;
    }

    /**
     * Represents a single piece of evidence supporting a detection result.
     */
    public static final class Evidence {
        private final String name;
        private final String value;
        private final double score;
        private final List<Evidence> evidence;

        public Evidence(String name, String value, double score, List<Evidence> evidence) {
            this.name = name;
            this.value = value;
            this.score = score;
            this.evidence = isNull(evidence) ? null : List.copyOf(evidence);
        }

        /**
         * Returns the name for this evidence.
         *
         * @return the evidence name
         */
        public String name() {
            return name;
        }

        /**
         * Returns the value for this evidence.
         *
         * @return the evidence value
         */
        public String value() {
            return value;
        }

        /**
         * Returns the confidence score for this evidence.
         *
         * @return the score
         */
        public double score() {
            return score;
        }

        /**
         * Returns the child evidence items, if any.
         *
         * @return the nested evidence list, or {@code null} if this is a leaf item
         */
        public List<Evidence> evidence() {
            return evidence;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            temp = Double.doubleToLongBits(score);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + ((evidence == null) ? 0 : evidence.hashCode());
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
            Evidence other = (Evidence) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
                return false;
            if (evidence == null) {
                if (other.evidence != null)
                    return false;
            } else if (!evidence.equals(other.evidence))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Evidence [name=" + name + ", value=" + value + ", score=" + score + ", evidence=" + evidence + "]";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((detectorId == null) ? 0 : detectorId.hashCode());
        result = prime * result + ((evidence == null) ? 0 : evidence.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        DetectionContextResponse other = (DetectionContextResponse) obj;
        if (detectorId == null) {
            if (other.detectorId != null)
                return false;
        } else if (!detectorId.equals(other.detectorId))
            return false;
        if (evidence == null) {
            if (other.evidence != null)
                return false;
        } else if (!evidence.equals(other.evidence))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DetectionContextResponse [detectorId=" + detectorId + ", detectionType=" + detectionType + ", detection=" + detection
            + ", score=" + score + ", evidence=" + evidence + "]";
    }
}
