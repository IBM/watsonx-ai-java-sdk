/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a create schema fetch operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * CreateSchemaFetchParameters.builder()
 *     .projectId("project-id")
 *     .build();
 * }</pre>
 *
 */
public final class CreateSchemaFetchParameters extends WatsonxParameters {

    private CreateSchemaFetchParameters(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * CreateSchemaFetchParameters.builder()
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
     * Builder class for constructing {@link CreateSchemaFetchParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link CreateSchemaFetchParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link CreateSchemaFetchParameters}
         */
        public CreateSchemaFetchParameters build() {
            return new CreateSchemaFetchParameters(this);
        }
    }
}
