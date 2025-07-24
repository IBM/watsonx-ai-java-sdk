/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

/**
 * Represents a set of parameters used to control the behavior of a text extraction fetch operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextExtractionFetchParameters.builder()
 *     .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
 *     .build();
 * }</pre>
 *
 */
public final class TextExtractionFetchParameters {
    private final String projectId;
    private final String spaceId;

    public TextExtractionFetchParameters(Builder build) {
        this.projectId = build.projectId;
        this.spaceId = build.spaceId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextExtractionFetchParameters.builder()
     *     .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextExtractionFetchParameters} instances.
     */
    public static class Builder {
        private String projectId;
        private String spaceId;

        /**
         * Sets the project id.
         *
         * @param projectId the project id.
         */
        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        /**
         * Sets the space id.
         *
         * @param spaceId the space id.
         */
        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        /**
         * Builds a {@link TextExtractionFetchParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link TextExtractionFetchParameters}
         */
        public TextExtractionFetchParameters build() {
            return new TextExtractionFetchParameters(this);
        }
    }
}
