/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

import com.ibm.watsonx.ai.textprocessing.SemanticConfig;

/**
 * Represents the semantic configuration for merge schema task.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * MergeSchemaSemanticConfig.builder()
 *     .defaultModelName("my-custom-model")
 *     .build();
 * }</pre>
 */
public final class MergeSchemaSemanticConfig extends SemanticConfig {

    private MergeSchemaSemanticConfig(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * MergeSchemaSemanticConfig.builder()
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
     * Builder class for constructing {@link MergeSchemaSemanticConfig} instance.
     */
    public final static class Builder extends SemanticConfig.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link MergeSchemaSemanticConfig} instance.
         *
         * @return a new instance of {@link MergeSchemaSemanticConfig}
         */
        public MergeSchemaSemanticConfig build() {
            return new MergeSchemaSemanticConfig(this);
        }
    }
}