/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.requireNonNull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
 * @param toolCall The fully constructed {@link ToolCall}.
 */
public record CompletedToolCall(String completionId, ToolCall toolCall) {

    /**
     * Processes the tool call using the provided {@link ToolExecutor}.
     *
     * @param executor the executor responsible for running the tool call logic
     * @return a {@link ToolMessage} object generated from the tool call
     */
    public ToolMessage processTool(ToolExecutor executor) {
        return toolCall.processTool(executor);
    }

    /**
     * Processes the tool call using the provided {@link ToolExecutor} and executes the task asynchronously using the specified {@link Executor}.
     *
     * @param toolExecutor the {@link ToolExecutor} responsible for executing the tool call logic
     * @param executor the {@link Executor} that will handle the asynchronous execution of the tool call
     * @return a {@link CompletableFuture} that will complete with a {@link ToolMessage} when the tool call is processed
     */
    public CompletableFuture<ToolMessage> processTool(ToolExecutor toolExecutor, Executor executor) {
        requireNonNull(executor, "The executor must be provided");
        return CompletableFuture.supplyAsync(() -> toolCall.processTool(toolExecutor), executor);
    }
}
