/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.WatsonxRestClient;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.textgeneration.TextRequest;
import com.ibm.watsonx.ai.timeseries.ForecastResponse;


/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Deployment APIs.
 */
public abstract class DeploymentRestClient extends WatsonxRestClient {

    protected DeploymentRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Retrieves deployment details with the specified parameters.
     *
     * @param parameters the request containing deployment, project or space identifiers
     * @return the {@link DeploymentResource} with the retrieved deployment details
     */
    public abstract DeploymentResource findById(FindByIdRequest parameters);

    /**
     * Generates text based on the provided {@link GenerateRequest}.
     *
     * @param request the text generation request
     * @return a {@link TextGenerationResponse} containing the generated text and associated metadata
     */
    public abstract TextGenerationResponse generate(GenerateRequest request);

    /**
     * Sends a streaming text generation request
     *
     * @param request the text generation request
     * @return a {@link CompletableFuture} that completes when the streaming is finished
     */
    public abstract CompletableFuture<Void> generateStreaming(GenerateStreamingRequest request);

    /**
     * Sends a chat request to the model using the provided messages, tools, and parameters.
     *
     * @param request the chat request
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public abstract ChatResponse chat(ChatRequest request);

    /**
     * Sends a streaming chat request using the provided messages, tools, and parameters.
     *
     * @param request the chat request
     * @return a {@link CompletableFuture} that completes when the streaming is finished
     */
    public abstract CompletableFuture<Void> chatStreaming(ChatStreamingRequest request);

    /**
     * Generates a forecast using the provided {@link ForecastRequest}.
     *
     * @param request the forecast request.
     * @return a {@link ForecastResponse} containing the forecasted time series values
     */
    public abstract ForecastResponse forecast(ForecastRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link DeploymentRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    static DeploymentRestClient.Builder builder() {
        return ServiceLoader.load(DeploymentRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link DeploymentRestClient} instances with configurable parameters.
     */
    public abstract static class Builder extends WatsonxRestClient.Builder<DeploymentRestClient, Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    public interface DeploymentRestClientBuilderFactory extends Supplier<DeploymentRestClient.Builder> {}

    public record GenerateRequest(
        String transactionId,
        String deploymentId,
        Duration timeout,
        TextRequest textRequest) {}

    public record GenerateStreamingRequest(
        String transactionId,
        String deploymentId,
        Duration timeout,
        TextRequest textRequest,
        TextGenerationHandler handler) {}

    public record ChatRequest(
        String transactionId,
        String deploymentId,
        Duration timeout,
        TextChatRequest textChatRequest) {}

    public record ChatStreamingRequest(
        String transactionId,
        String deploymentId,
        Duration timeout,
        ExtractionTags extractionTags,
        TextChatRequest textChatRequest,
        ChatHandler handler) {}

    public record ForecastRequest(
        String transactionId,
        String deploymentId,
        Duration timeout,
        com.ibm.watsonx.ai.timeseries.ForecastRequest forecastRequest) {}
}
