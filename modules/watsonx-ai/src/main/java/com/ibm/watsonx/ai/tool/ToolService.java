/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.Experimental;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
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
 *     .url("https://...")  // or use CloudRegion
 *     .apiKey("api-key")   // creates an IAM-based AuthenticationProvider
 *     .build();
 *
 * var structuredInput = Map.<String, Object>of("q", input);
 * var config = Map.<String, Object>of("maxResults", maxResults);
 * var input = ToolRequest.structuredInput("GoogleSearch", structuredInput, config);
 * var result = toolService.run(input);
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@link #authenticationProvider(AuthenticationProvider)}.
 *
 * @see GoogleSearchTool
 * @see TavilySearchTool
 * @see WeatherTool
 * @see WebCrawlerTool
 * @see WikipediaTool
 * @see AuthenticationProvider
 */
@Experimental
public final class ToolService extends WatsonxService {

    private static final String API_PATH = "/v1-beta/utility_agent_tools";

    public record Resources(List<UtilityTool> resources) {}

    public ToolService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
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

        var httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString().concat(API_PATH)))
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET();

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), Resources.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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

        var httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString() + "%s/%s".formatted(API_PATH, name)))
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET();

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), UtilityTool.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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

        var httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString() + "%s/run".formatted(API_PATH)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(BodyPublishers.ofString(toJson(toolRequest)));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            var result = fromJson(httpReponse.body(), Map.class);
            return (String) result.get("output");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ToolService service = ToolService.builder()
     *     .url("https://...")  // or use CloudRegion
     *     .apiKey("api-key")   // creates an IAM-based AuthenticationProvider
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
    public static class Builder extends WatsonxService.Builder<Builder> {

        private Builder() {}

        @Override
        public Builder url(CloudRegion url) {
            return super.url(url.getWxEndpoint());
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
