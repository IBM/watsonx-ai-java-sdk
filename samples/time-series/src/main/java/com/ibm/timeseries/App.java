/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.timeseries;

import static com.ibm.watsonx.ai.core.Json.prettyPrint;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.timeseries.ForecastData;
import com.ibm.watsonx.ai.timeseries.InputSchema;
import com.ibm.watsonx.ai.timeseries.TimeSeriesParameters;
import com.ibm.watsonx.ai.timeseries.TimeSeriesService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        try {

            var url = URI.create(config.getValue("WATSONX_URL", String.class));
            var apiKey = config.getValue("WATSONX_API_KEY", String.class);
            var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

            TimeSeriesService tsService = TimeSeriesService.builder()
                .apiKey(apiKey)
                .projectId(projectId)
                .baseUrl(url)
                .modelId("ibm/granite-ttm-512-96-r2")
                .logRequests(true)
                .logResponses(true)
                .build();

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

            System.out.println("""
                Forecast result:
                %s
                """.formatted(prettyPrint(tsService.forecast(inputSchema, data, parameters))));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
