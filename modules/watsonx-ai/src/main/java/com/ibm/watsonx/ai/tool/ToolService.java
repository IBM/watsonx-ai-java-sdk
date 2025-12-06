/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.util.List;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
import com.ibm.watsonx.ai.tool.builtin.PythonInterpreterTool;
import com.ibm.watsonx.ai.tool.builtin.TavilySearchTool;
import com.ibm.watsonx.ai.tool.builtin.WeatherTool;
import com.ibm.watsonx.ai.tool.builtin.WebCrawlerTool;
import com.ibm.watsonx.ai.tool.builtin.WikipediaTool;

/**
 * Service class to interact with IBM watsonx.ai Utility Agent Tools APIs.
 * <p>
 * This is a generic service for invoking any available tool by name. However, specific implementations for common tools already exist, such as
 * {@link GoogleSearchTool}, {@link WeatherTool}, {@link WebCrawlerTool} and {@link WikipediaTool}. It is recommended to use these concrete tool
 * classes where possible.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ToolService service = ToolService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("api-key")   // creates an IBM Cloud AuthenticationProvider
 *     .build();
 *
 * var structuredInput = Map.<String, Object>of("q", input);
 * var config = Map.<String, Object>of("maxResults", maxResults);
 * var input = ToolRequest.structuredInput("GoogleSearch", structuredInput, config);
 * var result = toolService.run(input);
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}.
 *
 * @see GoogleSearchTool
 * @see TavilySearchTool
 * @see WeatherTool
 * @see WebCrawlerTool
 * @see WikipediaTool
 * @see PythonInterpreterTool
 * @see AuthenticationProvider
 */
@Experimental
public class ToolService extends WatsonxService {
    public record Resources(List<UtilityTool> resources) {}

    private final ToolRestClient client;

    private ToolService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticationProvider(), "authenticationProvider cannot be null");
        client = ToolRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticationProvider(builder.authenticationProvider())
            .build();
    }

    /**
     * Retrieves the complete list of supported utility tools.
     *
     * @return a list of {@link UtilityTool} instances representing all available tools
     */
    public Resources getAll() {
        return getAll(ToolParameters.builder().build());
    }

    /**
     * Retrieves the complete list of supported utility tools.
     *
     * @param parameters Parameters to customize the request
     * @return a list of {@link UtilityTool} instances representing all available tools
     */
    public Resources getAll(ToolParameters parameters) {
        return client.getAll(nonNull(parameters) ? parameters.transactionId() : null);
    }

    /**
     * Retrieves the metadata and configuration details of a specific utility tool by name.
     *
     * @param name the unique name of the tool.
     * @return a {@link UtilityTool} instance representing the requested tool.
     */
    public UtilityTool getByName(String name) {
        return getByName(name, ToolParameters.builder().build());
    }

    /**
     * Retrieves the metadata and configuration details of a specific utility tool by name.
     *
     * @param name the unique name of the tool.
     * @param parameters Parameters to customize the request
     * @return a {@link UtilityTool} instance representing the requested tool.
     */
    public UtilityTool getByName(String name, ToolParameters parameters) {
        requireNonNull(name, "The name of the tool must be provided");
        var transactionId = nonNull(parameters) ? parameters.transactionId() : null;
        return client.getByName(transactionId, name);
    }

    /**
     * Executes the specified utility tool by sending a request with the given input and configuration.
     * <p>
     * The response is expected to contain a JSON object with an {@code output} field, which is returned as a string.
     *
     * @param toolRequest the {@link ToolRequest} object containing the tool name, input, and config.
     * @return the output string produced by the tool.
     */
    public String run(ToolRequest toolRequest) {
        return run(toolRequest, ToolParameters.builder().build());
    }

    /**
     * Executes the specified utility tool by sending a request with the given input and configuration.
     * <p>
     * The response is expected to contain a JSON object with an {@code output} field, which is returned as a string.
     *
     * @param toolRequest the {@link ToolRequest} object containing the tool name, input, and config.
     * @param parameters Parameters to customize the request
     * @return the output string produced by the tool.
     */
    public String run(ToolRequest toolRequest, ToolParameters parameters) {
        requireNonNull(toolRequest, "The tool run request must be provided");
        requireNonNull(toolRequest.toolName(), "The name of the tool must be provided");
        requireNonNull(toolRequest.input(), "The input of the tool must be provided");
        var transactionId = nonNull(parameters) ? parameters.transactionId() : null;
        return client.run(transactionId, toolRequest);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ToolService service = ToolService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("api-key")   // creates an IBM Cloud AuthenticationProvider
     *     .build();
     *
     * var structuredInput = Map.<String, Object>of("q", input);
     * var config = Map.<String, Object>of("maxResults", maxResults);
     * var input = ToolRequest.structuredInput("GoogleSearch", structuredInput, config);
     * var result = toolService.run(input);
     * }</pre>
     *
     * @see GoogleSearchTool
     * @see WeatherTool
     * @see WebCrawlerTool
     * @see WikipediaTool
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ToolService} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxService.Builder<Builder> {

        private Builder() {}

        @Override
        public Builder baseUrl(CloudRegion url) {
            return super.baseUrl(url.getWxEndpoint());
        }

        /**
         * Builds a {@link ToolService} instance using the configured parameters.
         *
         * @return a new instance of {@link ToolService}
         */
        public ToolService build() {
            return new ToolService(this);
        }
    }
}
