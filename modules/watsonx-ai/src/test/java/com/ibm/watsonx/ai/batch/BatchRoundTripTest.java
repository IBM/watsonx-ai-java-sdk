/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;

public class BatchRoundTripTest {

    @Test
    void should_round_trip_batch_create_request_with_mapped_fields_only() throws Exception {

        var EXPECTED = """
            {
                "input_file_id": "file-abc123",
                "endpoint": "/v1/chat/completions",
                "completion_window": "24h",
                "metadata": {
                    "batch_name": "nightly-run"
                }
            }""";

        var request = BatchCreateRequest.builder()
            .inputFileId("file-abc123")
            .endpoint("/v1/chat/completions")
            .completionWindow("24h")
            .metadata(Map.of("batch_name", "nightly-run"))
            .build();

        var json = Json.toJson(request);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTrip = Json.fromJson(json, BatchCreateRequest.class);
        assertEquals(request, roundTrip);
    }

    @Test
    void should_not_serialize_project_space_transaction_id_or_unmapped_fields() throws Exception {

        var EXPECTED = """
            {
                "input_file_id": "file-abc123",
                "endpoint": "/v1/chat/completions",
                "completion_window": "24h"
            }""";

        var request = BatchCreateRequest.builder()
            .projectId("p1")
            .spaceId("s1")
            .transactionId("t1")
            .inputFileId("file-abc123")
            .endpoint("/v1/chat/completions")
            .completionWindow("24h")
            .removeUploadedFile(true)
            .removeOutputFile(true)
            .timeout(Duration.ofMinutes(30))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(request), true);
    }
}
