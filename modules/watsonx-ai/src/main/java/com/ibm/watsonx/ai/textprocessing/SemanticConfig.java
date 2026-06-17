/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaSemanticConfig;
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
 * </ul>
 */
public abstract class SemanticConfig {
    private final String defaultModelName;
    private final Map<String, Object> taskModelNameOverride;

    protected SemanticConfig(Builder<?> builder) {
        defaultModelName = builder.defaultModelName;
        taskModelNameOverride = builder.taskModelNameOverride;
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
     * Gets the task model name overrides.
     *
     * @return the map of task names to model names
     */
    public Map<String, Object> taskModelNameOverride() {
        return taskModelNameOverride;
    }

    /**
     * Builder abstract class for constructing {@link SemanticConfig} instance.
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {
        private String defaultModelName;
        private Map<String, Object> taskModelNameOverride;

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

        /**
         * Sets custom model overrides for specific semantic extraction tasks.
         * <p>
         * Each entry in the map defines a task name and the model name to use for that task. Supported task keys include:
         * <ul>
         * <li><b>classification_exact</b></li>
         * <li><b>extraction</b></li>
         * <li><b>create_schema</b></li>
         * <li><b>create_schema_page_merger</b></li>
         * <li><b>improve_schema_description</b></li>
         * <li><b>cluster_schemas</b></li>
         * <li><b>merge_schemas</b></li>
         * </ul>
         *
         * Example:
         *
         * <pre>{@code { "create_schema": "pixtral-small-something-else" } }</pre>
         *
         * @param taskModelNameOverride a map of task names to custom model names @return this builder instance
         */
        public T taskModelNameOverride(Map<String, Object> taskModelNameOverride) {
            this.taskModelNameOverride = taskModelNameOverride;
            return (T) this;
        }
    }
}
