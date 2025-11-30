/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.core.Json;

/**
 * Functional interface representing an executor capable of handling and resolving model-generated tool calls.
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * Executes a tool call produced by the model.
     *
     * @param toolName the name of the tool invoked by the model
     * @param toolArgs the arguments extracted from the tool call JSON
     * @return the result of tool execution
     */
    Object execute(String toolName, ToolArguments toolArgs);

    /**
     * Normalizes the raw argument string received from the model before parsing into {@link ToolArguments}.
     *
     * @param rawArguments the unprocessed argument string produced by the model
     * @return the normalized argument string to be parsed into {@link ToolArguments}
     */
    default String normalize(String rawArguments) {
        try {
            return Json.fromJson(rawArguments, String.class);
        } catch (RuntimeException e) {
            return rawArguments;
        }
    }
}
