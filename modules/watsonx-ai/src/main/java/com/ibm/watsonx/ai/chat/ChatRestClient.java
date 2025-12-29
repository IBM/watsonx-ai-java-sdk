/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Text Chat APIs.
 */
public abstract class ChatRestClient extends WatsonxRestClient {

    protected ChatRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Executes a synchronous chat request against the watsonx.ai API.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param textChatRequest the structured chat request payload
     * @return the complete {@link ChatResponse} returned by watsonx.ai
     */
    public abstract ChatResponse chat(String transactionId, TextChatRequest textChatRequest);

    /**
     * Executes an asynchronous streaming chat request against the watsonx.ai API.
     * <p>
     * Partial results are delivered incrementally to the provided {@link ChatHandler}.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing
     * @param textChatRequest the structured chat request payload
     * @param context the {@link ChatClientContext} containing additional data needed by the client
     * @param handler the {@link ChatHandler} instance that receives streaming events
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public abstract CompletableFuture<ChatResponse> chatStreaming(
        String transactionId,
        TextChatRequest textChatRequest,
        ChatClientContext context,
        ChatHandler handler);

    /**
     * Creates a new {@link Builder} using the first available {@link ChatRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static ChatRestClient.Builder builder() {
        return ServiceLoader.load(ChatRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link ChatRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<ChatRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface ChatRestClientBuilderFactory extends Supplier<ChatRestClient.Builder> {}
}
