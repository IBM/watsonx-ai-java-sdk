/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * This interface defines the contract for invoking time series forecast operations by providing historical data and input schema metadata.
 *
 * @see TimeSeriesService
 * @see DeploymentService
 */
public interface TimeSeriesProvider {

    /**
     * Generates a forecast using the provided schema and data.
     *
     * @param inputSchema the schema describing the time series
     * @param data the historical data payload to use for prediction
     * @return a {@link ForecastResponse} containing the forecasted time series values
     */
    public default ForecastResponse forecast(InputSchema inputSchema, ForecastData data) {
        return forecast(inputSchema, data, null);
    }

    /**
     * Generates a forecast using the provided schema, data, and parameters.
     *
     * @param inputSchema the schema describing the time series
     * @param data the historical data payload to use for prediction
     * @param parameters additional forecasting configuration
     * @return a {@link ForecastResponse} containing the forecasted time series values
     */
    public ForecastResponse forecast(InputSchema inputSchema, ForecastData data, TimeSeriesParameters parameters);
}
