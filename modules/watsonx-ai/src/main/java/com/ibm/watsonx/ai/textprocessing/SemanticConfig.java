/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaSemanticConfig;
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
 * @see ClusterSchemaSemanticConfig
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
     *
     * @param <T> the type of the concrete builder subclass
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((defaultModelName == null) ? 0 : defaultModelName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SemanticConfig other = (SemanticConfig) obj;
        if (defaultModelName == null) {
            if (other.defaultModelName != null)
                return false;
        } else if (!defaultModelName.equals(other.defaultModelName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SemanticConfig [defaultModelName=" + defaultModelName + "]";
    }
}
