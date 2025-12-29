/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

/**
 * Represents a partial tool call emitted during chat completion streaming.
 * <p>
 * This object may be produced multiple times for a single tool call, typically containing incremental fragments (tokens) of the tool's arguments as
 * they are streamed by the model. Once all fragments have been received and assembled, a {@link CompletedToolCall} will be delivered instead.
 *
 * @param completionId The identifier of the chat completion request this tool call belongs to
 * @param index The index of this choice within the choices array
 * @param toolIndex The index of this tool call
 * @param id The identifier of the tool call
 * @param name The function name being invoked
 * @param arguments A partial fragment of the function arguments
 */
public record PartialToolCall(String completionId, int index, int toolIndex, String id, String name, String arguments) {}
