/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import com.ibm.watsonx.ai.chat.ChatResponse;

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
public record CompletedToolCall(String completionId, ToolCall toolCall) {}
