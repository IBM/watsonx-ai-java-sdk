package com.ibm.watsonx.runtime.rerank;

import java.util.List;

/**
 * Represents the response returned from a reranking operation.
 */
public record RerankResponse(
    String modelId,
    List<RerankResult> results,
    String createdAt,
    int inputTokenCount,
    String modelVersion,
    String query) {

    public record RerankInputResult(String text) {}
    public record RerankResult(int index, Double score, RerankInputResult input) {} 
}
