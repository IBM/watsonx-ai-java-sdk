/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import java.util.List;

/**
 * Represents the response body containing the embeddings for a given text.
 *
 * @param modelId the model identifier used for embedding generation
 * @param createdAt the timestamp when the embeddings were created
 * @param results the list of embedding results
 * @param inputTokenCount the total number of input tokens processed
 */
public record EmbeddingResponse(
    String modelId,
    String createdAt,
    List<Result> results,
    Integer inputTokenCount) {

    /**
     * Represents the embedding result for a given input text. Each embedding contains a list of floating point numbers representing the embedding
     * values.
     *
     * @param embedding A list of float values representing the embedding of the input text.
     */
    public record Result(List<Float> embedding, String input) {}
}

