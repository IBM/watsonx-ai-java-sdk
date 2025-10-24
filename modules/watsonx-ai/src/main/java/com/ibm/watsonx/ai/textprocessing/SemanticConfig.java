/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.nonNull;
import java.util.List;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig;

/**
 * Base class for semantic configuration.
 * <p>
 * <strong>Note:</strong> This class is intended for internal use only. Use one of the specific subclasses instead:
 * <ul>
 * <li>{@link TextExtractionSemanticConfig}</li>
 * <li>{@link TextClassificationSemanticConfig}</li>
 * </ul>
 */
public abstract class SemanticConfig {
    private final String schemasMergeStrategy;
    private final List<Schema> schemas;

    protected SemanticConfig(Builder<?> builder) {
        schemasMergeStrategy = nonNull(builder.schemasMergeStrategy) ? builder.schemasMergeStrategy.value() : null;
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
        private SchemaMergeStrategy schemasMergeStrategy;
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
        public T schemasMergeStrategy(SchemaMergeStrategy schemasMergeStrategy) {
            this.schemasMergeStrategy = schemasMergeStrategy;
            return (T) this;
        }

        /**
         * Sets the list of custom semantic schemas.
         *
         * @param schemas the list of schemas to use
         * @return this builder instance
         */
        public T schemas(Schema... schemas) {
            return schemas(List.of(schemas));
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

    /**
     * Defines the strategy used to merge predefined and user-defined input schemas for semantic key-value pair.
     * <p>
     * This setting determines how custom schemas provided by the user interact with the existing predefined schemas within the semantic
     * configuration.
     *
     * @see TextClassificationSemanticConfig
     * @see TextExtractionSemanticConfig
     */
    public enum SchemaMergeStrategy {

        /**
         * Combines predefined and user-defined schemas. User-defined schemas override conflicting definitions.
         */
        MERGE("merge"),

        /**
         * Replaces all predefined schemas with user-defined ones.
         */
        REPLACE("replace");

        private String value;

        SchemaMergeStrategy(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }
}
