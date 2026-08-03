/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.interceptor;

import static java.util.Objects.isNull;
import java.util.List;
import com.ibm.watsonx.ai.chat.BaseChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.model.ResultMessage;

/**
 * Functional interface for intercepting and modifying the textual content of assistant messages.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * MessageInterceptor<ChatRequest> interceptor =
 *     (ctx, message) -> message == null
 *         ? "Hello!"
 *         : message.strip();
 * }</pre>
 *
 * @param <R> the concrete chat request type handled by the intercepted provider
 */
@FunctionalInterface
public interface MessageInterceptor<R extends BaseChatRequest> {

    /**
     * Intercepts and modifies the textual content of an assistant message.
     *
     * @param ctx the interceptor context, providing access to request, response, and other contextual information
     * @param message the message content to intercept
     * @return the modified content
     */
    String intercept(InterceptorContext<R> ctx, String message);

    /**
     * Applies this interceptor to all messages in the current {@link ChatResponse}.
     *
     * @param ctx the interceptor context containing the current request and response
     * @return a list of {@link ResultChoice} containing rebuilt messages with applied transformations
     */
    default List<ResultChoice> intercept(InterceptorContext<R> ctx) {
        var response = ctx.response().orElseThrow();
        return response.choices().stream().map(choice -> {

            ResultMessage message = choice.message();

            if (isNull(message.content()))
                return choice;

            var normalized = intercept(ctx, message.content());

            var resultMessage = new ResultMessage(
                message.role(),
                normalized,
                message.reasoningContent(),
                message.refusal(),
                message.toolCalls()
            );

            return choice.withResultMessage(resultMessage);
        }).toList();
    }
}
