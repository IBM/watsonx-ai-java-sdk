/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.SseEventProcessor;
import com.ibm.watsonx.ai.chat.decorator.ChatHandlerDecorator;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Default implementation of {@link ChatSubscriber}.
 * <p>
 * <b>Threading Model:</b> The implementation uses {@link ExecutorProvider#ioExecutor()} for I/O-bound operations. User callbacks are executed via
 * {@link ExecutorProvider#callbackExecutor()}, which automatically switches to virtual threads on Java 21+ for improved scalability, or uses a cached
 * thread pool on earlier Java versions.
 */
public class DefaultChatSubscriber extends ChatSubscriber {

    /**
     * Creates a new DefaultChatSubscriber.
     *
     * @param processor the stream processor that parses SSE chunks
     * @param handler the decorated handler that executes user callbacks
     */
    public DefaultChatSubscriber(SseEventProcessor processor, ChatHandlerDecorator handler) {
        super(processor, handler);
    }

    @Override
    public CompletableFuture<ChatResponse> onComplete() {
        return awaitCallbacks()
            .thenCompose(completeToolCalls -> {
                var response = processor.buildResponse();
                if (nonNull(completeToolCalls) && !completeToolCalls.isEmpty()) {
                    var choices = response.choices().stream()
                        .map(choice -> {
                            var tools = completeToolCalls.stream()
                                .filter(ctc -> ctc.index().equals(choice.index()))
                                .map(CompletedToolCall::toolCall)
                                .toList();
                            return choice.withResultMessage(choice.message().withToolCalls(tools));
                        }).toList();
                    handler.onCompleteResponse(response.toBuilder().choices(choices).build());
                } else {
                    handler.onCompleteResponse(response);
                }
                return awaitCallbacks().thenApply(ignored -> response);
            });
    }

    @Override
    public CompletableFuture<Void> onError(Throwable throwable) {
        handler.onError(throwable);
        return awaitCallbacks().thenApply(v -> null);
    }

    /**
     * Creates a subscriber that listens to raw SSE messages from the chat stream.
     */
    public Flow.Subscriber<String> asFlowSubscriber(
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

                    DefaultChatSubscriber.this.onNext(partialMessage);

                } catch (RuntimeException e) {

                    Throwable t = nonNull(e.getCause()) ? e.getCause() : e;
                    continueProcessing = failOnFirstError;
                    DefaultChatSubscriber.this.onError(t).whenComplete((v, err) -> {
                        if (!continueProcessing)
                            response.completeExceptionally(t);
                    });

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
                DefaultChatSubscriber.this.onError(throwable);
            }

            @Override
            public void onComplete() {
                DefaultChatSubscriber.this.onComplete()
                    .whenComplete((chatResponse, error) -> {
                        if (nonNull(error)) {
                            error = nonNull(error.getCause()) ? error.getCause() : error;
                            DefaultChatSubscriber.this.onError(error);
                            response.completeExceptionally(error);
                        } else
                            response.complete(chatResponse);
                    });
            }
        };
    }

    /**
     * Waits for all pending handler callbacks to complete.
     * <p>
     * This method delegates to {@link ChatHandlerDecorator#awaitCallbacks()} to ensure that:
     * <ul>
     * <li>All parallel tool call callbacks have finished executing</li>
     * <li>The sequential callback chain has completed</li>
     * </ul>
     *
     * @return a CompletableFuture that resolves to a list of all processed {@link CompletedToolCall} objects
     */
    private CompletableFuture<List<CompletedToolCall>> awaitCallbacks() {
        // This cast is safe because the constructor enforces ChatHandlerDecorator type
        if (!(handler instanceof ChatHandlerDecorator handlerDecorator))
            throw new IllegalStateException("Handler must be a ChatHandlerDecorator");

        return handlerDecorator.awaitCallbacks();
    }
}