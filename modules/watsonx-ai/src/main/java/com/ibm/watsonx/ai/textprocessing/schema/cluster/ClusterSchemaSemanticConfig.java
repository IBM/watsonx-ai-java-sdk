/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

import com.ibm.watsonx.ai.textprocessing.SemanticConfig;

/**
 * Represents the semantic configuration for cluster schema.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ClusterSchemaSemanticConfig.builder()
 *     .defaultModelName("my-custom-model")
 *     .build();
 * }</pre>
 */
public final class ClusterSchemaSemanticConfig extends SemanticConfig {

    private ClusterSchemaSemanticConfig(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ClusterSchemaSemanticConfig.builder()
     *     .defaultModelName("my-custom-model")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ClusterSchemaSemanticConfig} instance.
     */
    public static final class Builder extends SemanticConfig.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link ClusterSchemaSemanticConfig} instance.
         *
         * @return a new instance of {@link ClusterSchemaSemanticConfig}
         */
        public ClusterSchemaSemanticConfig build() {
            return new ClusterSchemaSemanticConfig(this);
        }
    }

    @Override
    public String toString() {
        return "ClusterSchemaSemanticConfig [" + super.toString() + "]";
    }
}
