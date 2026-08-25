/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.interceptor;

import com.ibm.watsonx.ai.chat.BaseChatRequest;
import com.ibm.watsonx.ai.chat.ChatHandler;

/**
 * Functional interface for intercepting and modifying each partial response emitted during a streaming session.
 * <p>
 * Every invocation receives a single content token exactly as the model streamed it, and the returned value is what
 * {@link ChatHandler#onPartialResponse} delivers. Tokens are never buffered, so a transformation that has to match across token boundaries cannot be
 * expressed here. Use {@link MessageInterceptor}, which is always applied to the complete message.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * PartialResponseInterceptor<ChatRequest> interceptor =
 *     (ctx, partialResponse) -> partialResponse.replace("foo", "bar");
 * }</pre>
 *
 * @param <R> the concrete chat request type handled by the intercepted provider
 */
@FunctionalInterface
public interface PartialResponseInterceptor<R extends BaseChatRequest> {

    /**
     * Intercepts and modifies a single partial response.
     *
     * @param ctx the interceptor context, providing access to the request and other contextual information
     * @param partialResponse the content token to intercept
     * @return the modified token
     */
    String intercept(InterceptorContext<R> ctx, String partialResponse);
}
