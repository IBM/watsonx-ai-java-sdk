package com.ibm.watsonx.runtime.embedding;

import java.util.List;

/**
 * Represents the response body containing the embeddings for a given text.
 * 
 * @param modelId The id of the model used for the request. It identifies which model generated the embeddings.
 * @param createdAt The timestamp when the response was created, in ISO 8601 format.
 * @param results A list of embedding results for the given input text. It contains the actual embedding values.
 * @param inputTokenCount The number of input tokens that were consumed for this request.
 */
public record EmbeddingResponse(
    String modelId,
    String createdAt,
    List<Result> results,
    Integer inputTokenCount) {

    /**
     * Represents the embedding result for a given input text. Each embedding contains a list of floating point numbers representing the embedding values.
     *
     * @param embedding A list of float values representing the embedding of the input text.
     */
    public record Result(List<Float> embedding) {}
}

