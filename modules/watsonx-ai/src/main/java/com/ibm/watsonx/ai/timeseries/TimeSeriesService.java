/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.timeseries.ForecastRequest.Parameters;

/**
 * Service class to interact with IBM watsonx.ai Time Series Forecast APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TimeSeriesService tsService = TimeSeriesService.builder()
 *     .url("https://...") // or use CloudRegion
 *     .authenticationProvider(authProvider)
 *     .projectId("my-project-id")
 *     .modelId("ibm/granite-ttm-1536-96-r2")
 *     .build();
 *
 * InputSchema inputSchema = InputSchema.builder()
 *     .timestampColumn("date")
 *     .addIdColumn("ID1")
 *     .build();
 *
 * ForecastData data = ForecastData.create()
 *     .add("date", "2020-01-01T00:00:00")
 *     .add("date", "2020-01-01T01:00:00")
 *     .add("date", "2020-01-05T01:00:00")
 *     .add("ID1", "D1", 3)
 *     .addAll("TARGET1", 1.46, 2.34, 4.55);
 *
 * ForecastResponse response =
 *     tsService.forecast(inputSchema, data);
 * }</pre>
 *
 * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#time-series-forecast" target="_blank"> official documentation</a>.
 *
 * @see AuthenticationProvider
 */
public final class TimeSeriesService extends ModelService implements TimeSeriesProvider {

    protected TimeSeriesService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
    }

    @Override
    public ForecastResponse forecast(InputSchema inputSchema, ForecastData data, TimeSeriesParameters parameters) {
        requireNonNull(inputSchema, "InputSchema cannot be null");
        requireNonNull(data, "Data cannot be null");

        String modelId = this.modelId;
        String projectId = this.projectId;
        String spaceId = this.spaceId;
        Parameters requestParameters = null;

        if (nonNull(parameters)) {
            modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
            projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
            spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
            requestParameters = parameters.toParameters();
        }

        var forecastRequest = new ForecastRequest(modelId, spaceId, projectId, data.asMap(), inputSchema, null, requestParameters);

        var httpRequest = HttpRequest
            .newBuilder(URI.create(url.toString() + "%s/time_series/forecast?version=%s".formatted(ML_API_PATH, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(timeout)
            .POST(BodyPublishers.ofString(toJson(forecastRequest)))
            .build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpReponse.body(), ForecastResponse.class);

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
     * TimeSeriesService tsService = TimeSeriesService.builder()
     *     .url("https://...") // or use CloudRegion
     *     .authenticationProvider(authProvider)
     *     .projectId("my-project-id")
     *     .modelId("ibm/granite-ttm-1536-96-r2")
     *     .build();
     *
     * InputSchema inputSchema = InputSchema.builder()
     *     .timestampColumn("date")
     *     .addIdColumn("ID1")
     *     .build();
     *
     * ForecastData data = ForecastData.create()
     *     .add("date", "2020-01-01T00:00:00")
     *     .add("date", "2020-01-01T01:00:00")
     *     .add("date", "2020-01-05T01:00:00")
     *     .add("ID1", "D1", 3)
     *     .addAll("TARGET1", 1.46, 2.34, 4.55);
     *
     * ForecastResponse response =
     *     tsService.forecast(inputSchema, data);
     * }</pre>
     *
     * @see AuthenticationProvider
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TimeSeriesService} instances with configurable parameters.
     */
    public static class Builder extends ModelService.Builder<Builder> {

        /**
         * Builds a {@link TimeSeriesService} instance using the configured parameters.
         *
         * @return a new instance of {@link TimeSeriesService}
         */
        public TimeSeriesService build() {
            return new TimeSeriesService(this);
        }
    }
}
