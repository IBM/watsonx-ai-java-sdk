/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents the configuration parameters used by the Text Extraction API.
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
        Integer targetImageWidth,
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
