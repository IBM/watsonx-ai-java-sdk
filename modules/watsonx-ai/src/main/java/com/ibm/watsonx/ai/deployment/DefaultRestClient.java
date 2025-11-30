/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatSubscriber;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.textgeneration.TextGenerationSubscriber;
import com.ibm.watsonx.ai.textgeneration.TextRequest;
import com.ibm.watsonx.ai.timeseries.ForecastRequest;
import com.ibm.watsonx.ai.timeseries.ForecastResponse;

/**
 * Default implementation of the {@link DeploymentRestClient} abstract class.
 */
final class DefaultRestClient extends DeploymentRestClient {

    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticationProvider, "authenticationProvider is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticationProvider, LogMode.of(logRequests, logResponses));
        asyncHttpClient = HttpClientFactory.createAsync(authenticationProvider, LogMode.of(logRequests, logResponses));
    }

    @Override
    public DeploymentResource findById(FindByIdRequest parameters) {

        var deploymentId = parameters.getDeploymentId();

        StringJoiner queryParameters = new StringJoiner("&", "?", "");

        if (nonNull(parameters.getProjectId()))
            queryParameters.add("project_id=".concat(parameters.getProjectId()));

        if (nonNull(parameters.getSpaceId()))
            queryParameters.add("space_id=".concat(parameters.getSpaceId()));

        queryParameters.add("version=".concat(version));

        var httpRequest = HttpRequest
            .newBuilder(URI.create(baseUrl + "/ml/v4/deployments/%s%s".formatted(deploymentId, queryParameters.toString())))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(timeout.toMillis()))
            .GET();

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), DeploymentResource.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TextGenerationResponse generate(String transactionId, String deploymentId, Duration timeout, TextRequest textRequest) {

        var url = URI.create(baseUrl + "/ml/v1/deployments/%s/text/generation?version=%s".formatted(deploymentId, version));

        var httpRequest = HttpRequest
            .newBuilder(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(timeout)
            .POST(BodyPublishers.ofString(toJson(textRequest)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TextGenerationResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> generateStreaming(
        String transactionId,
        String deploymentId,
        Duration timeout,
        TextRequest textRequest,
        TextGenerationHandler handler) {

        var url = URI.create(baseUrl + "/ml/v1/deployments/%s/text/generation_stream?version=%s".formatted(deploymentId, version));

        var httpRequest = HttpRequest
            .newBuilder(url)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .timeout(timeout)
            .POST(BodyPublishers.ofString(toJson(textRequest)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        var subscriber = textGenerationSubscriber(handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> TextGenerationSubscriber.handleError(t, handler));
    }

    @Override
    public ChatResponse chat(String transactionId, String deploymentId, Duration timeout, TextChatRequest textChatRequest) {

        var url = URI.create(baseUrl + "/ml/v1/deployments/%s/text/chat?version=%s".formatted(deploymentId, version));

        var httpRequest =
            HttpRequest
                .newBuilder(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(textChatRequest)))
                .timeout(timeout);

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), ChatResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> chatStreaming(
        String transactionId,
        String deploymentId,
        Duration timeout,
        ExtractionTags extractionTags,
        TextChatRequest textChatRequest,
        ChatHandler handler) {

        var url = URI.create(baseUrl + "/ml/v1/deployments/%s/text/chat_stream?version=%s".formatted(deploymentId, version));

        var httpRequest =
            HttpRequest.newBuilder(url)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(BodyPublishers.ofString(toJson(textChatRequest)))
                .timeout(timeout);

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        var subscriber = chatSubscriber(textChatRequest.getToolChoiceOption(), ChatSubscriber.toolHasParameters(textChatRequest.getTools()),
            extractionTags, handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> ChatSubscriber.handleError(t, handler));
    }

    @Override
    public ForecastResponse forecast(String transactionId, String deploymentId, Duration timeout, ForecastRequest forecastRequest) {

        var url = URI.create(baseUrl + "/ml/v1/deployments/%s/time_series/forecast?version=%s".formatted(deploymentId, version));

        var httpRequest = HttpRequest
            .newBuilder(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(timeout)
            .POST(BodyPublishers.ofString(toJson(forecastRequest)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), ForecastResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a subscriber that listens to raw SSE messages from the chat stream, and delegates processing to a {@link TextGenerationSubscriber}.
     *
     * @param handler the handler that receives processed chat events
     * @return a {@link Flow.Subscriber} suitable for consumption by the HTTP client
     */
    private Flow.Subscriber<String> textGenerationSubscriber(TextGenerationHandler handler) {

        return new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private volatile boolean success = true;
            private volatile TextGenerationSubscriber chatSubscriber = TextGenerationSubscriber.createSubscriber(handler);

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(String partialMessage) {
                try {

                    chatSubscriber.onNext(partialMessage);

                } catch (RuntimeException e) {

                    onError(e);
                    success = !handler.failOnFirstError();

                } finally {
                    if (success)
                        subscription.request(1);
                    else
                        subscription.cancel();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                chatSubscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                chatSubscriber.onComplete();
            }
        };
    }

    /**
     * Creates a subscriber that listens to raw SSE messages from the chat stream, and delegates processing to a {@link ChatSubscriber}.
     *
     * @param toolChoiceOption tool selection strategy (e.g. "auto", "required")
     * @param toolHasParameters map of tool names
     * @param extractionTags optional tags for reasoning/extraction
     * @param handler the handler that receives processed chat events
     * @return a {@link Flow.Subscriber} suitable for consumption by the HTTP client
     */
    private Flow.Subscriber<String> chatSubscriber(
        String toolChoiceOption,
        Map<String, Boolean> toolHasParameters,
        ExtractionTags extractionTags,
        ChatHandler handler) {

        return new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private volatile boolean success = true;
            private volatile ChatSubscriber chatSubscriber =
                ChatSubscriber.createSubscriber(toolChoiceOption, toolHasParameters, extractionTags, handler);

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(String partialMessage) {
                try {

                    chatSubscriber.onNext(partialMessage);

                } catch (RuntimeException e) {

                    onError(e);
                    success = !handler.failOnFirstError();

                } finally {
                    if (success)
                        subscription.request(1);
                    else
                        subscription.cancel();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                chatSubscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                chatSubscriber.onComplete();
            }
        };
    }

    /**
     * Returns a new {@link Builder} instance.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DefaultRestClient} instances with configurable parameters.
     */
    public final static class Builder extends DeploymentRestClient.Builder {

        private Builder() {}

        /**
         * Builds a {@link DefaultRestClient} instance using the configured parameters.
         *
         * @return a new instance of {@link DefaultRestClient}
         */
        public DefaultRestClient build() {
            return new DefaultRestClient(this);
        }
    }
}
