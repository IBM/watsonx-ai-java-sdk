/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static java.util.Objects.requireNonNull;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool.GoogleSearchResult;

/**
 * Tool to search a query on Wikipedia.
 */
public class WikipediaTool {

    private final ToolService toolService;

    /**
     * Constructs a new {@code WikipediaTool} with the specified {@link ToolService}.
     *
     * @param toolService the service used to execute the tool calls.
     */
    public WikipediaTool(ToolService toolService) {
        this.toolService = requireNonNull(toolService, "ToolService can't be null");
    }

    /**
     * Search for online trends, news, current events, real-time information, or research topics.
     *
     * @param query Query to search for.
     * @return {@link List} of {@link GoogleSearchResult} that contain the retrieved web page content.
     */
    public String search(String query) {

        requireNonNull(query, "input can't be null");

        var structuredInput = Map.<String, Object>of("query", query);
        return toolService.run(ToolRequest.structuredInput("Wikipedia", structuredInput, null));
    }
}
