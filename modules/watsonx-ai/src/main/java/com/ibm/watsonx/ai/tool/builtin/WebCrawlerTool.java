/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.requireNonNull;
import java.util.Map;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool for fetching the content of web pages.
 */
@Experimental
public final class WebCrawlerTool {

    private final ToolService toolService;

    public WebCrawlerTool(ToolService toolService) {
        this.toolService = requireNonNull(toolService, "ToolService can't be null");
    }

    /**
     * Fetches the content of a single web page.
     *
     * @param url The URL of the web page to fetch.
     * @return a string containing the website data.
     */
    public String process(String url) {

        requireNonNull(url, "The URL must be provided");

        var structuredInput = Map.<String, Object>of("url", url);
        var result = toolService.run(ToolRequest.structuredInput("WebCrawler", structuredInput));
        return (String) fromJson(fromJson(result, String.class), Map.class).get("content");
    }
}
