package com.ibm.watsonx.runtime.embedding;

import java.util.List;

/**
 * Represents a request to generate embeddings from a given model for the provided input text.
 */
public record EmbeddingRequest(String modelId, String spaceId, String projectId,
    List<String> inputs, Parameters parameters) {

    public static class Parameters {
        private Integer truncateInputTokens;

        public Parameters(Integer truncateInputTokens) {
            this.truncateInputTokens = truncateInputTokens;
        }

        public Integer getTruncateInputTokens() {
            return truncateInputTokens;
        }
    }
}
