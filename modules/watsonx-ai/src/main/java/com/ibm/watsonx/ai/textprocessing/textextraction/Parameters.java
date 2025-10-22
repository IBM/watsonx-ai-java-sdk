/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import java.util.List;

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
    TextExtractionSemanticConfig semanticConfig) {

    public static Parameters of(List<String> requestedOutputs) {
        return new Parameters(requestedOutputs, null, null, null, null, null, null, null, null, null);
    }
}
