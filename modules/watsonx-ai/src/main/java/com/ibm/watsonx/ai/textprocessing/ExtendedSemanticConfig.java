/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig;

/**
 * Extended semantic configuration with full feature set.
 * <p>
 * <b>Note:</b> This class is intended for internal use only. Use one of the specific subclasses instead:
 * <ul>
 * <li>{@link TextExtractionSemanticConfig}</li>
 * <li>{@link TextClassificationSemanticConfig}</li>
 * </ul>
 */
public abstract class ExtendedSemanticConfig extends SemanticConfig {
    private final Boolean enableTextHints;
    private final Boolean enableGenericKvp;
    private final Boolean enableSchemaKvp;
    private final String groundingMode;
    private final String schemasMergeStrategy;
    private final String forceSchemaName;
    private final List<Schema> schemas;
    private final Map<String, Object> taskModelNameOverride;

    protected ExtendedSemanticConfig(Builder<?> builder) {
        super(builder);
        enableTextHints = builder.enableTextHints;
        enableGenericKvp = builder.enableGenericKvp;
        enableSchemaKvp = builder.enableSchemaKvp;
        groundingMode = builder.groundingMode;
        schemasMergeStrategy = nonNull(builder.schemasMergeStrategy) ? builder.schemasMergeStrategy.value() : null;
        forceSchemaName = builder.forceSchemaName;
        schemas = isNull(builder.schemas) ? null : List.copyOf(builder.schemas);
        taskModelNameOverride =
            isNull(builder.taskModelNameOverride) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.taskModelNameOverride));
    }

    /**
     * Gets whether text hints are enabled.
     *
     * @return true if text hints are enabled
     */
    public Boolean enableTextHints() {
        return enableTextHints;
    }

    /**
     * Gets whether generic key-value pair extraction is enabled.
     *
     * @return true if generic KVP extraction is enabled
     */
    public Boolean enableGenericKvp() {
        return enableGenericKvp;
    }

    /**
     * Gets whether schema key-value pair extraction is enabled.
     *
     * @return true if schema KVP extraction is enabled
     */
    public Boolean enableSchemaKvp() {
        return enableSchemaKvp;
    }

    /**
     * Gets the grounding mode.
     *
     * @return the grounding mode
     */
    public String groundingMode() {
        return groundingMode;
    }

    /**
     * Gets the forced schema name.
     *
     * @return the forced schema name, or null if not set
     */
    public String forceSchemaName() {
        return forceSchemaName;
    }

    /**
     * Gets the schema merge strategy.
     *
     * @return the schema merge strategy value
     */
    public String schemasMergeStrategy() {
        return schemasMergeStrategy;
    }

    /**
     * Gets the list of custom schemas.
     *
     * @return the list of schemas
     */
    public List<Schema> schemas() {
        return schemas;
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
     * Builder abstract class for constructing {@link ExtendedSemanticConfig} instance.
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> extends SemanticConfig.Builder<T> {
        private Boolean enableTextHints;
        private Boolean enableGenericKvp;
        private Boolean enableSchemaKvp;
        private String groundingMode;
        private SchemaMergeStrategy schemasMergeStrategy;
        private String forceSchemaName;
        private List<Schema> schemas;
        private Map<String, Object> taskModelNameOverride;

        /**
         * Sets whether to enable text hints during extraction.
         *
         * @param enableTextHints true to enable text hints, false otherwise
         * @return this builder instance
         */
        public T enableTextHints(Boolean enableTextHints) {
            this.enableTextHints = enableTextHints;
            return (T) this;
        }

        /**
         * Sets whether to enable generic key-value pair extraction.
         *
         * @param enableGenericKvp true to enable generic KVP extraction, false otherwise
         * @return this builder instance
         */
        public T enableGenericKvp(Boolean enableGenericKvp) {
            this.enableGenericKvp = enableGenericKvp;
            return (T) this;
        }

        /**
         * Sets whether to enable schema key-value pair extraction.
         * <p>
         * When enabled, the extractor performs schema-based KVP extraction using predefined or user-provided schemas.
         *
         * @param enableSchemaKvp true to enable schema KVP extraction, false otherwise
         */
        public T enableSchemaKvp(Boolean enableSchemaKvp) {
            this.enableSchemaKvp = enableSchemaKvp;
            return (T) this;
        }

        /**
         * Sets the grounding mode, which defines the level of key-value pair grounding precision. *
         * <p>
         * Allowable values:
         * <ul>
         * <li><b>"precise"</b> – Ensures higher accuracy when linking extracted values to schema keys.</li>
         * <li><b>"fast"</b> – Prioritizes speed over precision in grounding.</li>
         * </ul>
         *
         * @param groundingMode the grounding mode to use
         */
        public T groundingMode(GroundingMode groundingMode) {
            this.groundingMode = nonNull(groundingMode) ? groundingMode.value : null;
            return (T) this;
        }

        /**
         * Forces the use of a specific schema during extraction by overriding the document classification step.
         * <p>
         * The provided name must exactly match the {@code document_type} of one of the predefined or custom schemas currently in use.
         *
         * @param forceSchemaName the schema name to force during extraction
         */
        public T forceSchemaName(String forceSchemaName) {
            this.forceSchemaName = forceSchemaName;
            return (T) this;
        }

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

    /**
     * Specifies the grounding mode for extracting the physical location (bounding boxes) of key-value pairs within a document.
     *
     * @see TextClassificationSemanticConfig
     * @see TextExtractionSemanticConfig
     */
    public enum GroundingMode {

        /**
         * Prioritizes spatial accuracy. Calculates exact bounding box coordinates that tightly align with the text, at the cost of higher compute
         * resources and processing time.
         */
        PRECISE("precise"),

        /**
         * Prioritizes processing speed. Calculates approximate bounding box coordinates, resulting in faster extraction times and lower compute
         * costs.
         */
        FAST("fast");

        private String value;

        GroundingMode(String value) {
            this.value = value;
        }

        /**
         * Returns the string representation of the grounding mode.
         *
         * @return the string value of the mode ("precise" or "fast")
         */
        public String value() {
            return value;
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
            return value;
        }
    }
}
