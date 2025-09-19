/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Text Generation APIs.
 */
public abstract class TextGenerationRestClient extends WatsonxRestClient {

    protected TextGenerationRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Generates text based on the provided {@link TextRequest}.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing.
     * @param textRequest the {@link TextRequest} containing input, moderation, parameters.
     * @return a {@link TextGenerationResponse} containing the generated text and associated metadata
     */
    public abstract TextGenerationResponse generate(String transactionId, TextRequest textRequest);

    /**
     * Sends a streaming text generation request based on the provided {@link TextRequest}.
     * <p>
     * This method initiates an asynchronous text generation operation where partial responses are delivered incrementally through the provided
     * {@link TextGenerationHandler}.
     *
     * @param transactionId an optional client-provided transaction identifier used for tracing.
     * @param textRequest the {@link TextRequest} containing input, moderation, parameters, and optional deployment ID
     * @param handler the handler that will receive streamed generation events
     * @return a {@link CompletableFuture} that completes when the streaming generation is finished
     */
    public abstract CompletableFuture<Void> generateStreaming(String transactionId, TextRequest textRequest, TextGenerationHandler handler);

    /**
     * Creates a new {@link Builder} using the first available {@link TextGenerationRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static TextGenerationRestClient.Builder builder() {
        return ServiceLoader.load(TextGenerationRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link TextGenerationRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<TextGenerationRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks (e.g., Quarkus, Spring) to provide their own client implementations.
     */
    public interface TextGenerationRestClientBuilderFactory extends Supplier<TextGenerationRestClient.Builder> {}
}
