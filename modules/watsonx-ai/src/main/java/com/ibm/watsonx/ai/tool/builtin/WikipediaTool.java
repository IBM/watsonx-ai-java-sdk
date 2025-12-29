/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static java.util.Objects.requireNonNull;
import java.util.Map;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

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
