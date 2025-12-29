/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import java.util.List;
import com.ibm.watsonx.ai.Crypto;

/**
 * Represents a request to generate embeddings from a given model.
 *
 * @param modelId the model identifier
 * @param spaceId the space identifier
 * @param projectId the project identifier
 * @param inputs the list of input texts to embed
 * @param parameters the embedding parameters
 * @param crypto the crypto configuration for encryption
 */
public record EmbeddingRequest(String modelId, String spaceId, String projectId,
    List<String> inputs, Parameters parameters, Crypto crypto) {

    /**
     * Parameters for embedding generation.
     *
     * @param truncateInputTokens the maximum number of tokens accepted per input
     * @param returnOptions the return options
     */
    public record Parameters(Integer truncateInputTokens, ReturnOptions returnOptions) {}

    /**
     * Return options for embedding generation.
     *
     * @param inputText whether to include the input text in each result document
     */
    public record ReturnOptions(boolean inputText) {}
}
