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
import com.ibm.watsonx.ai.chat.ChatClientContext;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.textgeneration.TextRequest;
import com.ibm.watsonx.ai.timeseries.ForecastRequest;
import com.ibm.watsonx.ai.timeseries.ForecastResponse;

/**
 * Abstraction of a REST client for interacting with the IBM watsonx.ai Deployment APIs.
 */
public abstract class DeploymentRestClient extends WatsonxRestClient {

    protected DeploymentRestClient(Builder builder) {
        super(builder);
    }

    /**
     * Retrieves deployment details for a given deployment, project, or space identifier.
     *
     * @param parameters the request containing deployment identifiers
     * @return the {@link DeploymentResource} containing deployment metadata
     */
    public abstract DeploymentResource findById(FindByIdRequest parameters);

    /**
     * Generates text synchronously using a specified deployment.
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param deploymentId the deployment to execute the request on
     * @param timeout the maximum duration to wait for the response
     * @param textRequest the structured text generation request
     * @return a {@link TextGenerationResponse} containing the generated text and metadata
     */
    public abstract TextGenerationResponse generate(
        String transactionId,
        String deploymentId,
        Duration timeout,
        TextRequest textRequest);

    /**
     * Generates text asynchronously using streaming with a specified deployment.
     * <p>
     * Partial results are delivered incrementally to the provided {@link TextGenerationHandler}.
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param deploymentId the deployment to execute the request on
     * @param timeout the maximum duration to wait for completion
     * @param textRequest the structured text generation request
     * @param handler the {@link TextGenerationHandler} receiving streaming events
     * @return a {@link CompletableFuture} that completes when streaming finishes or fails
     */
    public abstract CompletableFuture<Void> generateStreaming(
        String transactionId,
        String deploymentId,
        Duration timeout,
        TextRequest textRequest,
        TextGenerationHandler handler);

    /**
     * Sends a synchronous chat request to a deployment.
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param deploymentId the deployment to execute the request on
     * @param timeout the maximum duration to wait for the response
     * @param textChatRequest the structured chat request
     * @return a {@link ChatResponse} containing the assistant's reply
     */
    public abstract ChatResponse chat(
        String transactionId,
        String deploymentId,
        Duration timeout,
        TextChatRequest textChatRequest);

    /**
     * Sends an asynchronous streaming chat request to a deployment.
     * <p>
     * Partial results are delivered incrementally to the provided {@link ChatHandler}.
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param deploymentId the deployment to execute the request on
     * @param textChatRequest the structured chat request
     * @param context the {@link ChatClientContext} containing additional data needed by the client
     * @param handler the {@link ChatHandler} instance that receives streaming events
     * @return a {@link CompletableFuture} that completes when the stream finishes or fails
     */
    public abstract CompletableFuture<ChatResponse> chatStreaming(
        String transactionId,
        String deploymentId,
        TextChatRequest textChatRequest,
        ChatClientContext context,
        ChatHandler handler);

    /**
     * Generates a time series forecast using a specified deployment.
     *
     * @param transactionId an optional transaction identifier for tracing
     * @param deploymentId the deployment to execute the request on
     * @param timeout the maximum duration to wait for the response
     * @param forecastRequest the structured forecast request
     * @return a {@link ForecastResponse} containing predicted time series values
     */
    public abstract ForecastResponse forecast(
        String transactionId,
        String deploymentId,
        Duration timeout,
        ForecastRequest forecastRequest);

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
}
