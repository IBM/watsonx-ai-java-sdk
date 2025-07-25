/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

/**
 * Represents a tool call generated by the model, such as a function call.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * ToolCall toolCall = ToolCall.of("call-123", "search", "{\"query\":\"openai\"}");
 * }</pre>
 *
 * @param id The unique identifier of the tool call
 * @param type the type of tool call, always {@code function}
 * @param function The function call details
 */
public record ToolCall(Integer index, String id, String type, FunctionCall function) {

    public static final String TYPE = "function";

    public ToolCall {
        type = TYPE;
    }

    /**
     * Creates a new {@link ToolCall} instance.
     *
     * @param id The unique identifier of the tool call.
     * @param name The name of the function to call.
     * @param arguments The arguments to call the function with, as generated by the model in JSON format.
     * @return A new {@link ToolCall} instance.
     */
    public static ToolCall of(String id, String name, String arguments) {
        return new ToolCall(null, id, TYPE, FunctionCall.of(name, arguments));
    }
}
