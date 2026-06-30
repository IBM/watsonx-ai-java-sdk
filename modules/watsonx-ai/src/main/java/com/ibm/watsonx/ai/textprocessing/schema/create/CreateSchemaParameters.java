/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxParameters;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.Language;
import com.ibm.watsonx.ai.textprocessing.Mode;
import com.ibm.watsonx.ai.textprocessing.OcrMode;

/**
 * Represents a set of parameters used to control the behavior of a create schema operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * CreateSchemaParameters.builder()
 *     .mode("standard")
 *     .languages(Language.ENGLISH, Language.ITALIAN)
 *     .maxPagesToProcess(10)
 *     .enableGrounding(true)
 *     .build();
 * }</pre>
 *
 */
public final class CreateSchemaParameters extends WatsonxParameters {
    private final String mode;
    private final String ocrMode;
    private final List<String> languages;
    private final Boolean autoRotationCorrection;
    private final String additionalPromptInstructions;
    private final Boolean enableGrounding;
    private final Integer maxPagesToProcess;
    private final CreateSchemaSemanticConfig semanticConfig;
    private final boolean removeUploadedFile;
    private final CosReference documentReference;
    private final Duration timeout;

    private CreateSchemaParameters(Builder builder) {
        super(builder);
        this.mode = builder.mode;
        this.ocrMode = requireNonNullElse(builder.ocrMode, OcrMode.DISABLED.value());
        this.languages = isNull(builder.languages) ? null : List.copyOf(builder.languages);
        this.autoRotationCorrection = builder.autoRotationCorrection;
        this.additionalPromptInstructions = builder.additionalPromptInstructions;
        this.enableGrounding = builder.enableGrounding;
        this.maxPagesToProcess = builder.maxPagesToProcess;
        this.semanticConfig = builder.semanticConfig;
        this.removeUploadedFile = builder.removeUploadedFile;
        this.documentReference = builder.documentReference;
        this.timeout = builder.timeout;
    }

    /**
     * Gets the extraction mode.
     *
     * @return the extraction mode
     */
    public String mode() {
        return mode;
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
     * Gets the list of languages for OCR.
     *
     * @return the list of language codes
     */
    public List<String> languages() {
        return languages;
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
     * Gets the additional prompt instructions.
     *
     * @return the additional prompt instructions
     */
    public String additionalPromptInstructions() {
        return additionalPromptInstructions;
    }

    /**
     * Gets whether grounding is enabled.
     *
     * @return true if grounding is enabled
     */
    public Boolean enableGrounding() {
        return enableGrounding;
    }

    /**
     * Gets the maximum number of pages to process.
     *
     * @return the maximum number of pages
     */
    public Integer maxPagesToProcess() {
        return maxPagesToProcess;
    }

    /**
     * Gets the semantic configuration.
     *
     * @return the semantic configuration
     */
    public CreateSchemaSemanticConfig semanticConfig() {
        return semanticConfig;
    }

    /**
     * Gets whether to remove the uploaded file after processing.
     *
     * @return true if the uploaded file should be removed
     */
    public boolean isRemoveUploadedFile() {
        return removeUploadedFile;
    }

    /**
     * Gets the document reference.
     *
     * @return the document reference
     */
    public CosReference documentReference() {
        return documentReference;
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
     * Converts this parameters object to a {@link Parameters} record for API requests.
     *
     * @return a Parameters record containing the configuration
     */
    public Parameters toParameters() {
        Parameters.SemanticConfig semanticConfigRecord = null;
        if (semanticConfig != null)
            semanticConfigRecord = new Parameters.SemanticConfig(semanticConfig.defaultModelName());


        return new Parameters(
            mode,
            ocrMode,
            autoRotationCorrection,
            languages,
            additionalPromptInstructions,
            enableGrounding,
            maxPagesToProcess,
            semanticConfigRecord
        );
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * CreateSchemaParameters.builder()
     *     .mode("standard")
     *     .languages(Language.ENGLISH, Language.ITALIAN)
     *     .maxPagesToProcess(10)
     *     .enableGrounding(true)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link CreateSchemaParameters} instances.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {
        private String mode;
        private String ocrMode;
        private List<String> languages;
        private Boolean autoRotationCorrection;
        private String additionalPromptInstructions;
        private Boolean enableGrounding;
        private Integer maxPagesToProcess;
        private CreateSchemaSemanticConfig semanticConfig;
        private boolean removeUploadedFile;
        private CosReference documentReference;
        private Duration timeout;

        private Builder() {}


        /**
         * Sets the extraction mode.
         *
         * @param mode the extraction mode (e.g., "standard", "high_quality")
         */
        public Builder mode(Mode mode) {
            requireNonNull(mode);
            this.mode = mode.value();
            return this;
        }

        /**
         * Sets the OCR mode.
         *
         * @param ocrMode the OCR mode
         */
        public Builder ocrMode(OcrMode ocrMode) {
            this.ocrMode = ocrMode.value();
            return this;
        }

        /**
         * Sets the languages for OCR.
         *
         * @param languages one or more language codes
         */
        public Builder languages(Language... languages) {
            return languages(Arrays.stream(languages).map(Language::isoCode).toList());
        }

        /**
         * Sets the languages for OCR.
         *
         * @param languages list of language codes
         */
        public Builder languages(List<String> languages) {
            this.languages = languages;
            return this;
        }

        /**
         * Sets whether to enable automatic rotation correction.
         *
         * @param autoRotationCorrection true to enable auto rotation correction
         */
        public Builder autoRotationCorrection(Boolean autoRotationCorrection) {
            this.autoRotationCorrection = autoRotationCorrection;
            return this;
        }

        /**
         * Sets additional prompt instructions to guide schema creation.
         *
         * @param additionalPromptInstructions the additional instructions
         */
        public Builder additionalPromptInstructions(String additionalPromptInstructions) {
            this.additionalPromptInstructions = additionalPromptInstructions;
            return this;
        }

        /**
         * Sets whether to enable grounding with examples.
         *
         * @param enableGrounding true to enable grounding
         */
        public Builder enableGrounding(Boolean enableGrounding) {
            this.enableGrounding = enableGrounding;
            return this;
        }

        /**
         * Sets the maximum number of pages to process for schema creation.
         *
         * @param maxPagesToProcess the maximum number of pages
         */
        public Builder maxPagesToProcess(Integer maxPagesToProcess) {
            this.maxPagesToProcess = maxPagesToProcess;
            return this;
        }

        /**
         * Sets the semantic configuration.
         *
         * @param semanticConfig the semantic configuration
         */
        public Builder semanticConfig(CreateSchemaSemanticConfig semanticConfig) {
            this.semanticConfig = semanticConfig;
            return this;
        }

        /**
         * Sets whether to remove the uploaded file after processing completes.
         * <p>
         * This parameter can only be used with synchronous operations (methods that wait for completion).
         *
         * @param removeUploadedFile true to remove the uploaded file
         */
        public Builder removeUploadedFile(boolean removeUploadedFile) {
            this.removeUploadedFile = removeUploadedFile;
            return this;
        }

        /**
         * Sets the document reference (COS connection and bucket).
         *
         * @param documentReference the document reference
         */
        public Builder documentReference(CosReference documentReference) {
            this.documentReference = documentReference;
            return this;
        }

        /**
         * Sets the timeout for synchronous operations.
         *
         * @param timeout the timeout duration
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Builds a {@link CreateSchemaParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link CreateSchemaParameters}
         */
        public CreateSchemaParameters build() {
            return new CreateSchemaParameters(this);
        }
    }
}
