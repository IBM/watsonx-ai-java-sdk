/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.rerank;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Rerank APIs.
 */
public abstract class RerankRestClient extends WatsonxRestClient {

    protected RerankRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Executes a rerank request against the watsonx.ai Rerank API.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param request the rerank request payload
     * @return A {@link RerankResponse} containing the reranked results.
     */
    public abstract RerankResponse rerank(String transactionId, RerankRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link RerankRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static RerankRestClient.Builder builder() {
        return ServiceLoader.load(RerankRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link RerankRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<RerankRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface RerankRestClientBuilderFactory extends Supplier<RerankRestClient.Builder> {}
}
