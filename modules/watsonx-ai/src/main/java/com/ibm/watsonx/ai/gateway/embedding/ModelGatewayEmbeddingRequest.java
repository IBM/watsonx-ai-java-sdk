/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.embedding;

import static java.util.Objects.isNull;
import java.util.List;

/**
 * Represents an embedding request for the {@link ModelGatewayEmbeddingService}.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var parameters = ModelGatewayEmbeddingParameters.builder()
 *     .encodingFormat(EncodingFormat.FLOAT)
 *     .build();
 *
 * ModelGatewayEmbeddingRequest request = ModelGatewayEmbeddingRequest.builder()
 *     .input("Hello, world!")
 *     .parameters(parameters)
 *     .build();
 * }</pre>
 *
 * @see ModelGatewayEmbeddingService
 * @see ModelGatewayEmbeddingParameters
 */
public final class ModelGatewayEmbeddingRequest {

    private final List<String> input;
    private final ModelGatewayEmbeddingParameters parameters;

    private ModelGatewayEmbeddingRequest(Builder builder) {
        input = isNull(builder.input) ? null : List.copyOf(builder.input);
        parameters = builder.parameters;
    }

    /**
     * Returns the input texts to embed.
     *
     * @return the list of input texts, or {@code null} if not set
     */
    public List<String> input() {
        return input;
    }

    /**
     * Returns the embedding parameters.
     *
     * @return the embedding parameters, or {@code null} if not set
     */
    public ModelGatewayEmbeddingParameters parameters() {
        return parameters;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ModelGatewayEmbeddingRequest} instances.
     */
    public static final class Builder {

        private List<String> input;
        private ModelGatewayEmbeddingParameters parameters;

        private Builder() {}

        /**
         * Sets the input texts to embed, replacing any existing input.
         *
         * @param input the input texts
         */
        public Builder input(String... input) {
            this.input = isNull(input) ? null : List.of(input);
            return this;
        }

        /**
         * Sets the input texts to embed, replacing any existing input.
         *
         * @param input the list of input texts
         */
        public Builder input(List<String> input) {
            this.input = isNull(input) ? null : List.copyOf(input);
            return this;
        }

        /**
         * Sets the parameters controlling the embedding model behavior.
         *
         * @param parameters a {@link ModelGatewayEmbeddingParameters} instance
         */
        public Builder parameters(ModelGatewayEmbeddingParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Builds a {@link ModelGatewayEmbeddingRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayEmbeddingRequest}
         */
        public ModelGatewayEmbeddingRequest build() {
            return new ModelGatewayEmbeddingRequest(this);
        }
    }

    @Override
    public String toString() {
        return "ModelGatewayEmbeddingRequest [input=" + input + ", parameters=" + parameters + "]";
    }
}
