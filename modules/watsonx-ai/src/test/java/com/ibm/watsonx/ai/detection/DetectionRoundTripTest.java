/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.detection.detector.Hap;
import com.ibm.watsonx.ai.detection.detector.Pii;

public class DetectionRoundTripTest {

    @Test
    void should_deserialize_detection_text_response_from_json() {

        var JSON = """
            {
                "text": "some text",
                "detection_type": "pii",
                "detection": "EmailAddress",
                "score": 0.987,
                "start": 5,
                "end": 10
            }""";

        var EXPECTED = new DetectionTextResponse("some text", "pii", "EmailAddress", 0.987, 5, 10);

        assertEquals(EXPECTED, Json.fromJson(JSON, DetectionTextResponse.class));
    }

    @Test
    void should_map_detection_type_snake_case_property() {

        var JSON = """
            {
                "text": "t",
                "detection_type": "hap",
                "detection": "Profanity",
                "score": 0.5,
                "start": 0,
                "end": 1
            }""";

        var response = Json.fromJson(JSON, DetectionTextResponse.class);

        assertEquals("hap", response.detectionType());
    }

    @Test
    void should_serialize_all_text_detection_content_detectors_properties() {

        var EXPECTED = """
            {
                "input": "analyze this text",
                "detectors": {
                    "pii": {},
                    "hap": { "threshold": 0.3 }
                },
                "project_id": "project-123",
                "space_id": "space-456"
            }""";

        var detectors = new TextDetectionContentDetectors(
            "analyze this text",
            Map.of("pii", Map.of(), "hap", Map.of("threshold", 0.3)),
            "project-123",
            "space-456");

        JSONAssert.assertEquals(EXPECTED, Json.toJson(detectors), true);
    }

    @Test
    void should_serialize_base_detection_request_properties_when_leaf_input_is_absent() {

        var EXPECTED = """
            {
                "detectors": { "pii": {} },
                "project_id": "project-123",
                "space_id": "space-456"
            }""";

        var detectors = new TextDetectionContentDetectors(null, Map.of("pii", Map.of()), "project-123", "space-456");

        JSONAssert.assertEquals(EXPECTED, Json.toJson(detectors), true);
    }

    @Test
    void should_serialize_all_detection_text_request_properties() {

        var EXPECTED = """
            {
                "input": "analyze this text",
                "detectors": {
                    "pii": {},
                    "hap": { "threshold": 0.3 }
                },
                "project_id": "project-123",
                "space_id": "space-456",
                "transaction_id": "txn-789"
            }""";

        var request = DetectionTextRequest.builder()
            .input("analyze this text")
            .detectors(Pii.ofDefaults(), Hap.ofThreshold(0.3))
            .projectId("project-123")
            .spaceId("space-456")
            .transactionId("txn-789")
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(request), true);
    }

    @Test
    void should_fail_to_deserialize_detection_text_request_on_conflicting_detectors_setters() {

        var ex = assertThrows(Exception.class, () -> Json.fromJson("{\"input\":\"x\",\"project_id\":\"p\"}", DetectionTextRequest.class));

        assertTrue(ex.getCause().getMessage().contains("detectors"));
    }
}
