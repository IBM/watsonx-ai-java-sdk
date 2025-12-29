/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.rerank;

import java.util.List;
import com.ibm.watsonx.ai.Crypto;

/**
 * Represents a request to perform text reranking using a specified model.
 *
 * @param modelId The identifier of the reranking model to use.
 * @param inputs The list of input texts to be reranked.
 * @param query The query text to rank the inputs against.
 * @param spaceId The deployment space identifier.
 * @param projectId The project identifier.
 * @param parameters Additional parameters for the reranking operation.
 * @param crypto Encryption configuration for sensitive data.
 */
public record RerankRequest(
    String modelId,
    List<RerankInput> inputs,
    String query,
    String spaceId,
    String projectId,
    Parameters parameters,
    Crypto crypto) {

    /**
     * Represents a single input text to be reranked.
     *
     * @param text The input text content.
     */
    public record RerankInput(String text) {}

    /**
     * Additional parameters for controlling the reranking behavior.
     *
     * @param truncateInputTokens Maximum number of tokens per input.
     * @param returnOptions Options for controlling what data is returned.
     */
    public record Parameters(Integer truncateInputTokens, ReturnOptions returnOptions) {}

    /**
     * Options for controlling what data is included in the response.
     *
     * @param topN Number of top results to return.
     * @param inputs Whether to include input texts in the response.
     * @param query Whether to include the query in the response.
     */
    public record ReturnOptions(Integer topN, boolean inputs, Boolean query) {}
}
