/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;

/**
 * Interface representing a tool that can be registered with a {@link ToolRegistry} and executed by the model.
 * <p>
 * Implementations of this interface define tools that can be invoked during chat interactions.
 * <p>
 * Each {@code ExecutableTool} must provide:
 * <ul>
 * <li>A unique name that identifies the tool</li>
 * <li>A schema describing the tool's purpose and parameters</li>
 * <li>An execution method that processes tool arguments and returns results</li>
 * </ul>
 *
 * @see ToolRegistry
 * @see ToolExecutor
 */
public interface ExecutableTool {

    /**
     * Returns the unique name of this tool.
     *
     * @return the tool name
     */
    String name();

    /**
     * Returns the schema definition for this tool.
     *
     * @return the tool schema
     */
    Tool schema();

    /**
     * Executes the tool with the provided arguments.
     *
     * @param args the arguments extracted from the model's tool call
     * @return the result of the tool execution as a string
     */
    String execute(ToolArguments args);

    /**
     * Callback invoked immediately before a tool is executed.
     *
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ToolRegistry registry = ToolRegistry.builder()
     *     .register(new WeatherTool(toolService))
     *     .beforeExecution((toolName, toolArgs) -> {
     *         System.out.println("Executing tool: " + toolName);
     *         System.out.println("Arguments: " + toolArgs);
     *     }).build();
     * }</pre>
     *
     */
    @FunctionalInterface
    interface BeforeExecution {
        /**
         * Accepts the tool name and arguments before execution.
         *
         * @param toolName the name of the tool about to be executed
         * @param toolArgs the arguments that will be passed to the tool
         */
        void accept(String toolName, ToolArguments toolArgs);
    }

    /**
     * Callback invoked immediately after a tool completes successfully.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ToolRegistry registry = ToolRegistry.builder()
     *     .register(new WeatherTool(toolService))
     *     .afterExecution((toolName, toolArgs, result) -> {
     *         System.out.println("Tool " + toolName + " completed");
     *         System.out.println("Result: " + result);
     *     }).build();
     * }</pre>
     *
     */
    @FunctionalInterface
    interface AfterExecution {
        /**
         * Accepts the tool name, arguments, and result after successful execution.
         *
         * @param toolName the name of the tool that was executed
         * @param toolArgs the arguments that were passed to the tool
         * @param result the result returned by the tool
         */
        void accept(String toolName, ToolArguments toolArgs, String result);
    }

    /**
     * Callback invoked when a tool execution fails with an exception.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ToolRegistry registry = ToolRegistry.builder()
     *     .register(new WeatherTool(toolService))
     *     .onError((toolName, toolArgs, error) -> {
     *         System.err.println("Tool " + toolName + " failed");
     *         System.err.println("Error: " + error.getMessage());
     *     }).build();
     * }</pre>
     *
     */
    @FunctionalInterface
    interface OnError {
        /**
         * Accepts the tool name, arguments, and exception when execution fails.
         *
         * @param toolName the name of the tool that failed
         * @param toolArgs the arguments that were passed to the tool
         * @param error the exception that was thrown during execution
         */
        void accept(String toolName, ToolArguments toolArgs, Exception error);
    }
}