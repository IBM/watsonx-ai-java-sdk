/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tool;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.core.Beta;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.BearerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
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
 *     .url("https://...") // or use CloudRegion
 *     .authenticationProvider(authProvider)
 *     .build();
 *
 * var structuredInput = Map.<String, Object>of("q", input);
 * var config = Map.<String, Object>of("maxResults", maxResults);
 * var input = ToolRequest.structuredInput("GoogleSearch", structuredInput, config);
 * var result = toolService.run(input);
 * }</pre>
 *
 * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#get-utility-agent-tools" target="_blank">official
 * documentation</a>.
 *
 * @see GoogleSearchTool
 * @see WeatherTool
 * @see WebCrawlerTool
 * @see WikipediaTool
 * @see AuthenticationProvider
 */
@Beta
public final class ToolService {

    private static final String API_PATH = "/v1-beta/utility_agent_tools";

    public record Resources(List<UtilityTool> resources) {}

    private final URI url;
    private final Duration timeout;
    private final AuthenticationProvider authenticationProvider;
    private final SyncHttpClient syncHttpClient;

    public ToolService(Builder builder) {
        url = requireNonNull(builder.url, "The url must be provided");

        timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(10));
        authenticationProvider = requireNonNull(builder.authenticationProvider, "The authentication provider is mandatory");

        boolean logRequests = requireNonNullElse(builder.logRequests, false);
        boolean logResponses = requireNonNullElse(builder.logResponses, false);

        var httpClient = requireNonNullElse(builder.httpClient, HttpClient.newBuilder().connectTimeout(timeout).build());
        var syncHttpClientBuilder = SyncHttpClient.builder().httpClient(httpClient);
        var asyncHttpClientBuilder = AsyncHttpClient.builder().httpClient(httpClient);

        var retryInterceptor = RetryInterceptor.onTokenExpired(1);
        syncHttpClientBuilder.interceptor(retryInterceptor);
        asyncHttpClientBuilder.interceptor(retryInterceptor);

        var bearerInterceptor = new BearerInterceptor(authenticationProvider);
        syncHttpClientBuilder.interceptor(bearerInterceptor);
        asyncHttpClientBuilder.interceptor(bearerInterceptor);

        if (logRequests || logResponses)
            syncHttpClientBuilder.interceptor(new LoggerInterceptor(logRequests, logResponses));

        syncHttpClient = syncHttpClientBuilder.build();
    }

    /**
     * Retrieves the complete list of supported utility tools.
     * <p>
     * Each tool contains metadata including name, description, input/config schemas, and agent instructions.
     *
     * @return a list of {@link UtilityTool} instances representing all available tools
     */
    public Resources getAll() {

        HttpRequest httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString().concat(API_PATH)))
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET().build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
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

        requireNonNull(name, "The name of the tool must be provided");

        HttpRequest httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString() + "%s/%s".formatted(API_PATH, name)))
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET().build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
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
        requireNonNull(toolRequest, "The tool run request must be provided");
        requireNonNull(toolRequest.toolName(), "The name of the tool must be provided");
        requireNonNull(toolRequest.input(), "The input of the tool must be provided");

        HttpRequest httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString() + "%s/run".formatted(API_PATH)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(BodyPublishers.ofString(toJson(toolRequest)))
                .build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
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
     *     .url("https://...") // or use CloudRegion
     *     .authenticationProvider(authProvider)
     *     .build();
     * }</pre>
     *
     * @see GoogleSearchTool
     * @see WeatherTool
     * @see WebCrawlerTool
     * @see WikipediaTool
     * @see AuthenticationProvider
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ToolService} instances with configurable parameters.
     */
    public static class Builder {
        private URI url;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private HttpClient httpClient;
        private AuthenticationProvider authenticationProvider;

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param url the endpoint URL as a string
         */
        public Builder url(URI url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param url the endpoint URL as a string
         */
        public Builder url(String url) {
            return url(URI.create(url));
        }

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param url the endpoint URL as a string
         */
        public Builder url(CloudRegion url) {
            return url(url.getWxEndpoint());
        }

        /**
         * Enables or disables logging of the request payload.
         *
         * @param logRequests {@code true} to log the request, {@code false} otherwise
         */
        public Builder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        /**
         * Enables or disables logging of the response payload.
         *
         * @param logResponses {@code true} to log the response, {@code false} otherwise
         */
        public Builder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        /**
         * Sets the request timeout.
         *
         * @param timeout {@link Duration} timeout.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets a custom {@link HttpClient} to be used for making requests.
         *
         * @param httpClient the {@code HttpClient} instance to use
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Sets the {@link AuthenticationProvider} used for authenticating requests.
         *
         * @param authenticationProvider {@link AuthenticationProvider} instance
         */
        public Builder authenticationProvider(AuthenticationProvider authenticationProvider) {
            this.authenticationProvider = authenticationProvider;
            return this;
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
