/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.decorator;

import static java.util.Objects.nonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * A decorator implementation of {@link ChatHandler} that provides asynchronous callback management and tool call interception capabilities for chat
 * streaming operations.
 *
 * <p>
 * This class wraps a delegate {@link ChatHandler} and ensures that all callbacks are executed in a controlled manner:
 * <ul>
 * <li>Sequential execution of standard callbacks (partial responses, complete responses, errors, thinking)</li>
 * <li>Parallel execution of tool call callbacks with optional interception</li>
 * <li>Thread-safe callback scheduling using {@link CompletableFuture}</li>
 * </ul>
 *
 * <b>Thread Safety</b>
 * <p>
 * This class is thread-safe and designed for concurrent use in streaming scenarios. All callback scheduling is handled through atomic operations and
 * thread-safe collections.
 */
public class ChatHandlerDecorator implements ChatHandler {
    /**
     * The underlying chat handler that receives the decorated callbacks.
     */
    private final ChatHandler delegate;

    /**
     * Context information passed to the tool interceptor for processing tool calls.
     */
    private final InterceptorContext context;

    /**
     * Optional interceptor for modifying or validating tool calls before they reach the delegate.
     */
    private final ToolInterceptor toolInterceptor;

    /**
     * Atomic reference to maintain a chain of sequential callbacks.
     */
    private final AtomicReference<CompletableFuture<Void>> callbackChain =
        new AtomicReference<>(CompletableFuture.completedFuture(null));

    /**
     * Thread-safe list of pending tool call futures that can execute in parallel. These are tracked separately to allow concurrent tool call
     * processing.
     */
    private final List<CompletableFuture<ToolCall>> pendingToolCallCallbacks =
        Collections.synchronizedList(new ArrayList<>());

    /**
     * Constructs a new {@code ChatHandlerDecorator}.
     *
     * @param delegate the underlying chat handler to receive decorated callbacks
     * @param context the interceptor context for tool call processing
     * @param toolInterceptor optional interceptor for tool calls
     */
    public ChatHandlerDecorator(ChatHandler delegate, InterceptorContext context, ToolInterceptor toolInterceptor) {
        this.delegate = delegate;
        this.context = context;
        this.toolInterceptor = toolInterceptor;
    }

    @Override
    public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
        scheduleCallback(() -> delegate.onPartialResponse(partialResponse, partialChatResponse));
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        scheduleCallback(() -> delegate.onCompleteResponse(completeResponse));
    }

    @Override
    public void onError(Throwable error) {
        scheduleCallback(() -> delegate.onError(error));
    }

    @Override
    public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
        scheduleCallback(() -> delegate.onPartialThinking(partialThinking, partialChatResponse));
    }

    @Override
    public void onPartialToolCall(PartialToolCall partialToolCall) {
        scheduleCallback(() -> delegate.onPartialToolCall(partialToolCall));
    }

    @Override
    public void onCompleteToolCall(CompletedToolCall completeToolCall) {
        var future = CompletableFuture
            .supplyAsync(() -> nonNull(toolInterceptor)
                ? toolInterceptor.intercept(context, completeToolCall)
                : completeToolCall, ExecutorProvider.callbackExecutor())
            .thenApplyAsync(toolCallNormalized -> {
                try {
                    delegate.onCompleteToolCall(toolCallNormalized);
                    return toolCallNormalized.toolCall();
                } catch (RuntimeException e) {
                    delegate.onError(e);
                    throw e;
                }
            }, ExecutorProvider.callbackExecutor());

        pendingToolCallCallbacks.add(future);
    }

    @Override
    public boolean failOnFirstError() {
        return delegate.failOnFirstError();
    }

    /**
     * Waits for all pending callbacks to complete.
     * <p>
     * This method returns a {@link CompletableFuture} that completes when:
     * <ol>
     * <li>All parallel tool call callbacks have finished</li>
     * <li>The sequential callback chain has completed</li>
     * </ol>
     *
     * <p>
     * The returned future resolves to a list of all processed {@link ToolCall} objects, in the order they were completed (not necessarily the order
     * they were received).
     *
     * @return a CompletableFuture that resolves to a list of all processed tool calls
     */
    public CompletableFuture<List<ToolCall>> awaitCallbacks() {
        return CompletableFuture.allOf(pendingToolCallCallbacks.toArray(new CompletableFuture[0]))
            .thenCompose(v -> callbackChain.get())
            .thenApply(v -> pendingToolCallCallbacks.stream().map(CompletableFuture::join).toList());
    }

    /**
     * Schedules a callback to run sequentially after all previous callbacks complete. This method ensures that callbacks are executed in the order
     * they are scheduled, maintaining the sequential nature of the callback chain.
     */
    private void scheduleCallback(Runnable callback) {
        callbackChain.updateAndGet(chain -> chain.thenRunAsync(() -> {
            try {
                callback.run();
            } catch (Exception e) {
                delegate.onError(e);
            }
        }, ExecutorProvider.callbackExecutor()));
    }
}
