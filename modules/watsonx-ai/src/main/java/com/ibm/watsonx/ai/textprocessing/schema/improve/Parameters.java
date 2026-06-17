/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

import com.ibm.watsonx.ai.textprocessing.Schema;

/**
 * Represents the configuration parameters used by the Improve Schema API.
 *
 * @param schema schema to improve.
 * @param semanticConfig properties related to semantic config.
 */
public record Parameters(
    Schema schema,
    SemanticConfig semanticConfig) {

    /**
     * Represents the semantic configuration for the Create Schema API.
     *
     * @param defaultModelName the model name to use.
     */
    public record SemanticConfig(String defaultModelName) {}
}
