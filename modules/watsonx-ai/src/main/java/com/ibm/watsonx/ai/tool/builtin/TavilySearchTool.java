/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool to search for online trends, news, current events, real-time information, or research topics.
 */
@Experimental
public class TavilySearchTool {

    public record TavilySearchResult(String title, String url, String content, Double score) {}

    /**
     * Pre-configured tool definition.
     */
    public static final Tool TOOL_SCHEMA = Tool.of(
        "tavily_search",
        "Search for online trends, news, current events, real-time information, or research topics.",
        JsonSchema.object()
            .property("query", JsonSchema.string("Tavily search query"))
            .required("query")
            .build());

    private final ToolService toolService;
    private final String apiKey;

    /**
     * Constructs a new {@code TavilySearchTool} with the specified {@link ToolService} and API key.
     *
     * @param toolService the service used to execute the tool calls
     * @param apiKey the Tavily API key for authentication
     */
    public TavilySearchTool(ToolService toolService, String apiKey) {
        this.toolService = requireNonNull(toolService, "ToolService can't be null");
        this.apiKey = requireNonNull(apiKey, "ApiKey can't be null");
    }

    /**
     * Search for online trends, news, current events, real-time information, or research topics.
     *
     * @param query Query to search for.
     * @return {@link List} of {@link TavilySearchResult} that contain the retrieved web page content.
     */
    public List<TavilySearchResult> search(String query) {
        return search(query, 10);
    }

    /**
     * Search for online trends, news, current events, real-time information, or research topics.
     *
     * @param query Query to search for.
     * @param maxResults Max number of results.
     * @return {@link List} of {@link TavilySearchResult} that contain the retrieved web page content.
     */
    public List<TavilySearchResult> search(String query, Integer maxResults) {

        requireNonNull(query, "input can't be null");
        maxResults = requireNonNullElse(maxResults, 10);

        var structuredInput = Map.<String, Object>of("query", query);
        var config = Map.<String, Object>of(
            "apiKey", apiKey,
            "maxResults", maxResults
        );

        var result = toolService.run(ToolRequest.structuredInput("TavilySearch", structuredInput, config));
        return fromJson(result, new TypeToken<List<TavilySearchResult>>() {});
    }
}
