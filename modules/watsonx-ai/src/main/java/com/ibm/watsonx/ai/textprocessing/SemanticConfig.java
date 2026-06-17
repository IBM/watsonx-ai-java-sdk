/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig;

/**
 * Base class for semantic configuration.
 * <p>
 * <b>Note:</b> This class is intended for internal use only. Use one of the specific subclasses instead:
 * <ul>
 * <li>{@link TextExtractionSemanticConfig}</li>
 * <li>{@link TextClassificationSemanticConfig}</li>
 * <li>{@link CreateSchemaSemanticConfig}</li>
 * <li>{@link ImproveSchemaSemanticConfig}</li>
 * </ul>
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
         * By default, all KVP tasks use the models documented
         * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html">in this page</a>. This parameter
         * allows changing the default model to another compatible vision model. A list of compatible vision models is available
         * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html">here</a>.
         *
         * @param defaultModelName the name of the default vision model to use
         */
        public T defaultModelName(String defaultModelName) {
            this.defaultModelName = defaultModelName;
            return (T) this;
        }
    }
}
