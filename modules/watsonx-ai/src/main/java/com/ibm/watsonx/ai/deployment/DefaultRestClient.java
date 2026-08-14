/*
 * Copyright 2025 IBM Corporation
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
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.ibm.watsonx.ai.chat.ChatClientContext;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.SseEventProcessor;
import com.ibm.watsonx.ai.chat.TextChatResponse;
import com.ibm.watsonx.ai.chat.decorator.ChatHandlerDecorator;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.streaming.DefaultChatSubscriber;
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
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
        asyncHttpClient = HttpClientFactory.createAsync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public DeploymentResource findById(FindByIdRequest parameters) {

        var deploymentId = parameters.deploymentId();

        StringJoiner queryParameters = new StringJoiner("&", "?", "");

        if (nonNull(parameters.projectId()))
            queryParameters.add("project_id=".concat(parameters.projectId()));

        if (nonNull(parameters.spaceId()))
            queryParameters.add("space_id=".concat(parameters.spaceId()));

        queryParameters.add("version=".concat(version));

        var httpRequest = HttpRequest
            .newBuilder(URI.create(baseUrl + "/ml/v4/deployments/%s%s".formatted(deploymentId, queryParameters.toString())))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(timeout.toMillis()))
            .GET();

        if (nonNull(parameters.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), DeploymentResource.class);

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

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), TextGenerationResponse.class);

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

        var response = new CompletableFuture<Void>();
        var subscriber = new CancellableTextGenerationSubscriber(handler);
        var httpFuture = asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber));

        httpFuture
            .thenAccept(r -> response.complete(null))
            .exceptionally(t -> {
                if (subscriber.isCancelled())
                    return null;

                TextGenerationSubscriber.handleError(t, handler);
                response.completeExceptionally(nonNull(t.getCause()) ? t.getCause() : t);
                return null;
            });

        response.whenComplete((r, t) -> {
            if (response.isCancelled()) {
                subscriber.cancelStream();
                httpFuture.cancel(true);
            }
        });

        return response;
    }

    @Override
    public TextChatResponse chat(String transactionId, String deploymentId, Duration timeout, TextChatRequest textChatRequest) {

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

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), TextChatResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(
        String transactionId,
        String deploymentId,
        TextChatRequest textChatRequest,
        ChatClientContext<DeploymentChatRequest> context,
        ChatHandler handler) {

        var url = URI.create(baseUrl + "/ml/v1/deployments/%s/text/chat_stream?version=%s".formatted(deploymentId, version));

        var httpRequest =
            HttpRequest.newBuilder(url)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(BodyPublishers.ofString(toJson(textChatRequest)))
                .timeout(Duration.ofMillis(textChatRequest.timeLimit()));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        var response = new CompletableFuture<ChatResponse>();
        var interceptorContext = new InterceptorContext<>(context.chatProvider(), context.chatRequest(), null);
        var chatSubscriber =
            new DefaultChatSubscriber(
                new SseEventProcessor(textChatRequest.tools(), context.extractionTags(), TextChatResponse::builder),
                new ChatHandlerDecorator<>(handler, interceptorContext, context.toolInterceptor())
            );

        var subscriber = chatSubscriber.asFlowSubscriber(response, !handler.failOnFirstError());
        var httpFuture = asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber));

        httpFuture
            .thenAccept(r -> {})
            .exceptionally(t -> {
                if (chatSubscriber.isCancelled())
                    return null;

                Throwable cause = nonNull(t.getCause()) ? t.getCause() : t;
                if (chatSubscriber.markErrorReported())
                    handler.onError(cause);
                response.completeExceptionally(cause);
                return null;
            });

        response.whenComplete((r, t) -> {
            if (response.isCancelled()) {
                chatSubscriber.cancelStream();
                httpFuture.cancel(true);
            }
        });

        return response;
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

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), ForecastResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A subscriber of raw SSE messages that delegates processing to a {@link TextGenerationSubscriber} and can be stopped through
     * {@link #cancelStream()}.
     */
    private static final class CancellableTextGenerationSubscriber implements Flow.Subscriber<String> {

        private final TextGenerationHandler handler;
        private final TextGenerationSubscriber chatSubscriber;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        private Flow.Subscription subscription;
        private volatile boolean success = true;

        CancellableTextGenerationSubscriber(TextGenerationHandler handler) {
            this.handler = handler;
            this.chatSubscriber = TextGenerationSubscriber.createSubscriber(handler);
        }

        /**
         * Stops the stream: no further callback is delivered to the handler and the body subscription is cancelled.
         */
        void cancelStream() {
            cancelled.set(true);

            var subscription = subscriptionRef.get();
            if (nonNull(subscription))
                subscription.cancel();
        }

        /**
         * Returns whether the stream has been cancelled.
         */
        boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscriptionRef.set(subscription);

            if (isCancelled()) {
                subscription.cancel();
                return;
            }

            this.subscription.request(1);
        }

        @Override
        public void onNext(String partialMessage) {

            if (isCancelled()) {
                subscription.cancel();
                return;
            }

            try {

                chatSubscriber.onNext(partialMessage);

            } catch (RuntimeException e) {

                onError(e);
                success = !handler.failOnFirstError();

            } finally {
                if (success && !isCancelled())
                    subscription.request(1);
                else
                    subscription.cancel();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (isCancelled())
                return;

            chatSubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            if (isCancelled())
                return;

            chatSubscriber.onComplete();
        }
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
