/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import java.util.List;
import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents the configuration parameters used by the Text Classification API.
 */
public record Parameters(
    String ocrMode,
    String classificationMode,
    Boolean autoRotationCorrection,
    List<String> languages,
    SemanticConfig semanticConfig) {
    public record SemanticConfig(String schemasMergeStrategy, List<Schema> schemas) {}
}
