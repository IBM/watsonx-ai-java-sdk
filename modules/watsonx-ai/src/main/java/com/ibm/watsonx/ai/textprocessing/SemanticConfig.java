/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import java.util.List;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig;

/**
 * Abstract class with properties related to semantic config.
 *
 * @see TextExtractionSemanticConfig
 * @see TextClassificationSemanticConfig
 */
public abstract class SemanticConfig {
    private final String schemasMergeStrategy;
    private final List<Schema> schemas;

    protected SemanticConfig(Builder<?> builder) {
        schemasMergeStrategy = builder.schemasMergeStrategy;
        schemas = builder.schemas;
    }

    public String getSchemasMergeStrategy() {
        return schemasMergeStrategy;
    }

    public List<Schema> getSchemas() {
        return schemas;
    }

    /**
     * Builder abstract class for constructing {@link SemanticConfig} instance.
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {
        private String schemasMergeStrategy;
        private List<Schema> schemas;

        /**
         * Sets the merge strategy for combining predefined and user-defined schemas.
         * <p>
         * Allowable values:
         * <ul>
         * <li><b>"merge"</b> – Combines predefined schemas with user-defined ones.</li>
         * <li><b>"replace"</b> – Uses only the user-defined schemas, ignoring predefined ones.</li>
         * </ul>
         *
         * @param schemasMergeStrategy the schema merge strategy ("merge" or "replace")
         */
        public T schemasMergeStrategy(String schemasMergeStrategy) {
            this.schemasMergeStrategy = schemasMergeStrategy;
            return (T) this;
        }

        /**
         * Sets the list of custom semantic schemas.
         *
         * @param schemas the list of schemas to use
         * @return this builder instance
         */
        public T schemas(List<Schema> schemas) {
            this.schemas = schemas;
            return (T) this;
        }
    }
}
