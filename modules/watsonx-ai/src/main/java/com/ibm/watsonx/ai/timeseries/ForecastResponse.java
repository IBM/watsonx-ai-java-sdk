/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import java.util.List;
import java.util.Map;

/**
 * Represents the response returned by the time series forecast api.
 *
 * @param modelId the identifier of the model used to generate the forecast
 * @param createdAt the ISO 8601 timestamp when the response was created
 * @param results a list of prediction result maps, each containing forecasted values
 * @param inputDataPoints the total number of input data points (rows * input columns)
 * @param outputDataPoints the total number of forecasted data points
 */
public record ForecastResponse(
    String modelId,
    String createdAt,
    List<Map<String, Object>> results,
    int inputDataPoints,
    int outputDataPoints) {}