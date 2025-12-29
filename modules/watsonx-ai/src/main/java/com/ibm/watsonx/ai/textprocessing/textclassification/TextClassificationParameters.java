/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import com.ibm.watsonx.ai.WatsonxParameters;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.Language;
import com.ibm.watsonx.ai.textprocessing.OcrMode;

/**
 * Represents a set of parameters used to control the behavior of a text classification operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextClassificationParameters.builder()
 *     .classificationMode(ClassificationMode.EXACT)
 *     .languages(Language.ENGLISH)
 *     .build();
 * }</pre>
 *
 * For more information, see the
 * <a href=" https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-classification-params.html?context=wx">documentation</a>.
 */
public final class TextClassificationParameters extends WatsonxParameters {
    private final String ocrMode;
    private final String classificationMode;
    private final Boolean autoRotationCorrection;
    private final List<String> languages;
    private final TextClassificationSemanticConfig semanticConfig;
    private final boolean removeUploadedFile;
    private final CosReference documentReference;
    private final Map<String, Object> custom;
    private final Duration timeout;

    private TextClassificationParameters(Builder builder) {
        super(builder);
        this.ocrMode = builder.ocrMode;
        this.classificationMode = nonNull(builder.classificationMode) ? builder.classificationMode.value() : null;
        this.autoRotationCorrection = builder.autoRotationCorrection;
        this.languages = builder.languages;
        this.semanticConfig = builder.semanticConfig;
        this.removeUploadedFile = builder.removeUploadedFile;
        this.documentReference = builder.documentReference;
        this.custom = builder.custom;
        this.timeout = builder.timeout;
    }

    /**
     * Gets the OCR mode.
     *
     * @return the OCR mode
     */
    public String ocrMode() {
        return ocrMode;
    }

    /**
     * Gets the classification mode.
     *
     * @return the classification mode
     */
    public String classificationMode() {
        return classificationMode;
    }

    /**
     * Gets whether automatic rotation correction is enabled.
     *
     * @return true if auto rotation correction is enabled
     */
    public Boolean autoRotationCorrection() {
        return autoRotationCorrection;
    }

    /**
     * Gets the list of languages for OCR.
     *
     * @return the list of language codes
     */
    public List<String> languages() {
        return languages;
    }

    /**
     * Gets the semantic configuration.
     *
     * @return the semantic configuration
     */
    public TextClassificationSemanticConfig semanticConfig() {
        return semanticConfig;
    }

    /**
     * Gets whether to remove the uploaded file after classification.
     *
     * @return true if the uploaded file should be removed
     */
    public boolean isRemoveUploadedFile() {
        return removeUploadedFile;
    }

    /**
     * Gets the document reference for COS.
     *
     * @return the document COS reference
     */
    public CosReference documentReference() {
        return documentReference;
    }

    /**
     * Gets the custom properties map.
     *
     * @return the map of custom properties
     */
    public Map<String, Object> custom() {
        return custom;
    }

    /**
     * Gets the timeout duration.
     *
     * @return the timeout duration
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Converts the {@link TextClassificationParameters} into a new {@link Parameters} object.
     */
    Parameters toParameters() {
        var semanticConfig = isNull(semanticConfig())
            ? null
            : new Parameters.SemanticConfig(
                this.semanticConfig.enableTextHints(),
                this.semanticConfig.enableGenericKvp(),
                this.semanticConfig.enableSchemaKvp(),
                this.semanticConfig.groundingMode(),
                this.semanticConfig.forceSchemaName(),
                this.semanticConfig.defaultModelName(),
                this.semanticConfig.taskModelNameOverride(),
                this.semanticConfig.schemasMergeStrategy(),
                this.semanticConfig.schemas());

        return new Parameters(
            ocrMode(),
            classificationMode(),
            autoRotationCorrection(),
            languages(),
            semanticConfig
        );
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextClassificationParameters.builder()
     *     .classificationMode(ClassificationMode.EXACT)
     *     .languages(Language.ENGLISH)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextClassificationParameters} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String ocrMode;
        private ClassificationMode classificationMode;
        private Boolean autoRotationCorrection;
        private List<String> languages;
        private TextClassificationSemanticConfig semanticConfig;
        private boolean removeUploadedFile = false;
        private CosReference documentReference;
        private Map<String, Object> custom;
        private Duration timeout;

        private Builder() {}

        /**
         * Sets the OCR mode.
         *
         * @param ocrMode the OCR mode
         */
        public Builder ocrMode(OcrMode ocrMode) {
            this.ocrMode = switch(ocrMode) {
                case AUTO -> null;
                default -> ocrMode.value();
            };
            return this;
        }

        /**
         * Sets the classification mode.
         * <p>
         * The value {@code exact} gives the exact schema name the the document is classified to.
         * <p>
         * The option {@code binary} only gives whether the document is classified to a known schema or not.
         *
         * @param classificationMode classification mode value.
         */
        public Builder classificationMode(ClassificationMode classificationMode) {
            this.classificationMode = classificationMode;
            return this;
        }

        /**
         * Enables or disables automatic rotation correction.
         *
         * @param autoRotationCorrection true to enable rotation correction, false otherwise
         */
        public Builder autoRotationCorrection(boolean autoRotationCorrection) {
            this.autoRotationCorrection = autoRotationCorrection;
            return this;
        }

        /**
         * Sets the list of languages expected in the document.
         *
         * @param languages the list of language codes (ISO 639)
         */
        public Builder languages(List<String> languages) {
            this.languages = languages;
            return this;
        }

        /**
         * Sets the list of languages expected in the document.
         *
         * @param languages the list of language
         */
        public Builder languages(Language... languages) {
            return languages(Stream.of(languages).map(Language::isoCode).toList());
        }

        /**
         * Sets properties related to semantic config.
         *
         * @param semanticConfig the semantic configuration instance
         */
        public Builder semanticConfig(TextClassificationSemanticConfig semanticConfig) {
            this.semanticConfig = semanticConfig;
            return this;
        }

        /**
         * Specifies whether the uploaded source file should be removed after processing.
         *
         * @param removeUploadedFile {@code true} to delete the uploaded file after processing, {@code false} to retain it.
         */
        public Builder removeUploadedFile(boolean removeUploadedFile) {
            this.removeUploadedFile = removeUploadedFile;
            return this;
        }

        /**
         * Sets the reference to the Cloud Object Storage (COS) location of the input document.
         *
         * @param documentReference the {@link CosReference} pointing to the input file location.
         */
        public Builder documentReference(CosReference documentReference) {
            this.documentReference = documentReference;
            return this;
        }

        /**
         * Adds a custom property.
         * <p>
         * This method allows you to include arbitrary key-value pairs that can be used to pass additional, user-defined metadata or configuration to
         * the classification process.
         *
         * @param key the name of the custom property.
         * @param value the value of the custom property.
         */
        public Builder addCustomProperty(String key, Object value) {
            requireNonNull(key, "key cannot be null");
            requireNonNull(value, "value cannot be null");
            custom = requireNonNullElse(custom, new HashMap<String, Object>());
            custom.put(key, value);
            return this;
        }

        /**
         * Sets the timeout.
         *
         * @param timeout timeout
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Builds a {@link TextClassificationParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link TextClassificationParameters}
         */
        public TextClassificationParameters build() {
            return new TextClassificationParameters(this);
        }
    }

    /**
     * Enum representing the possible types of classification.
     */
    public static enum ClassificationMode {
        EXACT("exact"),
        BINARY("binary");

        private String value;

        ClassificationMode(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }
}