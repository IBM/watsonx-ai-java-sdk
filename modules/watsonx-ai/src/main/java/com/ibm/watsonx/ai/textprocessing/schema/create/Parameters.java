/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import java.util.List;

/**
 * Represents the configuration parameters used by the Create Schema API.
 *
 * @param mode extraction mode
 * @param ocrMode OCR mode
 * @param autoRotationCorrection whether to enable automatic rotation correction
 * @param languages list of language codes for OCR
 * @param additionalPromptInstructions additional instructions to guide schema creation.
 * @param enableGrounding if we should return grounding data with examples of each field.
 * @param maxPagesToProcess how many pages we should create a schema for.
 * @param semanticConfig properties related to semantic config.
 */
public record Parameters(
    String mode,
    String ocrMode,
    Boolean autoRotationCorrection,
    List<String> languages,
    String additionalPromptInstructions,
    Boolean enableGrounding,
    Integer maxPagesToProcess,
    SemanticConfig semanticConfig) {

    /**
     * Represents the semantic configuration for the Create Schema API.
     *
     * @param defaultModelName the model name to use.
     */
    public record SemanticConfig(String defaultModelName) {}
}
