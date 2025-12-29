/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import java.util.List;
import java.util.Map;

/**
 * Represents the request used by the time series forecast api.
 * <p>
 * This request contains all necessary data, schema, and parameters to perform forecasting on historical time series data.
 *
 * @param modelId the id of the forecasting model
 * @param spaceId the id of the space containing the resource
 * @param projectId the id of the project containing the resource
 * @param data the time series data payload
 * @param schema metadata describing the data structure
 * @param parameters additional forecast parameters
 */
public record ForecastRequest(
    String modelId,
    String spaceId,
    String projectId,
    Map<String, List<Object>> data,
    InputSchema schema,
    Map<String, List<Object>> futureData,
    Parameters parameters) {

    /**
     * Parameters for controlling the forecast.
     *
     * @param predictionLength the number of periods to predict beyond the last timestamp (≥1, max model default)
     */
    public record Parameters(Integer predictionLength) {}
}