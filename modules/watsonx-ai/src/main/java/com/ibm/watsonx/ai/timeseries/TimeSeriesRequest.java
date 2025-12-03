/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * Represents a time series forecast request.
 * <p>
 * Instances are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * var inputSchema = InputSchema.builder()
 *     .timestampColumn("date")
 *     .addIdColumn("ID1")
 *     .build();
 *
 * var data = ForecastData.create()
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
 * }</pre>
 */
public final class TimeSeriesRequest {
    private String deploymentId;
    private InputSchema inputSchema;
    private ForecastData data;
    private TimeSeriesParameters parameters;

    private TimeSeriesRequest(Builder builder) {
        inputSchema = requireNonNull(builder.inputSchema, "InputSchema cannot be null");
        data = requireNonNull(builder.data, "Data cannot be null");
        parameters = builder.parameters;
        deploymentId = builder.deploymentId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public InputSchema getInputSchema() {
        return inputSchema;
    }

    public ForecastData getData() {
        return data;
    }

    public TimeSeriesParameters getParameters() {
        return parameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TimeSeriesRequest} instances.
     */
    public final static class Builder {
        private String deploymentId;
        private InputSchema inputSchema;
        private ForecastData data;
        private TimeSeriesParameters parameters;

        private Builder() {}

        /**
         * Sets the deployment identifier for the time series request.
         * <p>
         * This value is required if the request will be sent via a {@link DeploymentService}. For other services, this value may be ignored.
         *
         * @param deploymentId the unique identifier of the deployment
         */
        public Builder deploymentId(String deploymentId) {
            this.deploymentId = deploymentId;
            return this;
        }

        /**
         * Sets the input schema for the time series request.
         * <p>
         * The {@link InputSchema} defines the timestamp column and any identifier columns for the time series. This is required for the service to
         * understand the structure of the provided data.
         *
         * @param inputSchema the {@link InputSchema} instance
         */
        public Builder inputSchema(InputSchema inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        /**
         * Sets the actual data for the time series forecast request.
         * <p>
         * The {@link ForecastData} contains all the timestamped records and target values that the service will use for forecasting.
         *
         * @param data the {@link ForecastData} instance
         */
        public Builder data(ForecastData data) {
            this.data = data;
            return this;
        }

        /**
         * Sets the parameters controlling the time series forecast behavior.
         *
         * @param parameters a {@link TimeSeriesParameters} instance
         */
        public Builder parameters(TimeSeriesParameters parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Builds a {@link TimeSeriesRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link TimeSeriesRequest}
         */
        public TimeSeriesRequest build() {
            return new TimeSeriesRequest(this);
        }
    }
}
