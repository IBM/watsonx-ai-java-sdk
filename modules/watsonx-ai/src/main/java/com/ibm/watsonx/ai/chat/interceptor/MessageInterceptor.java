/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.interceptor;

import static java.util.Objects.isNull;
import java.util.List;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.model.ResultMessage;

/**
 * Functional interface for intercepting and modifying the textual content of assistant messages in a {@link ChatResponse}.
 * <p>
 * This interceptor is invoked with the original {@link ChatRequest} and the message content. Implementations can transform, sanitize, normalize or
 * rewrite the content before it is returned or consumed by application logic.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * MessageInterceptor interceptor =
 *     (request, content) -> isNull(content)
 *         ? "Hello!"
 *         : content.strip();
 * }</pre>
 */
@FunctionalInterface
public interface MessageInterceptor {

    /**
     * Intercepts and modifies the textual content of an assistant message.
     *
     * @param request the original chat request
     * @param message the message content to intercept
     * @return the modified content
     */
    String intercept(ChatRequest request, String message);

    default List<ResultChoice> intercept(ChatRequest request, ChatResponse response) {
        return response.choices().stream().map(choice -> {

            ResultMessage message = choice.message();

            if (isNull(message.content()))
                return choice;

            var normalized = intercept(request, message.content());

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
