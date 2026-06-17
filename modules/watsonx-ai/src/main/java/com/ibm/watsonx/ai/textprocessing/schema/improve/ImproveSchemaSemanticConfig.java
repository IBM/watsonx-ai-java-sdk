/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

import com.ibm.watsonx.ai.textprocessing.SemanticConfig;

/**
 * Represents the semantic configuration for improve schema task.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ImproveSchemaSemanticConfig.builder()
 *     .defaultModelName("my-custom-model")
 *     .build();
 * }</pre>
 */
public final class ImproveSchemaSemanticConfig extends SemanticConfig {

    private ImproveSchemaSemanticConfig(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ImproveSchemaSemanticConfig.builder()
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
     * Builder class for constructing {@link ImproveSchemaSemanticConfig} instance.
     */
    public final static class Builder extends SemanticConfig.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link ImproveSchemaSemanticConfig} instance.
         *
         * @return a new instance of {@link ImproveSchemaSemanticConfig}
         */
        public ImproveSchemaSemanticConfig build() {
            return new ImproveSchemaSemanticConfig(this);
        }
    }
}