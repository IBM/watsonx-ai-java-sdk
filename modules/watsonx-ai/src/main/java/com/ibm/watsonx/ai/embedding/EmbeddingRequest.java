/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import java.util.List;
import com.ibm.watsonx.ai.Crypto;

/**
 * Represents a request to generate embeddings from a given model.
 */
public record EmbeddingRequest(String modelId, String spaceId, String projectId,
    List<String> inputs, Parameters parameters, Crypto crypto) {

    public record Parameters(Integer truncateInputTokens, ReturnOptions returnOptions) {}
    public record ReturnOptions(boolean inputText) {}
}
