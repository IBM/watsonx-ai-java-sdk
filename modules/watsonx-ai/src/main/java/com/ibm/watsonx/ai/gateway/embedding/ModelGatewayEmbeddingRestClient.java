/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.embedding;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Model Gateway embeddings API.
 */
public abstract class ModelGatewayEmbeddingRestClient extends WatsonxRestClient {

    protected ModelGatewayEmbeddingRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Sends a synchronous embedding request to the Model Gateway.
     *
     * @param model the model ID to use for generating embeddings
     * @param request the {@link ModelGatewayEmbeddingRequest} containing the input and optional parameters
     * @return a {@link ModelGatewayEmbeddingResponse} containing the generated embeddings
     */
    public abstract ModelGatewayEmbeddingResponse embed(String model, ModelGatewayEmbeddingRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link ModelGatewayEmbeddingRestClientBuilderFactory} discovered via
     * {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ModelGatewayEmbeddingRestClient.Builder builder() {
        return ServiceLoader.load(ModelGatewayEmbeddingRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ModelGatewayEmbeddingRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ModelGatewayEmbeddingRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface ModelGatewayEmbeddingRestClientBuilderFactory extends Supplier<ModelGatewayEmbeddingRestClient.Builder> {}
}
