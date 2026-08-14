/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.textgeneration.TextGenerationSubscriber.createSubscriber;
import static com.ibm.watsonx.ai.textgeneration.TextGenerationSubscriber.handleError;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link TextGenerationRestClient} abstract class.
 */
final class DefaultRestClient extends TextGenerationRestClient {

    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
        asyncHttpClient = HttpClientFactory.createAsync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public TextGenerationResponse generate(String transactionId, TextRequest textRequest) {

        var timeout = textRequest.parameters().timeLimit();

        var httpRequest =
            HttpRequest
                .newBuilder(URI.create(baseUrl + "/ml/v1/text/generation?version=%s".formatted(version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(timeout))
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
    public CompletableFuture<Void> generateStreaming(String transactionId, TextRequest textRequest, TextGenerationHandler handler) {

        var timeout = textRequest.parameters().timeLimit();

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/text/generation_stream?version=%s".formatted(version)))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .timeout(Duration.ofMillis(timeout))
            .POST(BodyPublishers.ofString(toJson(textRequest)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        var response = new CompletableFuture<Void>();
        var subscriber = new CancellableSubscriber(handler);
        var httpFuture = asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber));

        httpFuture
            .thenAccept(r -> response.complete(null))
            .exceptionally(t -> {
                if (subscriber.isCancelled())
                    return null;

                handleError(t, handler);
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

    /**
     * A subscriber of raw SSE messages that delegates processing to a {@link TextGenerationSubscriber} and can be stopped through
     * {@link #cancelStream()}.
     */
    private static final class CancellableSubscriber implements Flow.Subscriber<String> {

        private final TextGenerationHandler handler;
        private final TextGenerationSubscriber chatSubscriber;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        private Flow.Subscription subscription;
        private volatile boolean success = true;

        CancellableSubscriber(TextGenerationHandler handler) {
            this.handler = handler;
            this.chatSubscriber = createSubscriber(handler);
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
    public final static class Builder extends TextGenerationRestClient.Builder {

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
