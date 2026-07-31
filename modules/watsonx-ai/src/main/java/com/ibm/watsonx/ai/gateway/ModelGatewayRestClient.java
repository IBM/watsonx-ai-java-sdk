/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */


package com.ibm.watsonx.ai.gateway;

import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;
import com.ibm.watsonx.ai.chat.ChatClientContext;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Model Gateway APIs.
 */
public abstract class ModelGatewayRestClient extends WatsonxRestClient {

    protected ModelGatewayRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Sends a synchronous chat request to the Model Gateway.
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param timeout the maximum duration to wait for the response
     * @param gatewayRequest the structured gateway chat request
     * @return a {@link GatewayChatResponse} containing the assistant's reply
     */
    public abstract GatewayChatResponse chat(
        String transactionId,
        Duration timeout,
        GatewayTextChatRequest gatewayRequest);

    /**
     * Sends an asynchronous streaming chat request to the Model Gateway.
     * <p>
     * Partial results are delivered incrementally to the provided {@link ChatHandler}, and the returned future completes with the accumulated
     * {@link ChatResponse} once the stream finishes.
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param gatewayRequest the structured gateway chat request
     * @param context the {@link ChatClientContext} containing additional data needed by the client
     * @param handler the {@link ChatHandler} instance that receives streaming events
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public abstract CompletableFuture<ChatResponse> chatStreaming(
        String transactionId,
        GatewayTextChatRequest gatewayRequest,
        ChatClientContext context,
        ChatHandler handler);

    /**
     * Creates a new {@link Builder} using the first available {@link ModelGatewayRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ModelGatewayRestClient.Builder builder() {
        return ServiceLoader.load(ModelGatewayRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ModelGatewayRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ModelGatewayRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface ModelGatewayRestClientBuilderFactory extends Supplier<ModelGatewayRestClient.Builder> {}
}
