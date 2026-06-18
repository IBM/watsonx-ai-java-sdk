/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig;

/**
 * Base class for semantic configuration.
 *
 * @see TextExtractionSemanticConfig
 * @see TextClassificationSemanticConfig
 * @see CreateSchemaSemanticConfig
 * @see ImproveSchemaSemanticConfig
 * @see MergeSchemaSemanticConfig
 */
public abstract class SemanticConfig {
    private final String defaultModelName;

    protected SemanticConfig(Builder<?> builder) {
        defaultModelName = builder.defaultModelName;
    }

    /**
     * Gets the default model name.
     *
     * @return the default model name
     */
    public String defaultModelName() {
        return defaultModelName;
    }

    /**
     * Builder abstract class for constructing {@link SemanticConfig} instance.
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {
        private String defaultModelName;

        /**
         * Model to use.
         *
         * @param defaultModelName the name of the default model to use
         */
        public T defaultModelName(String defaultModelName) {
            this.defaultModelName = defaultModelName;
            return (T) this;
        }
    }
}
