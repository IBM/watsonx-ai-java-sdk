/*
 * Copyright 2025 IBM Corporation
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
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Default implementation of {@link ChatSubscriber}.
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
                handler.onCompleteToolCall(e.completeToolCall());
            } else if (event instanceof ErrorEvent e) {
                handler.onError(e.error());
            }
        }
    }

    @Override
    public CompletableFuture<ChatResponse> onComplete() {
        return handler.awaitCallbacks()
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
                return handler.awaitCallbacks().thenApply(ignored -> response);
            });
    }

    @Override
    public CompletableFuture<Void> onError(Throwable throwable) {
        handler.onError(throwable);
        return handler.awaitCallbacks().thenApply(v -> null);
    }
}