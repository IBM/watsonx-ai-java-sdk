/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;

@DisabledInNativeImage
public class TimeSeriesRoundTripTest {

    @Test
    void should_round_trip_time_series_parameters_with_mapped_fields_only() throws Exception {

        var EXPECTED = """
            {
                "model_id": "ibm/granite-ttm-512-96-r2",
                "prediction_length": 12
            }""";

        var parameters = TimeSeriesParameters.builder()
            .modelId("ibm/granite-ttm-512-96-r2")
            .predictionLength(12)
            .build();

        var json = Json.toJson(parameters);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTrip = Json.fromJson(json, TimeSeriesParameters.class);
        assertEquals(parameters, roundTrip);
    }

    @Test
    void should_not_serialize_project_space_and_transaction_id() throws Exception {

        var EXPECTED = """
            {
                "model_id": "ibm/granite-ttm-512-96-r2",
                "prediction_length": 12
            }""";

        var parameters = TimeSeriesParameters.builder()
            .projectId("p1")
            .spaceId("s1")
            .transactionId("t1")
            .modelId("ibm/granite-ttm-512-96-r2")
            .predictionLength(12)
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(parameters), true);
    }

    @Test
    void should_round_trip_input_schema() throws Exception {

        var EXPECTED = """
            {
                "timestamp_column": "date",
                "id_columns": ["ID1", "ID2"],
                "freq": "D",
                "target_columns": ["sales"]
            }""";

        var schema = InputSchema.builder()
            .timestampColumn("date")
            .idColumns("ID1", "ID2")
            .freq("D")
            .targetColumns("sales")
            .build();

        var json = Json.toJson(schema);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTrip = Json.fromJson(json, InputSchema.class);
        assertEquals(schema, roundTrip);
    }
}
