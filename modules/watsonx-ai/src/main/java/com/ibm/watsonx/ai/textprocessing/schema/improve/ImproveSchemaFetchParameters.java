/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of an improve schema fetch operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ImproveSchemaFetchParameters.builder()
 *     .projectId("project-id")
 *     .build();
 * }</pre>
 *
 */
public final class ImproveSchemaFetchParameters extends WatsonxParameters {

    private ImproveSchemaFetchParameters(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ImproveSchemaFetchParameters.builder()
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
     * Builder class for constructing {@link ImproveSchemaFetchParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link ImproveSchemaFetchParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link ImproveSchemaFetchParameters}
         */
        public ImproveSchemaFetchParameters build() {
            return new ImproveSchemaFetchParameters(this);
        }
    }
}
