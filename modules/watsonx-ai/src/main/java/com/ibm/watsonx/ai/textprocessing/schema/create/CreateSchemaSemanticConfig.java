/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import com.ibm.watsonx.ai.textprocessing.SemanticConfig;

/**
 * Represents the semantic configuration for schema creation.
 * <p>
 * This configuration only supports model name settings, without the full feature set available in
 * {@link com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationSemanticConfig} and
 * {@link com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig}.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * CreateSchemaSemanticConfig.builder()
 *     .defaultModelName("my-custom-model")
 *     .build();
 * }</pre>
 */
public final class CreateSchemaSemanticConfig extends SemanticConfig {

    private CreateSchemaSemanticConfig(Builder builder) {
        super(builder);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * CreateSchemaSemanticConfig.builder()
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
     * Builder class for constructing {@link CreateSchemaSemanticConfig} instance.
     */
    public final static class Builder extends SemanticConfig.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link CreateSchemaSemanticConfig} instance.
         *
         * @return a new instance of {@link CreateSchemaSemanticConfig}
         */
        public CreateSchemaSemanticConfig build() {
            return new CreateSchemaSemanticConfig(this);
        }
    }
}