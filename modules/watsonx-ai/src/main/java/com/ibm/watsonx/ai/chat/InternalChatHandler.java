/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;

/**
 * Internal extension of ChatHandler that allows interceptors to transform {@code CompletedToolCall} during streaming, and return the normalized
 * version back to the subscriber.
 */
public class InternalChatHandler implements ChatHandler {
    private final InterceptorContext context;
    private final ChatHandler delegate;
    private final ToolInterceptor toolInterceptor;

    public InternalChatHandler(InterceptorContext context, ChatHandler delegate, ToolInterceptor toolInterceptor) {
        this.context = context;
        this.delegate = delegate;
        this.toolInterceptor = toolInterceptor;
    }

    /**
     * Applies normalization and transformation logic to the given {@link CompletedToolCall} and returns the modified instance that should replace the
     * original one in the streaming pipeline.
     *
     * @param call the fully assembled {@link CompletedToolCall} produced from streamed fragments
     * @return the normalized or transformed {@link CompletedToolCall}
     */
    public CompletedToolCall normalizeToolCall(CompletedToolCall completeToolCall) {

        if (nonNull(toolInterceptor))
            completeToolCall = toolInterceptor.intercept(context, completeToolCall);

        delegate.onCompleteToolCall(completeToolCall);
        return completeToolCall;
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
    public void onCompleteToolCall(CompletedToolCall completeToolCall) {}

    @Override
    public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
        delegate.onPartialThinking(partialThinking, partialChatResponse);
    }

    @Override
    public void onPartialToolCall(PartialToolCall partialToolCall) {
        delegate.onPartialToolCall(partialToolCall);
    }
}