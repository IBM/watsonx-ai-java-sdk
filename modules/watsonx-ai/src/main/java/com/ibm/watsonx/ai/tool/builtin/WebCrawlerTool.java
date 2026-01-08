/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import java.util.Map;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolArguments;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool for fetching the content of web pages.
 */
@Experimental
public class WebCrawlerTool implements ExecutableTool {

    private static final String TOOL_SCHEMA_NAME = "webcrawler";
    private static final Tool TOOL_SCHEMA = Tool.of(
        TOOL_SCHEMA_NAME,
        "Fetches and extracts content from a specific webpage URL. Use this tool when you need to retrieve, read, or summarize the content of a known webpage. Do not use for web search or discovering new URLs.",
        JsonSchema.object()
            .property(
                "url",
                JsonSchema
                    .string("The complete URL of the webpage to fetch")
                    .pattern("^(https?://)?([\\w.-]+)\\.([a-z]{2,})(:[0-9]+)?(/[\\w./-]*)*/?$"))
            .required("url")
            .build());

    private final ToolService toolService;

    /**
     * Constructs a new {@code WebCrawlerTool} with the specified {@link ToolService}.
     *
     * @param toolService the service used to execute the tool calls
     */
    public WebCrawlerTool(ToolService toolService) {
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
        if (isNull(args) || !args.contains("url"))
            throw new IllegalArgumentException("url argument is required");

        return process(args.get("url"));
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
