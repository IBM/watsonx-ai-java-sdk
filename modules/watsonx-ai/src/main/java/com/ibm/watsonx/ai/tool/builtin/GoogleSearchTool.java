/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool to search online for trends, news, current events, real-time information, or research topics.
 */
@Experimental
public class GoogleSearchTool implements ExecutableTool {

    private static final String TOOL_SCHEMA_NAME = "google_search";
    private static final Tool TOOL_SCHEMA = Tool.of(
        TOOL_SCHEMA_NAME,
        "Search for online trends, news, current events, real-time information, or research topics.",
        JsonSchema.object()
            .property("query", JsonSchema.string("Google search query"))
            .required("query")
            .build());

    public record GoogleSearchResult(String title, String description, String url) {}

    private final ToolService toolService;

    /**
     * Constructs a new {@code GoogleSearchTool} with the specified {@link ToolService}.
     *
     * @param toolService the service used to execute the tool calls.
     */
    public GoogleSearchTool(ToolService toolService) {
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

        var result = search(args.get("query"));
        return Json.prettyPrint(result);
    }

    /**
     * Search for online trends, news, current events, real-time information, or research topics.
     *
     * @param query Query to search for.
     * @return {@link List} of {@link GoogleSearchResult} that contain the retrieved web page content.
     */
    public List<GoogleSearchResult> search(String query) {
        return search(query, 10);
    }

    /**
     * Search for online trends, news, current events, real-time information, or research topics.
     *
     * @param query Query to search for.
     * @param maxResults Max number of results.
     * @return {@link List} of {@link GoogleSearchResult} that contain the retrieved web page content.
     */
    public List<GoogleSearchResult> search(String query, Integer maxResults) {

        requireNonNull(query, "query can't be null");
        maxResults = requireNonNullElse(maxResults, 10);

        var structuredInput = Map.<String, Object>of("q", query);
        var config = Map.<String, Object>of("maxResults", maxResults);

        var result = toolService.run(ToolRequest.structuredInput("GoogleSearch", structuredInput, config));
        return fromJson(result, new TypeToken<List<GoogleSearchResult>>() {});
    }
}
