/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool.builtin;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.Map;
import com.ibm.watsonx.ai.core.Beta;
import com.ibm.watsonx.ai.tool.ToolRequest;
import com.ibm.watsonx.ai.tool.ToolService;

/**
 * Tool to fetch weather information.
 */
@Beta
public class WeatherTool {

    private final ToolService toolService;

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
