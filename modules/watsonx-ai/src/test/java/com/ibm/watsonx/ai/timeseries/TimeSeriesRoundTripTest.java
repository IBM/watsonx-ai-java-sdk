/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

public class TimeSeriesRoundTripTest {

    @Test
    @SuppressWarnings("unchecked")
    void should_snapshot_nested_forecast_results_and_round_trip_missing_values() throws Exception {
        var values = new ArrayList<>(Arrays.asList(1, null, 3));
        var result = new LinkedHashMap<String, Object>();
        result.put("target", values);
        var response = new ForecastResponse("model", "1", "2026-11-12", new ArrayList<>(List.of(result)), 3, 3);

        values.set(0, 99);
        result.put("other", "changed");

        var json = Json.toJson(response);
        JSONAssert.assertEquals("""
            {
                "model_id": "model", "model_version": "1", "created_at": "2026-11-12",
                "results": [{"target": [1, null, 3]}], "input_data_points": 3, "output_data_points": 3
            }
            """, json, true);
        assertEquals(response, Json.fromJson(json, ForecastResponse.class));
        assertThrows(UnsupportedOperationException.class, () -> response.results().get(0).put("other", "changed"));
        assertThrows(UnsupportedOperationException.class, () -> ((List<Object>) response.results().get(0).get("target")).add(4));
    }

    @Test
    void should_round_trip_forecast_request_with_historical_and_future_missing_values() throws Exception {
        var historical = ForecastData.create().addAll("target", 1, null, 3);
        var future = ForecastData.create().addAll("target", 4, null);
        var schema = InputSchema.builder().timestampColumn("date").targetColumns("target").build();
        var request = new ForecastRequest("model", null, "project", historical.asMap(), schema, future.asMap(), new ForecastRequest.Parameters(2));

        var json = Json.toJson(request);
        JSONAssert.assertEquals("""
            {
                "model_id": "model",
                "project_id": "project",
                "data": {"target": [1, null, 3]},
                "schema": {"timestamp_column": "date", "target_columns": ["target"]},
                "future_data": {"target": [4, null]},
                "parameters": {"prediction_length": 2}
            }
            """, json, true);

        var roundTrip = Json.fromJson(json, ForecastRequest.class);
        assertEquals(request, roundTrip);
    }

    @Test
    void should_round_trip_forecast_columns_with_missing_values() throws Exception {
        var data = ForecastData.create()
            .add("target", 1)
            .add("target", null)
            .add("target", 3)
            .add("date", "2026-11-12");

        var json = Json.toJson(data.asMap());
        JSONAssert.assertEquals("""
            {"target": [1, null, 3], "date": ["2026-11-12"]}
            """, json, true);

        var roundTrip = ForecastData.from(Json.fromJson(json, new TypeToken<Map<String, List<Object>>>() {}));
        assertEquals(data, roundTrip);
    }

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

    @Test
    void should_copy_columns_in_both_directions() {
        var values = new ArrayList<Object>(List.of(1));
        var source = new LinkedHashMap<String, List<Object>>();
        source.put("target", values);
        var data = ForecastData.from(source);

        values.add(2);
        source.clear();
        assertEquals(List.of(1), data.get("target"));

        data.add("target", 3);
        assertEquals(List.of(1, 2), values);
        assertEquals(List.of(1, 3), data.get("target"));
    }

    @Test
    void should_append_to_columns_copied_from_immutable_lists() {
        var data = ForecastData.from(Map.of("target", List.of(1)));

        data.add("target", 2).add("target", 3, 2).addAll("target", List.of(4));

        assertEquals(List.of(1, 2, 3, 3, 4), data.get("target"));
    }

    @Test
    void should_accept_missing_values_in_varargs_without_retaining_the_array() {
        Object[] values = { 1, null, 3 };
        var data = ForecastData.create().addAll("target", values);

        values[0] = 9;
        data.addAll("target", (Object) null);

        assertEquals(Arrays.asList(1, null, 3, null), data.get("target"));
    }

    @Test
    void should_protect_columns_through_all_accessors() {
        var data = ForecastData.create().add("target", 1);
        var view = data.asMap();

        assertThrows(UnsupportedOperationException.class, () -> data.get("target").clear());
        assertThrows(UnsupportedOperationException.class, () -> view.get("target").clear());
        assertThrows(UnsupportedOperationException.class, () -> view.values().iterator().next().clear());
        assertThrows(UnsupportedOperationException.class, () -> view.entrySet().iterator().next().getValue().clear());
        assertThrows(UnsupportedOperationException.class, () -> view.entrySet().iterator().next().setValue(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> view.put("other", List.of()));
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertEquals(List.of(1), data.get("target"));
    }

    @Test
    void should_keep_read_only_views_live_and_preserve_column_order() {
        var data = ForecastData.create().add("target", 1);
        var view = data.asMap();
        var entries = view.entrySet();
        var column = view.get("target");

        data.add("target", 2).add("date", "2026-11-12");

        assertEquals(List.of(1, 2), column);
        assertEquals(2, entries.size());
        assertEquals(List.of("target", "date"), new ArrayList<>(view.keySet()));
        assertEquals(List.of("2026-11-12"), view.get("date"));
    }

    @Test
    void should_preserve_null_columns_and_missing_values() {
        var source = new LinkedHashMap<String, List<Object>>();
        source.put("target", Arrays.asList(1, null, 3));
        source.put("empty", null);
        var data = ForecastData.from(source);

        assertEquals(Arrays.asList(1, null, 3), data.get("target"));
        assertNull(data.get("missing"));
        assertNull(data.asMap().get("empty"));
        assertTrue(data.asMap().containsKey("empty"));
        assertTrue(ForecastData.from(null).asMap().isEmpty());

        data.add("empty", null);
        assertEquals(Arrays.asList((Object) null), data.get("empty"));
    }

    @Test
    void should_snapshot_nested_collection_values_in_columns() {
        var values = new ArrayList<>(List.of("original"));
        var cell = new LinkedHashMap<String, Object>();
        cell.put("values", values);
        var columns = Map.of("target", List.<Object>of(cell));
        var request = new ForecastRequest(null, null, null, columns, null, columns, null);
        values.clear();
        cell.clear();

        for (var data : List.of(request.data(), request.futureData())) {
            var snapshot = (Map<?, ?>) data.get("target").get(0);
            assertEquals(Map.of("values", List.of("original")), snapshot);
            assertThrows(UnsupportedOperationException.class, snapshot::clear);
            assertThrows(UnsupportedOperationException.class, () -> ((List<?>) snapshot.get("values")).clear());
        }
    }

    @Test
    void should_copy_historical_and_future_columns() {
        var historicalValues = new ArrayList<Object>(Arrays.asList(1, null, 3));
        var futureValues = new ArrayList<Object>(Arrays.asList(4, null));
        var historical = new LinkedHashMap<String, List<Object>>();
        var future = new LinkedHashMap<String, List<Object>>();
        historical.put("target", historicalValues);
        future.put("target", futureValues);
        var request = new ForecastRequest(null, null, null, historical, null, future, null);

        historicalValues.clear();
        futureValues.clear();
        historical.clear();
        future.clear();

        assertEquals(Arrays.asList(1, null, 3), request.data().get("target"));
        assertEquals(Arrays.asList(4, null), request.futureData().get("target"));
    }

    @Test
    void should_snapshot_live_forecast_data_views() {
        var historical = ForecastData.create().addAll("target", 1, null, 3);
        var future = ForecastData.create().addAll("target", 4, null);
        var request = new ForecastRequest(null, null, null, historical.asMap(), null, future.asMap(), null);

        historical.add("target", 9).add("other", 10);
        future.add("target", 9).add("other", 10);

        assertEquals(Arrays.asList(1, null, 3), request.data().get("target"));
        assertEquals(Arrays.asList(4, null), request.futureData().get("target"));
        assertFalse(request.data().containsKey("other"));
        assertFalse(request.futureData().containsKey("other"));
    }

    @Test
    void should_expose_read_only_maps_and_columns() {
        var columns = Map.of("target", List.<Object>of(1));
        var request = new ForecastRequest(null, null, null, columns, null, columns, null);

        for (var view : List.of(request.data(), request.futureData())) {
            assertThrows(UnsupportedOperationException.class, () -> view.put("other", List.of()));
            assertThrows(UnsupportedOperationException.class, () -> view.get("target").clear());
            assertThrows(UnsupportedOperationException.class, () -> view.values().iterator().next().clear());
            assertThrows(UnsupportedOperationException.class, () -> view.entrySet().iterator().next().setValue(List.of()));
        }
    }

    @Test
    void should_preserve_column_order_and_null_columns() {
        var columns = new LinkedHashMap<String, List<Object>>();
        columns.put("target", Arrays.asList(1, null));
        columns.put("date", List.of("2026-11-12"));
        columns.put("empty", null);
        var request = new ForecastRequest(null, null, null, columns, null, columns, null);

        for (var view : List.of(request.data(), request.futureData())) {
            assertEquals(List.of("target", "date", "empty"), new ArrayList<>(view.keySet()));
            assertTrue(view.containsKey("empty"));
            assertNull(view.get("empty"));
            assertEquals(Arrays.asList(1, null), view.get("target"));
        }
    }

    @Test
    void should_preserve_null_and_empty_maps() {
        var absent = new ForecastRequest(null, null, null, null, null, null, null);
        assertNull(absent.data());
        assertNull(absent.futureData());

        var empty = new ForecastRequest(null, null, null, Map.of(), null, Map.of(), null);
        assertTrue(empty.data().isEmpty());
        assertTrue(empty.futureData().isEmpty());
    }

}
