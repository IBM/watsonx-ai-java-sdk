/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.core.Beta;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool to search online for trends, news, current events, real-time information, or research topics.
 */
@Beta
public class GoogleSearchTool {

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

        requireNonNull(query, "input can't be null");
        maxResults = requireNonNullElse(maxResults, 10);

        var structuredInput = Map.<String, Object>of("q", query);
        var config = Map.<String, Object>of("maxResults", maxResults);

        var result = toolService.run(ToolRequest.structuredInput("GoogleSearch", structuredInput, config));
        return fromJson(result, new TypeToken<List<GoogleSearchResult>>() {});
    }
}
