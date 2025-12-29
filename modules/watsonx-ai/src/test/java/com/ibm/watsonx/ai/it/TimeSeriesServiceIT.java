/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.timeseries.ForecastData;
import com.ibm.watsonx.ai.timeseries.InputSchema;
import com.ibm.watsonx.ai.timeseries.TimeSeriesParameters;
import com.ibm.watsonx.ai.timeseries.TimeSeriesService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class TimeSeriesServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final Authenticator authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final TimeSeriesService timeSeriesService = TimeSeriesService.builder()
        .baseUrl(URL)
        .authenticator(authentication)
        .projectId(PROJECT_ID)
        .modelId("ibm/granite-ttm-512-96-r2")
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_return_forecast_results_with_valid_metadata_and_output() {

        var inputSchema = InputSchema.builder()
            .timestampColumn("date")
            .addIdColumn("ID1")
            .addTargetColumn("TARGET1")
            .build();

        Random rand = new Random();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start = LocalDateTime.of(2023, 10, 1, 0, 0);

        var dates = IntStream.range(0, 512)
            .mapToObj(i -> start.plusHours(i).format(formatter))
            .collect(Collectors.toList());

        var target = IntStream.range(0, 512)
            .mapToDouble(v -> rand.nextDouble())
            .boxed()
            .collect(Collectors.toList());

        var data = ForecastData.create()
            .add("ID1", "D1", 512)
            .addAll("TARGET1", target)
            .addAll("date", dates);

        var parameters = TimeSeriesParameters.builder()
            .predictionLength(6)
            .build();

        var response = timeSeriesService.forecast(inputSchema, data, parameters);
        assertNotNull(response);
        assertNotNull(response.modelId());
        assertNotNull(response.modelVersion());
        assertNotNull(response.createdAt());
        assertNotNull(response.inputDataPoints());
        assertNotNull(response.outputDataPoints());
        assertNotNull(response.createdAt());
        assertNotNull(response.results());
        assertNotNull(response.results().get(0).get("ID1"));
        assertNotNull(response.results().get(0).get("TARGET1"));
        assertNotNull(response.results().get(0).get("date"));
    }
}
