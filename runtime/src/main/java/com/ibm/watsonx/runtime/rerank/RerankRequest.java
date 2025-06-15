package com.ibm.watsonx.runtime.rerank;

import java.util.List;

/**
 * Represents a request to perform text reranking using a specified model.
 */
public record RerankRequest(
    String modelId,
    List<RerankInput> inputs,
    String query,
    String spaceId,
    String projectId,
    Parameters parameters) {
       
    public record RerankInput(String text) {}
    public record Parameters(Integer truncateInputTokens, ReturnOptions returnOptions) {}
    public record ReturnOptions(Integer topN, boolean inputs, Boolean query) {}
}
