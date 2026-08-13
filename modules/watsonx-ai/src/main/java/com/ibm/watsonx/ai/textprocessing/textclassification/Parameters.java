/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

import static java.util.Objects.isNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents the configuration parameters used by the Text Classification API.
 *
 * @param ocrMode OCR mode
 * @param classificationMode classification mode
 * @param autoRotationCorrection whether to enable automatic rotation correction
 * @param languages list of language codes for OCR
 * @param semanticConfig semantic classification configuration
 */
public record Parameters(
    String ocrMode,
    String classificationMode,
    Boolean autoRotationCorrection,
    List<String> languages,
    SemanticConfig semanticConfig) {

    public Parameters {
        languages = isNull(languages) ? null : List.copyOf(languages);
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
        List<Schema> schemas) {

        public SemanticConfig {
            taskModelNameOverride =
                isNull(taskModelNameOverride) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(taskModelNameOverride));
            schemas = isNull(schemas) ? null : List.copyOf(schemas);
        }
    }
}
