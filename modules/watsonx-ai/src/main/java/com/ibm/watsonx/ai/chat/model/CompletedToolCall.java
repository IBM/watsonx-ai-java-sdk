/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ToolExecutor;

/**
 * Represents a fully assembled tool call emitted during chat completion streaming.
 * <p>
 * Unlike the final {@link ChatResponse}, which is delivered only once the entire assistant reply has finished streaming, a {@code CompletedToolCall}
 * is emitted as soon as a single tool call has been completely received. This means that multiple {@code CompletedToolCall} events may occur within
 * the same chat response, if the model decides to call more than one tool.
 *
 * @param completionId The identifier of the chat completion request this tool call belongs to.
 * @param index Choice index.
 * @param toolCall The fully constructed {@link ToolCall}.
 */
public record CompletedToolCall(String completionId, Integer index, ToolCall toolCall) {

    /**
     * Processes the tool call using the provided {@link ToolExecutor}.
     *
     * @param executor the executor responsible for running the tool call logic
     * @return a {@link ToolMessage} object generated from the tool call
     */
    public ToolMessage processTool(ToolExecutor executor) {
        return toolCall.processTool(executor);
    }
}
