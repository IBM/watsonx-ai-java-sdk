/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.SemanticConfig;

/**
 * Represents the semantic configuration for text extraction.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * KvpFields fields = KvpFields.builder()
 *     .add("invoice_date", KvpField.of("The date when the invoice was issued.", "2024-07-10"))
 *     .add("invoice_number", KvpField.of("The unique number identifying the invoice.", "INV-2024-001"))
 *     .add("total_amount", KvpField.of("The total amount to be paid.", "1250.50"))
 *     .build();
 *
 * TextExtractionSemanticConfig.builder()
 *     .schemasMergeStrategy("replace")
 *     .schemas(
 *         Schema.builder()
 *             .documentDescription("A vendor-issued invoice listing purchased items, prices, and payment information.")
 *             .documentType("Invoice")
 *             .fields(fields)
 *             .build()
 *     ).build();
 * }</pre>
 */
public final class TextExtractionSemanticConfig extends SemanticConfig {
    private final Integer targetImageWidth;
    private final Boolean enableTextHints;
    private final Boolean enableGenericKvp;
    private final Boolean enableSchemaKvp;
    private final String groundingMode;
    private final String forceSchemaName;
    private final String defaultModelName;
    private final Map<String, Object> taskModelNameOverride;

    private TextExtractionSemanticConfig(Builder builder) {
        super(builder);
        targetImageWidth = builder.targetImageWidth;
        enableTextHints = builder.enableTextHints;
        enableGenericKvp = builder.enableGenericKvp;
        enableSchemaKvp = builder.enableSchemaKvp;
        groundingMode = builder.groundingMode;
        forceSchemaName = builder.forceSchemaName;
        defaultModelName = builder.defaultModelName;
        taskModelNameOverride = builder.taskModelNameOverride;
    }

    public Integer getTargetImageWidth() {
        return targetImageWidth;
    }

    public Boolean getEnableTextHints() {
        return enableTextHints;
    }

    public Boolean getEnableGenericKvp() {
        return enableGenericKvp;
    }

    public Boolean getEnableSchemaKvp() {
        return enableSchemaKvp;
    }

    public String getGroundingMode() {
        return groundingMode;
    }

    public String getForceSchemaName() {
        return forceSchemaName;
    }

    public String getDefaultModelName() {
        return defaultModelName;
    }

    public Map<String, Object> getTaskModelNameOverride() {
        return taskModelNameOverride;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     *
     * TextExtractionSemanticConfig.builder()
     *     .schemasMergeStrategy("replace")
     *     .schemas(
     *         Schema.builder()
     *             .documentDescription("A vendor-issued invoice listing purchased items, prices, and payment information.")
     *             .documentType("Invoice")
     *             .fields(...)
     *             .build()
     *     ).build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextExtractionSemanticConfig} instance.
     */
    public final static class Builder extends SemanticConfig.Builder<Builder> {
        private Integer targetImageWidth;
        private Boolean enableTextHints;
        private Boolean enableGenericKvp;
        private Boolean enableSchemaKvp;
        private String groundingMode;
        private String forceSchemaName;
        private String defaultModelName;
        private Map<String, Object> taskModelNameOverride;

        private Builder() {}

        /**
         * Sets the target image width for downscaling.
         *
         * @param targetImageWidth the target width in pixels
         * @return this builder instance
         */
        public Builder targetImageWidth(Integer targetImageWidth) {
            this.targetImageWidth = targetImageWidth;
            return this;
        }

        /**
         * Sets whether to enable text hints during extraction.
         *
         * @param enableTextHints true to enable text hints, false otherwise
         * @return this builder instance
         */
        public Builder enableTextHints(Boolean enableTextHints) {
            this.enableTextHints = enableTextHints;
            return this;
        }

        /**
         * Sets whether to enable generic key-value pair extraction.
         *
         * @param enableGenericKvp true to enable generic KVP extraction, false otherwise
         * @return this builder instance
         */
        public Builder enableGenericKvp(Boolean enableGenericKvp) {
            this.enableGenericKvp = enableGenericKvp;
            return this;
        }

        /**
         * Sets whether to enable schema key-value pair extraction.
         * <p>
         * When enabled, the extractor performs schema-based KVP extraction using predefined or user-provided schemas.
         *
         * @param enableSchemaKvp true to enable schema KVP extraction, false otherwise
         */
        public Builder enableSchemaKvp(Boolean enableSchemaKvp) {
            this.enableSchemaKvp = enableSchemaKvp;
            return this;
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
        public Builder groundingMode(String groundingMode) {
            this.groundingMode = groundingMode;
            return this;
        }

        /**
         * Forces the use of a specific schema during extraction by overriding the document classification step.
         * <p>
         * The provided name must exactly match the {@code document_type} of one of the predefined or custom schemas currently in use.
         *
         * @param forceSchemaName the schema name to force during extraction
         */
        public Builder forceSchemaName(String forceSchemaName) {
            this.forceSchemaName = forceSchemaName;
            return this;
        }

        /**
         * By default, all KVP tasks use the models documented
         * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html">in this page</a>. This parameter
         * allows changing the default model to another compatible vision model. A list of compatible vision models is available
         * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html">here</a>.
         *
         * @param defaultModelName the name of the default vision model to use
         */
        public Builder defaultModelName(String defaultModelName) {
            this.defaultModelName = defaultModelName;
            return this;
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
         * <pre>{@code
         * {
         *   "create_schema": "pixtral-small-something-else"
         * }
         * }</pre>
         *
         * @param taskModelNameOverride a map of task names to custom model names
         * @return this builder instance
         */
        public Builder taskModelNameOverride(Map<String, Object> taskModelNameOverride) {
            this.taskModelNameOverride = taskModelNameOverride;
            return this;
        }

        /**
         * Builds a {@link TextExtractionSemanticConfig} instance.
         *
         * @return a new instance of {@link TextExtractionSemanticConfig}
         */
        public TextExtractionSemanticConfig build() {
            return new TextExtractionSemanticConfig(this);
        }
    }
}
