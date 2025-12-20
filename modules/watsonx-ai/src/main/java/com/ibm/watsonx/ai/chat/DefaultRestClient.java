/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.chat.decorator.ChatHandlerDecorator;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.streaming.ChatSubscriber;
import com.ibm.watsonx.ai.chat.streaming.DefaultChatSubscriber;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link ChatRestClient} abstract class.
 */
final class DefaultRestClient extends ChatRestClient {

    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
        asyncHttpClient = HttpClientFactory.createAsync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public ChatResponse chat(String transactionId, TextChatRequest textChatRequest) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/text/chat?version=%s".formatted(version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(toJson(textChatRequest)))
            .timeout(Duration.ofMillis(textChatRequest.timeLimit()));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), ChatResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(
        String transactionId,
        TextChatRequest textChatRequest,
        ChatClientContext context,
        ChatHandler handler) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/text/chat_stream?version=%s".formatted(version)))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .POST(BodyPublishers.ofString(toJson(textChatRequest)))
            .timeout(Duration.ofMillis(textChatRequest.timeLimit()));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        var response = new CompletableFuture<ChatResponse>();
        var interceptorContext = new InterceptorContext(context.chatProvider(), context.chatRequest(), null);
        var chatSubscriber =
            new DefaultChatSubscriber(
                new SseEventProcessor(ChatSubscriber.toolHasParameters(textChatRequest.tools()), context.extractionTags()),
                new ChatHandlerDecorator(handler, interceptorContext, context.toolInterceptor())
            );

        var subscriber = subscriber(chatSubscriber, response, !handler.failOnFirstError());
        asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> {
                response.completeExceptionally(ChatSubscriber.handleError(t, handler));
                return null;
            });
        return response;
    }

    /**
     * Creates a subscriber that listens to raw SSE messages from the chat stream, and delegates processing to a {@link ChatSubscriber}.
     */
    private Flow.Subscriber<String> subscriber(
        ChatSubscriber chatSubscriber,
        CompletableFuture<ChatResponse> response,
        boolean failOnFirstError) {

        return new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;
            private volatile boolean continueProcessing = true;

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

                    Throwable t = nonNull(e.getCause()) ? e.getCause() : e;
                    onError(t);
                    continueProcessing = failOnFirstError;
                    if (!continueProcessing)
                        response.completeExceptionally(t);

                } finally {
                    if (continueProcessing)
                        subscription.request(1);
                    else {
                        subscription.cancel();
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                chatSubscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                chatSubscriber.onComplete()
                    .whenComplete((chatResponse, error) -> {
                        if (nonNull(error)) {
                            error = nonNull(error.getCause()) ? error.getCause() : error;
                            chatSubscriber.onError(error);
                            response.completeExceptionally(error);
                        } else
                            response.complete(chatResponse);
                    });
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
    public final static class Builder extends ChatRestClient.Builder {

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
