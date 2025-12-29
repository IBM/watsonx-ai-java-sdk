/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents the configuration parameters used by the Text Extraction API.
 *
 * @param requestedOutputs list of output types to generate
 * @param mode extraction mode
 * @param ocrMode OCR mode
 * @param languages list of language codes for OCR
 * @param autoRotationCorrection whether to enable automatic rotation correction
 * @param createEmbeddedImages embedded image creation mode
 * @param outputDpi output DPI for images
 * @param outputTokens whether to output tokens
 * @param kvpMode key-value pair extraction mode
 * @param semanticConfig semantic extraction configuration
 */
public record Parameters(
    List<String> requestedOutputs,
    String mode,
    String ocrMode,
    List<String> languages,
    Boolean autoRotationCorrection,
    String createEmbeddedImages,
    Integer outputDpi,
    Boolean outputTokens,
    String kvpMode,
    SemanticConfig semanticConfig) {

    public static Parameters of(List<String> requestedOutputs) {
        return new Parameters(requestedOutputs, null, null, null, null, null, null, null, null, null);
    }

    public record SemanticConfig(
        Boolean enableTextHints,
        Boolean enableGenericKvp,
        Boolean enableSchemaKvp,
        String groundingMode,
        String forceSchemaName,
        String defaultModelName,
        Map<String, Object> taskModelNameOverride,
        String schemasMergeStrategy,
        List<Schema> schemas) {}
}
