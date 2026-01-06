/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool to fetch weather information.
 */
@Experimental
public class WeatherTool {

    private final ToolService toolService;

    /**
     * Pre-configured tool definition.
     */
    public static final Tool TOOL_SCHEMA = Tool.of(
        "weather",
        "Find the weather for a location.",
        JsonSchema.object()
            .property("location", JsonSchema.string("Name of the location"))
            .property("country", JsonSchema.string("Name of the country"))
            .required("location")
            .build());

    /**
     * Constructs a new {@code WeatherTool} with the specified {@link ToolService}.
     *
     * @param toolService the service used to execute the tool calls.
     */
    public WeatherTool(ToolService toolService) {
        this.toolService = requireNonNull(toolService, "ToolService can't be null");
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
