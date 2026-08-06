/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a cluster schema fetch operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ClusterSchemaFetchParameters.builder()
 *     .projectId("project-id")
 *     .build();
 * }</pre>
 *
 */
public final class ClusterSchemaFetchParameters extends WatsonxParameters {

    private ClusterSchemaFetchParameters(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ClusterSchemaFetchParameters.builder()
     *     .projectId("project-id")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ClusterSchemaFetchParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link ClusterSchemaFetchParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link ClusterSchemaFetchParameters}
         */
        public ClusterSchemaFetchParameters build() {
            return new ClusterSchemaFetchParameters(this);
        }
    }

    @Override
    public String toString() {
        return "ClusterSchemaFetchParameters [" + super.toString() + "]";
    }
}
