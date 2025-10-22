/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Tokenization APIs.
 */
public abstract class TokenizationRestClient extends WatsonxRestClient {

    protected TokenizationRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Executes a synchronous tokenization request against the watsonx.ai API.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param request The {@link TokenizationRequest} request.
     * @return A {@link TokenizationResponse} containing the tokenized representation of the input text and related metadata.
     */
    public abstract TokenizationResponse tokenize(String transactionId, TokenizationRequest request);

    /**
     * Executes an asynchronous tokenization request against the watsonx.ai API.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param request The {@link TokenizationRequest} request.
     * @return A {@link CompletableFuture} resolving to a {@link TokenizationResponse} containing the tokenized representation of the input text and
     *         related metadata.
     */
    public abstract CompletableFuture<TokenizationResponse> asyncTokenize(String transactionId, TokenizationRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link TokenizationRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static TokenizationRestClient.Builder builder() {
        return ServiceLoader.load(TokenizationRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link TokenizationRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<TokenizationRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface TokenizationRestClientBuilderFactory extends Supplier<TokenizationRestClient.Builder> {}
}
