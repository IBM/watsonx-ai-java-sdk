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
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.FunctionCall;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.ToolCall;

/**
 * Functional interface for intercepting and modifying {@link FunctionCall} instances contained within tool calls of an assistant message.
 * <p>
 * This interceptor is invoked for every {@link FunctionCall} produced by the model.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ToolInterceptor interceptor =
 *     (request, fc) -> {
 *         var args = fc.arguments();
 *         return nonNull(args) && args.startsWith("\"")
 *             ? fc.withArguments(Json.fromJson(args, String.class))
 *             : fc;
 *     };
 * }</pre>
 */
@FunctionalInterface
public interface ToolInterceptor {

    /**
     * Intercepts and modifies a {@link FunctionCall} extracted from a tool call.
     *
     * @param request the original chat request
     * @param fc the function call to intercept
     * @return the modified {@link FunctionCall}
     */
    FunctionCall intercept(ChatRequest request, FunctionCall fc);

    /**
     * Applies this interceptor to all tool calls in the given {@link ChatResponse}.
     * <p>
     * The interceptor is applied to each {@link FunctionCall} found in the tool calls of every {@link ResultChoice}. The method reconstructs the list
     * of {@link ToolCall} instances with the transformed function calls while preserving the original indices and identifiers.
     *
     * @param request the original chat request
     * @param response the model-generated response
     * @return a list of {@link ResultChoice} containing rebuilt tool calls
     */
    default List<ResultChoice> intercept(ChatRequest request, ChatResponse response) {
        return response.choices().stream().map(choice -> {

            var message = choice.message();

            if (isNull(message.toolCalls()))
                return choice;

            var toolCalls = message.toolCalls();
            var functionCalls = toolCalls.stream().map(ToolCall::function).toList();
            var normalized = functionCalls.stream().map(fc -> intercept(request, fc)).toList();
            var rebuiltToolCalls = toolCalls.stream().flatMap(toolCall -> normalized.stream().map(fc -> toolCall.withFunctionCall(fc))).toList();

            var resultMessage = new ResultMessage(
                message.role(),
                message.content(),
                message.reasoningContent(),
                message.refusal(),
                rebuiltToolCalls
            );

            return choice.withResultMessage(resultMessage);
        }).toList();
    }

    /**
     * Applies the interceptor to a full {@link CompletedToolCall}, returning a new instance with the modified {@link FunctionCall}.
     *
     * @param completedToolCall the completed tool call to intercept
     * @return a new {@link CompletedToolCall} containing the modified {@link FunctionCall}
     */
    default CompletedToolCall intercept(CompletedToolCall completedToolCall) {
        var normalized = intercept(null, completedToolCall.toolCall().function());
        return new CompletedToolCall(
            completedToolCall.completionId(),
            completedToolCall.toolCall().withFunctionCall(normalized)
        );
    }
}
