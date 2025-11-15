/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.timeseries.ForecastData;
import com.ibm.watsonx.ai.timeseries.InputSchema;
import com.ibm.watsonx.ai.timeseries.TimeSeriesParameters;
import com.ibm.watsonx.ai.timeseries.TimeSeriesService;

@ExtendWith(MockitoExtension.class)
public class TimeSeriesServiceTest extends AbstractWatsonxTest {

    @Test
    void should_forecast_without_parameters() {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var EXPECTED = """
            {
                  "model_id": "ibm/ttm-1024-96-r2",
                  "created_at": "2020-05-02T16:27:51Z",
                  "results": [
                    {
                      "date": [
                        "2020-01-05T02:00:00",
                        "2020-01-05T03:00:00",
                        "2020-01-06T00:00:00"
                      ],
                      "ID1": [
                        "D1",
                        "D1",
                        "D1"
                      ],
                      "TARGET1": [
                        1.86,
                        3.24,
                        6.78
                      ]
                    }
                  ],
                  "input_data_points": 512,
                  "output_data_points": 1024
            }""";

        TimeSeriesService tsService = TimeSeriesService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("my-project-id")
            .modelId("ibm/granite-ttm-1536-96-r2")
            .build();

        InputSchema inputSchema = InputSchema.builder()
            .timestampColumn("date")
            .addIdColumn("ID1")
            .build();

        ForecastData data = ForecastData.create()
            .add("date", "2020-01-01T00:00:00")
            .add("date", "2020-01-01T01:00:00")
            .add("date", "2020-01-05T01:00:00")
            .addAll("ID1", Arrays.asList("D1", "D1", "D1"))
            .addAll("TARGET1", Arrays.asList(1.46, 2.34, 4.55));

        wireMock.stubFor(post("/ml/v1/time_series/forecast?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "my-project-id",
                    "model_id": "ibm/granite-ttm-1536-96-r2",
                    "schema": {
                        "timestamp_column": "date",
                        "id_columns": [ "ID1" ]
                    },
                    "data": {
                        "date": [
                            "2020-01-01T00:00:00",
                            "2020-01-01T01:00:00",
                            "2020-01-05T01:00:00"
                        ],
                        "ID1": [
                            "D1",
                            "D1",
                            "D1"
                        ],
                        "TARGET1": [
                            1.46,
                            2.34,
                            4.55
                        ]
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(EXPECTED)));

        var result = tsService.forecast(inputSchema, data);
        JSONAssert.assertEquals(EXPECTED, toJson(result), true);
    }

    @Test
    void should_forecast_with_parameters() {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var EXPECTED = """
            {
                  "model_id": "my-model-id",
                  "created_at": "2020-05-02T16:27:51Z",
                  "results": [
                    {
                      "date": [
                        "2020-01-05T02:00:00",
                        "2020-01-05T03:00:00",
                        "2020-01-06T00:00:00"
                      ],
                      "ID1": [
                        "D1",
                        "D1",
                        "D1"
                      ],
                      "TARGET1": [
                        1.86,
                        3.24,
                        6.78
                      ]
                    }
                  ],
                  "input_data_points": 512,
                  "output_data_points": 1024
            }""";

        var BODY = """
            {
                "project_id": "my-project-id",
                "model_id": "my-model-id",
                "space_id": "my-space-id",
                "schema": {
                    "timestamp_column": "date",
                    "id_columns": [ "ID1" ]
                },
                "data": {
                    "date": [
                        "2020-01-01T00:00:00",
                        "2020-01-01T01:00:00",
                        "2020-01-05T01:00:00"
                    ],
                    "ID1": [
                        "D1",
                        "D1",
                        "D1"
                    ],
                    "TARGET1": [
                        1.46,
                        2.34,
                        4.55
                    ]
                },
                "parameters": {
                    "prediction_length": 512
                }
            }""";

        TimeSeriesService tsService = TimeSeriesService.builder()
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("my-project-id")
            .modelId("ibm/granite-ttm-1536-96-r2")
            .build();

        InputSchema inputSchema = InputSchema.builder()
            .timestampColumn("date")
            .addIdColumn("ID1")
            .build();

        ForecastData data = ForecastData.create()
            .add("date", "2020-01-01T00:00:00")
            .add("date", "2020-01-01T01:00:00")
            .add("date", "2020-01-05T01:00:00")
            .add("ID1", "D1", 3)
            .addAll("TARGET1", 1.46, 2.34, 4.55);

        wireMock.stubFor(post("/ml/v1/time_series/forecast?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .withRequestBody(equalToJson(BODY))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(EXPECTED)));

        var parameters = TimeSeriesParameters.builder()
            .modelId("my-model-id")
            .projectId("my-project-id")
            .spaceId("my-space-id")
            .predictionLength(512)
            .transactionId("my-transaction-id")
            .build();

        var result = tsService.forecast(inputSchema, data, parameters);
        JSONAssert.assertEquals(EXPECTED, toJson(result), true);
        assertEquals(512, parameters.getPredictionLength());

        var dataFromJson = fromJson("""
            {
                "date": [
                    "2020-01-01T00:00:00",
                    "2020-01-01T01:00:00",
                    "2020-01-05T01:00:00"
                ],
                "ID1": [
                    "D1",
                    "D1",
                    "D1"
                ],
                "TARGET1": [
                    1.46,
                    2.34,
                    4.55
                ]
            }""", new TypeToken<Map<String, List<Object>>>() {});

        result = tsService.forecast(inputSchema, ForecastData.from(dataFromJson), parameters);
        JSONAssert.assertEquals(EXPECTED, toJson(result), true);
        assertTrue(data.containsKey("date"));
        assertEquals(List.of("2020-01-01T00:00:00", "2020-01-01T01:00:00", "2020-01-05T01:00:00"), data.get("date"));
        assertNotNull(data.toString());
    }

    @Test
    void should_build_correct_input_schema() {

        var EXPECTED = """
            {
                "timestamp_column": "date",
                "id_columns": [ "ID1", "ID2", "ID3" ],
                "target_columns": [ "TARGET1", "TARGET2", "TARGET3" ],
                "freq": "freq"
            }""";

        var inputSchema = InputSchema.builder()
            .timestampColumn("date")
            .idColumns("ID1", "ID2")
            .targetColumns("TARGET1", "TARGET2")
            .addIdColumn("ID3")
            .addTargetColumn("TARGET3")
            .freq("freq")
            .build();

        JSONAssert.assertEquals(EXPECTED, toJson(inputSchema), true);
    }

    @Test
    void should_serialize_time_series_parameters_correctly() {

        var parameters = TimeSeriesParameters.builder()
            .modelId("test")
            .build();

        var EXPECTED = """
            {
                "model_id": "test"
            }""";

        JSONAssert.assertEquals(EXPECTED, toJson(parameters), true);
        assertEquals(null, parameters.toParameters());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_throw_exception_for_invalid_input_or_http_errors() throws Exception {

        withWatsonxServiceMock(() -> {
            TimeSeriesService tsService = TimeSeriesService.builder()
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("my-project-id")
                .modelId("ibm/granite-ttm-1536-96-r2")
                .build();

            InputSchema inputSchema = InputSchema.builder()
                .timestampColumn("date")
                .addIdColumn("ID1")
                .build();

            ForecastData data = ForecastData.create()
                .add("date", "2020-01-01T00:00:00")
                .add("date", "2020-01-01T01:00:00")
                .add("date", "2020-01-05T01:00:00")
                .addAll("ID1", Arrays.asList("D1", "D1", "D1"))
                .addAll("TARGET1", Arrays.asList(1.46, 2.34, 4.55));

            var ex = assertThrows(NullPointerException.class, () -> tsService.forecast(null, data));
            assertEquals(ex.getMessage(), "InputSchema cannot be null");

            ex = assertThrows(NullPointerException.class, () -> tsService.forecast(inputSchema, null));
            assertEquals(ex.getMessage(), "Data cannot be null");

            try {
                when(mockHttpClient.send(any(), any(BodyHandler.class))).thenThrow(IOException.class);
                assertThrows(RuntimeException.class, () -> tsService.forecast(inputSchema, data));

                when(mockHttpClient.send(any(), any(BodyHandler.class))).thenThrow(InterruptedException.class);
                assertThrows(RuntimeException.class, () -> tsService.forecast(inputSchema, data));
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
