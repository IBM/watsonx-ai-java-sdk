/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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