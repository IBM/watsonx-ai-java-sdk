/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.rerank;

import java.util.List;

/**
 * Represents the response returned from a reranking operation.
 *
 * @param modelId The identifier of the model used for reranking.
 * @param results The list of reranked results with scores.
 * @param createdAt The timestamp when the response was created.
 * @param inputTokenCount The total number of input tokens processed.
 * @param modelVersion The version of the model used.
 * @param query The original query text used for reranking.
 */
public record RerankResponse(
    String modelId,
    List<RerankResult> results,
    String createdAt,
    int inputTokenCount,
    String modelVersion,
    String query) {

    /**
     * Represents an input text that was reranked.
     *
     * @param text The input text content.
     */
    public record RerankInputResult(String text) {}

    /**
     * Represents a single reranked result with its relevance score.
     *
     * @param index The original index of the input in the request.
     * @param score The relevance score assigned by the reranking model.
     * @param input The input text that was reranked.
     */
    public record RerankResult(int index, Double score, RerankInputResult input) {}
}
