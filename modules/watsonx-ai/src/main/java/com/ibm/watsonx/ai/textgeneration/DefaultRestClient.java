/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.textgeneration.TextGenerationSubscriber.createSubscriber;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link TextGenerationRestClient} abstract class.
 */
final class DefaultRestClient extends TextGenerationRestClient {
    private static final Logger logger = LoggerFactory.getLogger(DefaultRestClient.class);

    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses), requestLogger,
            requestLogLevel, responseLogger, responseLogLevel);
        asyncHttpClient = HttpClientFactory.createAsync(authenticator, httpClient, LogMode.of(logRequests, logResponses), requestLogger,
            requestLogLevel, responseLogger, responseLogLevel);
    }

    @Override
    public TextGenerationResponse generate(String transactionId, TextRequest textRequest) {

        var httpRequest =
            HttpRequest
                .newBuilder(URI.create(baseUrl + "/ml/v1/text/generation?version=%s".formatted(version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(timeout)
                .POST(BodyPublishers.ofString(toJson(textRequest)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), TextGenerationResponse.class);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> generateStreaming(String transactionId, TextRequest textRequest, TextGenerationHandler handler) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/text/generation_stream?version=%s".formatted(version)))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .timeout(timeout)
            .POST(BodyPublishers.ofString(toJson(textRequest)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        var response = new CompletableFuture<Void>();
        var subscriber = new CancellableSubscriber(handler, response);
        var httpFuture = asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(
                new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers(), responseLogger, responseLogLevel))
            : BodySubscribers.fromLineSubscriber(subscriber));

        httpFuture
            .thenCompose(r -> subscriber.processing)
            .thenRun(() -> response.complete(null))
            .exceptionally(t -> {
                if (subscriber.isCancelled())
                    return null;

                Throwable cause = nonNull(t.getCause()) ? t.getCause() : t;
                subscriber.onError(cause);
                return null;
            });

        response.whenComplete((r, t) -> {
            if (response.isCancelled() || t != null) {
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
        private final CompletableFuture<Void> response;
        private final CompletableFuture<Void> processing = new CompletableFuture<>();
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean errorReported = new AtomicBoolean(false);
        private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        private Flow.Subscription subscription;
        private volatile boolean success = true;

        CancellableSubscriber(TextGenerationHandler handler, CompletableFuture<Void> response) {
            this.handler = handler;
            this.response = response;
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

            if (isCancelled() || !success) {
                subscription.cancel();
                return;
            }

            try {

                chatSubscriber.onNext(partialMessage);

            } catch (RuntimeException e) {

                success = !handler.failOnFirstError();
                if (success)
                    notifyError(e);
                else
                    onError(e);

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

            success = false;
            if (errorReported.compareAndSet(false, true)) {
                try {
                    notifyError(throwable);
                } finally {
                    response.completeExceptionally(throwable);
                    processing.completeExceptionally(throwable);
                }
            }
        }

        private void notifyError(Throwable throwable) {
            try {
                chatSubscriber.onError(throwable);
            } catch (RuntimeException | Error callbackFailure) {
                if (callbackFailure != throwable)
                    throwable.addSuppressed(callbackFailure);
                logger.warn("Text generation error callback failed", callbackFailure);
            }
        }

        @Override
        public void onComplete() {
            if (isCancelled() || !success)
                return;

            try {
                chatSubscriber.onComplete();
                processing.complete(null);
            } catch (RuntimeException | Error e) {
                onError(e);
            }
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
