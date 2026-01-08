/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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
 * Tool to fetch weather information.
 */
@Experimental
public class WeatherTool implements ExecutableTool {

    private static final String TOOL_SCHEMA_NAME = "weather";
    private static final Tool TOOL_SCHEMA = Tool.of(
        TOOL_SCHEMA_NAME,
        "Find the weather for a location.",
        JsonSchema.object()
            .property("location", JsonSchema.string("Name of the location"))
            .property("country", JsonSchema.string("Name of the country"))
            .required("location")
            .build());

    private final ToolService toolService;

    /**
     * Constructs a new {@code WeatherTool} with the specified {@link ToolService}.
     *
     * @param toolService the service used to execute the tool calls.
     */
    public WeatherTool(ToolService toolService) {
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
        if (isNull(args) || !args.contains("location"))
            throw new IllegalArgumentException("location argument is required");

        String location = args.get("location");
        return args.contains("country")
            ? find(location, args.get("country"))
            : find(location);
    }

    /**
     * Finds weather information for the given location.
     *
     * @param location the name of the location.
     * @return a string containing the weather data.
     */
    public String find(String location) {
        return find(location, null);
    }

    /**
     * Finds weather information for the given location and country.
     *
     * @param location the name of the location.
     * @param country the name of the country.
     * @return a string containing the weather data.
     */
    public String find(String location, String country) {

        requireNonNull(location, "Location can't be null");

        var structuredInput = nonNull(country)
            ? Map.<String, Object>of("location", location, "country", country)
            : Map.<String, Object>of("location", location);

        return toolService.run(ToolRequest.structuredInput("Weather", structuredInput));
    }
}
