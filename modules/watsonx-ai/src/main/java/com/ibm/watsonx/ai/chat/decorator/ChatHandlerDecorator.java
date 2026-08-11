/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.decorator;

import static java.util.Objects.isNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import com.ibm.watsonx.ai.chat.BaseChatRequest;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * A decorator implementation of {@link ChatHandler} that provides asynchronous callback management and tool call interception capabilities for chat
 * streaming operations.
 *
 * <p>
 * This class wraps a delegate {@link ChatHandler} and ensures that all callbacks are executed in a controlled manner:
 * <ul>
 * <li>Sequential delivery of every callback (partial responses, partial thinking, partial and complete tool calls, complete responses, errors), one
 * at a time and in the order they were emitted</li>
 * <li>Optional interception of complete tool calls, applied before they are delivered</li>
 * <li>Callback scheduling using {@link CompletableFuture}, so the emitting thread is never blocked by user code</li>
 * </ul>
 *
 * <b>Thread Safety</b>
 * <p>
 * This class is thread-safe and designed for concurrent use in streaming scenarios. Consecutive callbacks may run on different threads of the
 * callback executor, but never at the same time: each one is guaranteed to have returned before the next one starts, and to be visible to it.
 * <p>
 * Scheduling is lock-free and holds no monitor while running user code, so a callback that blocks never pins the virtual thread it runs on to its
 * carrier thread.
 *
 * @param <R> the concrete chat request type handled by the intercepted provider
 */
public class ChatHandlerDecorator<R extends BaseChatRequest> implements ChatHandler {
    /**
     * The underlying chat handler that receives the decorated callbacks.
     */
    private final ChatHandler delegate;

    /**
     * Context information passed to the tool interceptor for processing tool calls.
     */
    private final InterceptorContext<R> context;

    /**
     * Optional interceptor for modifying or validating tool calls before they reach the delegate.
     */
    private final ToolInterceptor<R> toolInterceptor;

    /**
     * The tool calls already handed to the delegate, in delivery order.
     */
    private final List<CompletedToolCall> deliveredToolCalls = new CopyOnWriteArrayList<>();

    /**
     * Tail of the chain of sequential callbacks: the future that completes once the last callback scheduled so far has been delivered.
     */
    private final AtomicReference<CompletableFuture<Void>> callbackChain =
        new AtomicReference<>(CompletableFuture.completedFuture(null));

    /**
     * Constructs a new {@code ChatHandlerDecorator}.
     *
     * @param delegate the underlying chat handler to receive decorated callbacks
     * @param context the interceptor context for tool call processing
     * @param toolInterceptor optional interceptor for tool calls
     */
    public ChatHandlerDecorator(ChatHandler delegate, InterceptorContext<R> context, ToolInterceptor<R> toolInterceptor) {
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
        scheduleCallback(() -> {
            var toolCallNormalized = normalize(completeToolCall);
            deliveredToolCalls.add(toolCallNormalized);
            delegate.onCompleteToolCall(toolCallNormalized);
        });
    }

    @Override
    public boolean failOnFirstError() {
        return delegate.failOnFirstError();
    }

    /**
     * Waits for every callback scheduled so far to be delivered.
     * <p>
     * The returned future resolves to a list of all processed {@link CompletedToolCall} objects, in the order they were received.
     *
     * @return a CompletableFuture that resolves to a list of all processed tool calls
     */
    public CompletableFuture<List<CompletedToolCall>> awaitCallbacks() {
        return callbackChain.get().thenApply(v -> List.copyOf(deliveredToolCalls));
    }

    /**
     * Applies {@link #toolInterceptor} to the given tool call, falling back to the un-normalized tool call when the interceptor fails.
     */
    private CompletedToolCall normalize(CompletedToolCall completeToolCall) {
        if (isNull(toolInterceptor))
            return completeToolCall;

        try {
            return toolInterceptor.intercept(context, completeToolCall);
        } catch (RuntimeException | Error e) {
            safeOnError(e);
            return completeToolCall;
        }
    }

    /**
     * Schedules a callback to run sequentially after all previous callbacks complete. This method ensures that callbacks are executed in the order
     * they are scheduled, maintaining the sequential nature of the callback chain.
     * <p>
     * The tail of the chain is swapped with {@link AtomicReference#getAndSet(Object)}, so each scheduled callback is handed exactly one predecessor
     * and nothing is executed while a lock is held.
     */
    private void scheduleCallback(Runnable callback) {
        var delivered = new CompletableFuture<Void>();
        var previous = callbackChain.getAndSet(delivered);

        previous.thenRunAsync(() -> {
            try {
                callback.run();
            } catch (RuntimeException | Error e) {
                safeOnError(e);
            } finally {
                delivered.complete(null);
            }
        }, ExecutorProvider.callbackExecutor());
    }

    /**
     * Reports an error to the delegate, ignoring any failure of the error callback itself.
     */
    private void safeOnError(Throwable error) {
        try {
            delegate.onError(error);
        } catch (RuntimeException | Error ignored) {
            // Nothing more we can do.
        }
    }
}
