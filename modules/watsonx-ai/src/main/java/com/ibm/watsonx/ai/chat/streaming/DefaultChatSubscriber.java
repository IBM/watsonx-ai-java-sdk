/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static java.util.Objects.nonNull;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.SseEventProcessor;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.CompleteToolCallEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.ErrorEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialResponseEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialThinkingEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialToolCallEvent;
import com.ibm.watsonx.ai.chat.decorator.ChatHandlerDecorator;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Default implementation of {@link ChatSubscriber} for SDK standalone usage with CompletableFuture.
 * <p>
 * This subscriber orchestrates the streaming chat completion flow by:
 * <ul>
 * <li>Delegating SSE parsing to {@link SseEventProcessor}</li>
 * <li>Dispatching events to {@link ChatHandlerDecorator} for callback execution</li>
 * <li>Managing asynchronous completion using {@link CompletableFuture} and {@link ExecutorProvider}</li>
 * </ul>
 * <p>
 * <b>Threading Model:</b> The implementation uses {@link ExecutorProvider#ioExecutor()} for I/O-bound operations. User callbacks are executed via
 * {@link ExecutorProvider#callbackExecutor()}, which automatically switches to virtual threads on Java 21+ for improved scalability, or uses a cached
 * thread pool on earlier Java versions.
 */
public class DefaultChatSubscriber implements ChatSubscriber {
    private final SseEventProcessor processor;
    private final ChatHandlerDecorator handler;

    /**
     * Creates a new DefaultChatSubscriber.
     *
     * @param processor the stream processor that parses SSE chunks
     * @param handler the decorated handler that executes user callbacks
     */
    public DefaultChatSubscriber(SseEventProcessor processor, ChatHandlerDecorator handler) {
        this.processor = processor;
        this.handler = handler;
    }

    @Override
    public void onNext(String partialMessage) {
        var result = processor.processChunk(partialMessage);

        if (result.hasError()) {
            Throwable error = result.error();
            if (error instanceof RuntimeException re)
                throw re;
            throw new RuntimeException(error);
        }

        for (var event : result.events()) {
            if (event instanceof PartialResponseEvent e) {
                handler.onPartialResponse(e.content(), e.chunk());
            } else if (event instanceof PartialThinkingEvent e) {
                handler.onPartialThinking(e.content(), e.chunk());
            } else if (event instanceof PartialToolCallEvent e) {
                handler.onPartialToolCall(e.toolCall());
            } else if (event instanceof CompleteToolCallEvent e) {
                handler.onCompleteToolCall(e.toolCall());
            } else if (event instanceof ErrorEvent e) {
                handler.onError(e.error());
            }
        }
    }

    @Override
    public CompletableFuture<ChatResponse> onComplete() {
        return CompletableFuture
            .supplyAsync(() -> {
                var completedToolCall = processor.finalCompletedToolCall();
                if (nonNull(completedToolCall))
                    handler.onCompleteToolCall(completedToolCall);
                return null;
            }, ExecutorProvider.ioExecutor())
            .thenCompose(v -> handler.awaitCallbacks())
            .thenCompose(toolCalls -> {
                var response = processor.buildResponse(toolCalls);
                handler.onCompleteResponse(response);
                return handler.awaitCallbacks().thenApply(ignored -> response);
            });
    }

    @Override
    public void onError(Throwable throwable) {
        handler.onError(throwable);
    }
}