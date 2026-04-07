/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import java.util.List;

/**
 * Represents an embedding request.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var parameters = EmbeddingParameters.builder()
 *     .truncateInputTokens(512)
 *     .returnOptions(ReturnOptions.builder().inputText(true).build())
 *     .build();
 *
 * EmbeddingRequest request = EmbeddingRequest.builder()
 *     .inputs("What is watsonx.ai?")
 *     .parameters(parameters)
 *     .build();
 * }</pre>
 */
public final class EmbeddingRequest {
    private final List<String> inputs;
    private final EmbeddingParameters parameters;

    private EmbeddingRequest(Builder builder) {
        inputs = builder.inputs;
        parameters = builder.parameters;
    }

    /**
     * Returns the input texts to be converted into embeddings.
     *
     * @return the list of input texts, or {@code null} if not set
     */
    public List<String> inputs() {
        return inputs;
    }

    /**
     * Returns the embedding parameters.
     *
     * @return the embedding parameters, or {@code null} if not set
     */
    public EmbeddingParameters parameters() {
        return parameters;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * var parameters = EmbeddingParameters.builder()
     *     .truncateInputTokens(512)
     *     .returnOptions(ReturnOptions.builder().inputText(true).build())
     *     .build();
     *
     * EmbeddingRequest request = EmbeddingRequest.builder()
     *     .inputs("What is watsonx.ai?")
     *     .parameters(parameters)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link EmbeddingRequest} instances.
     */
    public final static class Builder {
        private List<String> inputs;
        private EmbeddingParameters parameters;

        private Builder() {}

        /**
         * Sets the input texts for the request, replacing any existing inputs.
         *
         * @param inputs the list of input texts to embed
         */
        public Builder inputs(List<String> inputs) {
            this.inputs = inputs;
            return this;
        }

        /**
         * Sets the parameters controlling the embedding model behavior.
         *
         * @param parameters an {@link EmbeddingParameters} instance
         */
        public Builder parameters(EmbeddingParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Builds a {@link EmbeddingRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link EmbeddingRequest}
         */
        public EmbeddingRequest build() {
            return new EmbeddingRequest(this);
        }
    }
}
