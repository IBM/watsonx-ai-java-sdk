/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Text Embeddings APIs.
 */
public abstract class EmbeddingRestClient extends WatsonxRestClient {

    protected EmbeddingRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Executes an embedding request against the watsonx.ai API.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param embeddingRequest the structured embedding request payload
     * @return An {@link EmbeddingResponse} containing the embedding vectors and metadata
     */
    public abstract EmbeddingResponse embedding(String transactionId, EmbeddingRequest embeddingRequest);

    /**
     * Creates a new {@link Builder} using the first available {@link EmbeddingRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static EmbeddingRestClient.Builder builder() {
        return ServiceLoader.load(EmbeddingRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link EmbeddingRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<EmbeddingRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface EmbeddingRestClientBuilderFactory extends Supplier<EmbeddingRestClient.Builder> {}
}
