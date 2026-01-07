/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import java.util.Map;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool to search a query on Wikipedia.
 */
public class WikipediaTool implements ExecutableTool {

    private static final String TOOL_SCHEMA_NAME = "wikipedia";
    private static final Tool TOOL_SCHEMA = Tool.of(
        TOOL_SCHEMA_NAME,
        "Search a query on Wikipedia.",
        JsonSchema.object()
            .property("query", JsonSchema.string("Wikipedia search query"))
            .required("query")
            .build());

    private final ToolService toolService;

    /**
     * Constructs a new {@code WikipediaTool} with the specified {@link ToolService}.
     *
     * @param toolService the service used to execute the tool calls.
     */
    public WikipediaTool(ToolService toolService) {
        this.toolService = requireNonNull(toolService, "ToolService can't be null");
    }

    @Override
    public String name() {
        return TOOL_SCHEMA_NAME;
    }

    @Override
    public Tool schema() {
        return TOOL_SCHEMA;
    }

    @Override
    public String execute(ToolArguments args) {
        if (isNull(args) || !args.contains("query"))
            throw new IllegalArgumentException("query argument is required");

        return search(args.get("query"));
    }

    /**
     * Searches for information on Wikipedia.
     *
     * @param query Query to search for.
     * @return A string containing the Wikipedia search results.
     */
    public String search(String query) {

        requireNonNull(query, "input can't be null");

        var structuredInput = Map.<String, Object>of("query", query);
        return toolService.run(ToolRequest.structuredInput("Wikipedia", structuredInput, null));
    }
}
