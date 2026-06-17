/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

import java.util.List;
import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents the configuration parameters used by the Merge Schema API.
 *
 * @param schemas schemas to merge.
 * @param semanticConfig properties related to semantic config.
 */
public record Parameters(
    List<Schema> schemas,
    SemanticConfig semanticConfig) {

    /**
     * Represents the semantic configuration for the Merge Schema API.
     *
     * @param defaultModelName the model name to use.
     */
    public record SemanticConfig(String defaultModelName) {}
}
