/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static com.ibm.watsonx.ai.chat.ChatSubscriber.createSubscriber;
import static com.ibm.watsonx.ai.chat.ChatSubscriber.handleError;
import static com.ibm.watsonx.ai.chat.ChatSubscriber.toolHasParameters;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link ChatRestClient} interface.
 */
final class DefaultRestClient extends ChatRestClient {

    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticationProvider, "authenticationProvider is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticationProvider, LogMode.of(logRequests, logResponses));
        asyncHttpClient = HttpClientFactory.createAsync(authenticationProvider, LogMode.of(logRequests, logResponses));
    }

    @Override
    public ChatResponse chat(String transactionId, TextChatRequest textChatRequest) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "%s/text/chat?version=%s".formatted(ML_API_PATH, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(toJson(textChatRequest)))
            .timeout(Duration.ofMillis(textChatRequest.getTimeLimit()));

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
    public CompletableFuture<Void> chatStreaming(String transactionId, ExtractionTags extractionTags, TextChatRequest textChatRequest,
        ChatHandler handler) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "%s/text/chat_stream?version=%s".formatted(ML_API_PATH, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .POST(BodyPublishers.ofString(toJson(textChatRequest)))
            .timeout(Duration.ofMillis(textChatRequest.getTimeLimit()));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        var subscriber = subscriber(textChatRequest.getToolChoiceOption(), toolHasParameters(textChatRequest.getTools()), extractionTags, handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> handleError(t, handler));
    }

    /**
     * Creates a subscriber that listens to raw SSE messages from the chat stream, and delegates processing to a {@link ChatSubscriber}.
     */
    private Flow.Subscriber<String> subscriber(
        String toolChoiceOption,
        Map<String, Boolean> toolHasParameters,
        ExtractionTags extractionTags,
        ChatHandler handler) {

        return new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private volatile boolean success = true;
            private volatile ChatSubscriber chatSubscriber = createSubscriber(toolChoiceOption, toolHasParameters, extractionTags, handler);

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
    public static class Builder extends ChatRestClient.Builder {

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
