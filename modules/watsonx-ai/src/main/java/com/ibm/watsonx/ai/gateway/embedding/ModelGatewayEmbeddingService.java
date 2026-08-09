/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.embedding;

import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service for interacting with IBM watsonx.ai Model Gateway embeddings APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ModelGatewayEmbeddingService embeddingService = ModelGatewayEmbeddingService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .modelId("text-embedding-3-small")
 *     .build();
 *
 * ModelGatewayEmbeddingResponse response = embeddingService.embed("Hello, world!");
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class ModelGatewayEmbeddingService extends WatsonxService {

    private final ModelGatewayEmbeddingRestClient client;
    private final String modelId;

    private ModelGatewayEmbeddingService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        modelId = requireNonNull(builder.modelId, "The modelId must be provided");

        client = ModelGatewayEmbeddingRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
    }

    /**
     * Generates embeddings for the provided input texts.
     *
     * @param inputs the texts to embed
     * @return a {@link ModelGatewayEmbeddingResponse} containing the generated embeddings
     */
    public ModelGatewayEmbeddingResponse embed(String... inputs) {
        requireNonNull(inputs, "inputs cannot be null");
        return embed(Arrays.asList(inputs));
    }

    /**
     * Generates embeddings for the provided input texts.
     *
     * @param inputs the list of texts to embed
     * @return a {@link ModelGatewayEmbeddingResponse} containing the generated embeddings
     */
    public ModelGatewayEmbeddingResponse embed(List<String> inputs) {
        return embed(inputs, null);
    }

    /**
     * Generates embeddings for the provided input texts.
     *
     * @param inputs the list of texts to embed
     * @param parameters the parameters for the embedding request
     * @return a {@link ModelGatewayEmbeddingResponse} containing the generated embeddings
     */
    public ModelGatewayEmbeddingResponse embed(List<String> inputs, ModelGatewayEmbeddingParameters parameters) {
        return embed(
            ModelGatewayEmbeddingRequest.builder()
                .input(inputs)
                .parameters(parameters)
                .build()
        );
    }

    /**
     * Generates embeddings for the provided request.
     *
     * @param request the {@link ModelGatewayEmbeddingRequest}
     * @return a {@link ModelGatewayEmbeddingResponse} containing the generated embeddings
     */
    public ModelGatewayEmbeddingResponse embed(ModelGatewayEmbeddingRequest request) {
        requireNonNull(request, "request cannot be null");
        requireNonNull(request.input(), "input cannot be null");

        if (request.input().isEmpty())
            throw new IllegalArgumentException("At least one input must be provided");

        return client.embed(ModelGatewayEmbeddingPayload.of(modelId, request));
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
     * Builder class for constructing {@link ModelGatewayEmbeddingService} instances with configurable parameters.
     */
    public static final class Builder extends WatsonxService.Builder<Builder> {

        private String modelId;

        private Builder() {}

        /**
         * Sets the model identifier to use for generating embeddings (e.g., {@code "text-embedding-3-small"}).
         *
         * @param modelId the model id
         */
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /**
         * Builds a {@link ModelGatewayEmbeddingService} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayEmbeddingService}
         */
        public ModelGatewayEmbeddingService build() {
            return new ModelGatewayEmbeddingService(this);
        }
    }
}
