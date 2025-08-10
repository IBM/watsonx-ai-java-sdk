/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

import java.util.Map;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.tool.ToolRequest.StructuredInput;
import com.ibm.watsonx.ai.tool.ToolRequest.UnstructuredInput;

/**
 * Represents a request to run a utility tool.
 *
 * @see StructuredInput
 * @see UnstructuredInput
 */
@Experimental
public sealed interface ToolRequest permits StructuredInput, UnstructuredInput {

    /**
     * Gets the name of the tool to be executed.
     *
     * @return the tool name.
     */
    public String toolName();

    /**
     * Gets the input provided to the tool.
     *
     * @return the tool input.
     */
    public Object input();

    /**
     * Gets the configuration passed to the tool.
     *
     * @return the configuration map.
     */
    public Map<String, Object> config();

    /**
     * Creates a structured input tool request with no configuration.
     *
     * @param toolName the name of the tool
     * @param input the structured input map
     * @return a new {@link StructuredInput} instance
     */
    public static ToolRequest structuredInput(String toolName, Map<String, Object> input) {
        return structuredInput(toolName, input, null);
    }

    /**
     * Creates a structured input tool request with configuration.
     *
     * @param toolName the name of the tool.
     * @param input the structured input map.
     * @param config the configuration map.
     * @return a new {@link StructuredInput} instance.
     */
    public static ToolRequest structuredInput(String toolName, Map<String, Object> input, Map<String, Object> config) {
        return new StructuredInput(toolName, input, config);
    }

    /**
     * Creates an unstructured input tool request with no configuration.
     *
     * @param toolName the name of the tool
     * @param input the input string
     * @return a new {@link UnstructuredInput} instance
     */
    public static ToolRequest unstructuredInput(String toolName, String input) {
        return unstructuredInput(toolName, input, null);
    }

    /**
     * Creates an unstructured input tool request with configuration.
     *
     * @param toolName the name of the tool.
     * @param input the input string.
     * @param config the configuration map.
     * @return a new {@link UnstructuredInput} instance.
     */
    public static ToolRequest unstructuredInput(String toolName, String input, Map<String, Object> config) {
        return new UnstructuredInput(toolName, input, config);
    }

    /**
     * Represents a tool request with a structured input map, typically used for tools that define an input schema.
     *
     * @param toolName The name of the tool to run.
     * @param input A map representing the structured input, conforming to the tool's input schema.
     * @param config Configuration parameters specific to the tool.
     */
    public static record StructuredInput(String toolName, Map<String, Object> input, Map<String, Object> config)
        implements
            ToolRequest {}

    /**
     * Represents a tool request with a plain string input, used for tools that do not define an input schema.
     *
     * @param toolName The name of the tool to run.
     * @param input A string representing the unstructured input for the tool.
     * @param config Configuration parameters specific to the tool.
     */
    public static record UnstructuredInput(String toolName, String input, Map<String, Object> config)
        implements
            ToolRequest {}
}
