/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.decorator;

import static java.util.Objects.nonNull;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * A {@link ChatHandler} decorator that intercepts and transforms {@link CompletedToolCall} instances using a {@link ToolInterceptor}.
 */
public class ChatHandlerDecorator implements ChatHandler {
    private final InterceptorContext context;
    private final ChatHandler delegate;
    private final ToolInterceptor toolInterceptor;

    /**
     * Constructs a new {@link ChatHandlerDecorator}.
     * <p>
     * This decorator wraps a {@link ChatHandler} delegate and applies a {@link ToolInterceptor} to {@link CompletedToolCall} instances before
     * forwarding them to the {@link #onCompleteToolCall(CompletedToolCall)}.
     *
     * @param context the {@link InterceptorContext} providing contextual information for the interceptor
     * @param delegate the underlying {@link ChatHandler} to which all callbacks will be forwarded
     * @param toolInterceptor the {@link ToolInterceptor} used to transform {@link CompletedToolCall} instances
     */
    public ChatHandlerDecorator(InterceptorContext context, ChatHandler delegate, ToolInterceptor toolInterceptor) {
        this.context = context;
        this.delegate = delegate;
        this.toolInterceptor = toolInterceptor;
    }

    /**
     * Intercepts and transforms the given {@link CompletedToolCall}.
     *
     * @param completeToolCall the {@link CompletedToolCall} to intercept
     * @return the normalized {@link CompletedToolCall}
     */
    public CompletableFuture<CompletedToolCall> normalize(CompletedToolCall completeToolCall) {
        return (nonNull(toolInterceptor))
            ? CompletableFuture.supplyAsync(() -> toolInterceptor.intercept(context, completeToolCall), ExecutorProvider.interceptorExecutor())
            : CompletableFuture.completedFuture(completeToolCall);
    }

    @Override
    public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
        delegate.onPartialResponse(partialResponse, partialChatResponse);
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        delegate.onCompleteResponse(completeResponse);
    }

    @Override
    public void onError(Throwable error) {
        delegate.onError(error);
    }

    @Override
    public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
        delegate.onPartialThinking(partialThinking, partialChatResponse);
    }

    @Override
    public void onPartialToolCall(PartialToolCall partialToolCall) {
        delegate.onPartialToolCall(partialToolCall);
    }

    @Override
    public void onCompleteToolCall(CompletedToolCall completeToolCall) {
        delegate.onCompleteToolCall(completeToolCall);
    }

    @Override
    public boolean failOnFirstError() {
        return delegate.failOnFirstError();
    }
}