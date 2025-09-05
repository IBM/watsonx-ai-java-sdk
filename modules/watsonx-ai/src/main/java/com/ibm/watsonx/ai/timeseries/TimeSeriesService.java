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
import static java.util.Optional.ofNullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *     .url("https://...")      // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
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
 * TimeSeriesRequest request = TimeSeriesRequest.builder()
 *     .inputSchema(inputSchema)
 *     .data(data)
 *     .build();
 *
 * ForecastResponse response = tsService.forecast(request);
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@link #authenticationProvider(AuthenticationProvider)}.
 *
 * @see AuthenticationProvider
 */
public final class TimeSeriesService extends ModelService implements TimeSeriesProvider {

    public static final Logger logger = LoggerFactory.getLogger(TimeSeriesService.class);

    protected TimeSeriesService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
    }

    @Override
    public ForecastResponse forecast(TimeSeriesRequest request) {

        if (nonNull(request.getDeploymentId()))
            logger.info("The deploymentId parameter can not be used with the TimeSeriesService. Use the DeploymentService instead");

        var inputSchema = request.getInputSchema();
        var data = request.getData();
        var parameters = request.getParameters();

        String modelId = this.modelId;
        String projectId = this.projectId;
        String spaceId = this.spaceId;
        String transactionId = null;
        Parameters requestParameters = null;

        if (nonNull(parameters)) {
            modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
            projectId = ofNullable(parameters.getProjectId()).orElse(this.projectId);
            spaceId = ofNullable(parameters.getSpaceId()).orElse(this.spaceId);
            transactionId = parameters.getTransactionId();
            requestParameters = parameters.toParameters();
        }

        var forecastRequest = new ForecastRequest(modelId, spaceId, projectId, data.asMap(), inputSchema, null, requestParameters);

        var httpRequest = HttpRequest
            .newBuilder(URI.create(url.toString() + "%s/time_series/forecast?version=%s".formatted(ML_API_PATH, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(timeout)
            .POST(BodyPublishers.ofString(toJson(forecastRequest)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), ForecastResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a forecast using the provided schema and data.
     *
     * @param inputSchema the schema describing the time series
     * @param data the historical data payload to use for prediction
     * @return a {@link ForecastResponse} containing the forecasted time series values
     */
    public ForecastResponse forecast(InputSchema inputSchema, ForecastData data) {
        return forecast(inputSchema, data, null);
    }

    /**
     * Generates a forecast using the provided schema and data.
     *
     * @param inputSchema the schema describing the time series
     * @param data the historical data payload to use for prediction
     * @param parameters the parameters to configure time series behavior
     *
     * @return a {@link ForecastResponse} containing the forecasted time series values
     */
    public ForecastResponse forecast(InputSchema inputSchema, ForecastData data, TimeSeriesParameters parameters) {
        return forecast(
            TimeSeriesRequest.builder()
                .inputSchema(inputSchema)
                .data(data)
                .parameters(parameters)
                .build());
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TimeSeriesService tsService = TimeSeriesService.builder()
     *     .url("https://...")      // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
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
     * TimeSeriesRequest request = TimeSeriesRequest.builder()
     *     .inputSchema(inputSchema)
     *     .data(data)
     *     .build();
     *
     * ForecastResponse response = tsService.forecast(request);
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TimeSeriesService} instances with configurable parameters.
     */
    public static class Builder extends ModelService.Builder<Builder> {

        private Builder() {}

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
