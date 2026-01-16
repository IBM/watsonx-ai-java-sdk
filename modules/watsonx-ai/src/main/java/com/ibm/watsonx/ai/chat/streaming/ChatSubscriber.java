/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.SseEventProcessor;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.CompleteToolCallEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.ErrorEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialResponseEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialThinkingEvent;
import com.ibm.watsonx.ai.chat.SseEventProcessor.CallbackEvent.PartialToolCallEvent;

/**
 * Abstract base class for consuming and processing Server-Sent Events (SSE) in chat streaming operations.
 * <p>
 * This class providing a common implementation for processing SSE chunks and dispatching events to the handler, while allowing subclasses to
 * customize error handling and completion behavior.
 */
public abstract class ChatSubscriber {
    /**
     * The SSE event processor that parses and transforms raw SSE chunks into domain events.
     */
    protected final SseEventProcessor processor;

    /**
     * The handler that receives callbacks for chat events.
     */
    protected final ChatHandler handler;

    /**
     * Constructs a new ChatSubscriber with the specified processor and handler.
     *
     * @param processor the SSE event processor for parsing chunks
     * @param handler the handler for receiving chat events
     */
    protected ChatSubscriber(SseEventProcessor processor, ChatHandler handler) {
        this.processor = processor;
        this.handler = handler;
    }

    /**
     * Processes a new SSE message chunk and dispatches resulting events to the handler.
     *
     * @param partialMessage the raw SSE event payload
     */
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

    /**
     * Handles errors that occur during the streaming session.
     *
     * @param throwable the error that occurred
     * @return a CompletableFuture that completes when error handling is finished
     */
    public abstract CompletableFuture<Void> onError(Throwable throwable);

    /**
     * Handles the completion of the streaming session.
     *
     * @return a CompletableFuture that completes with the final {@link ChatResponse}
     */
    public abstract CompletableFuture<ChatResponse> onComplete();
}
