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
     * Generates a forecast using the provided {@link TimeSeriesRequest}.
     *
     * @param request a {@link TimeSeriesRequest} containing the input schema, data, and parameters
     * @return a {@link ForecastResponse} containing the forecasted time series values
     */
    ForecastResponse forecast(TimeSeriesRequest request);
}
