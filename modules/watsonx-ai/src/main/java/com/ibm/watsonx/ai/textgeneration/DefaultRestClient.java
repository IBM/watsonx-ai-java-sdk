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

        var subscriber = subscriber(handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> handleError(t, handler));
    }

    /**
     * Creates a subscriber that listens to raw SSE messages from the chat stream, and delegates processing to a {@link TextGenerationSubscriber}.
     *
     * @param handler the handler that receives processed chat events
     * @return a {@link Flow.Subscriber} suitable for consumption by the HTTP client
     */
    private Flow.Subscriber<String> subscriber(TextGenerationHandler handler) {

        return new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private volatile boolean success = true;
            private volatile TextGenerationSubscriber chatSubscriber = createSubscriber(handler);

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
