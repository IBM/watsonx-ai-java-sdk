/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import static java.util.Objects.isNull;
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

    public EmbeddingResponse {
        results = isNull(results) ? null : List.copyOf(results);
    }

    /**
     * Represents the embedding result for a given input text. Each embedding contains a list of floating point numbers representing the embedding
     * values.
     *
     * @param embedding A list of float values representing the embedding of the input text.
     * @param input the input text that was embedded
     */
    public record Result(List<Float> embedding, String input) {
        public Result {
            embedding = isNull(embedding) ? null : List.copyOf(embedding);
        }
    }
}

