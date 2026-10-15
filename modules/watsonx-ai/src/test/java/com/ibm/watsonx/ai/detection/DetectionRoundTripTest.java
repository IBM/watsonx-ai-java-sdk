/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.detection.detector.AnswerRelevance;
import com.ibm.watsonx.ai.detection.detector.ContextRelevance;
import com.ibm.watsonx.ai.detection.detector.Faithfulness;
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
    void should_round_trip_all_detection_text_request_properties() {

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
        assertEquals(request, Json.fromJson(Json.toJson(request), DetectionTextRequest.class));
    }

    @Test
    void should_round_trip_detection_text_request_with_service_instance_id() {

        var EXPECTED = """
            {
                "input": "analyze this text",
                "detectors": { "pii": {} },
                "service_instance_id": "4165a2fb-3b57-4b19-8a2b-037b9b0f7b08"
            }""";

        var request = DetectionTextRequest.builder()
            .input("analyze this text")
            .detectors(Pii.ofDefaults())
            .serviceInstanceId("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08")
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(request), true);
        assertEquals(request, Json.fromJson(Json.toJson(request), DetectionTextRequest.class));
    }

    @Test
    void should_fail_to_deserialize_detection_text_request_on_conflicting_detectors_setters() {

        var ex = assertThrows(Exception.class, () -> Json.fromJson("{\"input\":\"x\",\"project_id\":\"p\"}", DetectionTextRequest.class));

        assertTrue(ex.getCause().getMessage().contains("detectors"));
    }

    @Test
    void should_deserialize_detection_context_response_with_nested_evidence() {

        var JSON = """
            {
                "detector_id": "granite_guardian_3_2_5b",
                "detection_type": "context_relevance",
                "detection": "relevant",
                "score": 0.87,
                "evidence": [
                    {
                        "name": "sentence_1",
                        "value": "IBM is a technology company.",
                        "score": 0.92,
                        "evidence": [
                            {
                                "name": "token_match",
                                "value": "IBM",
                                "score": 0.95,
                                "evidence": null
                            }
                        ]
                    }
                ]
            }""";

        var child = new DetectionContextResponse.Evidence("token_match", "IBM", 0.95, null);
        var parent = new DetectionContextResponse.Evidence("sentence_1", "IBM is a technology company.", 0.92, List.of(child));
        var EXPECTED = new DetectionContextResponse("granite_guardian_3_2_5b", "context_relevance", "relevant", 0.87, List.of(parent));

        var response = Json.fromJson(JSON, DetectionContextResponse.class);
        assertEquals(EXPECTED, response);
        assertEquals("granite_guardian_3_2_5b", response.detectorId());
    }

    @Test
    void should_serialize_text_detection_context_detectors_properties() {

        var EXPECTED = """
            {
                "input": "What is IBM?",
                "context_type": "docs",
                "context": ["https://en.wikipedia.org/wiki/IBM"],
                "detectors": {
                    "context_relevance": {}
                },
                "project_id": "project-123",
                "space_id": "space-456"
            }""";

        var carrier = new TextDetectionContextDetectors(
            "What is IBM?",
            List.of("https://en.wikipedia.org/wiki/IBM"),
            Map.of("context_relevance", Map.of()),
            "project-123",
            "space-456");

        JSONAssert.assertEquals(EXPECTED, Json.toJson(carrier), true);
    }

    @Test
    void should_round_trip_detection_context_request_via_builder() {

        var EXPECTED = """
            {
                "input": "What is IBM?",
                "context": ["https://en.wikipedia.org/wiki/IBM"],
                "detectors": {
                    "context_relevance": {},
                    "faithfulness": { "threshold": 0.6 }
                },
                "project_id": "project-123",
                "space_id": "space-456",
                "transaction_id": "txn-abc"
            }""";

        var request = DetectionContextRequest.builder()
            .input("What is IBM?")
            .context(List.of("https://en.wikipedia.org/wiki/IBM"))
            .detectors(ContextRelevance.ofDefaults(), Faithfulness.ofThreshold(0.6))
            .projectId("project-123")
            .spaceId("space-456")
            .transactionId("txn-abc")
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(request), true);
        assertEquals(request, Json.fromJson(Json.toJson(request), DetectionContextRequest.class));
    }

    @Test
    void should_round_trip_detection_context_request_with_service_instance_id() {

        var EXPECTED = """
            {
                "input": "What is IBM?",
                "context": ["https://en.wikipedia.org/wiki/IBM"],
                "detectors": { "context_relevance": {} },
                "service_instance_id": "4165a2fb-3b57-4b19-8a2b-037b9b0f7b08"
            }""";

        var request = DetectionContextRequest.builder()
            .input("What is IBM?")
            .context(List.of("https://en.wikipedia.org/wiki/IBM"))
            .detectors(ContextRelevance.ofDefaults())
            .serviceInstanceId("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08")
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(request), true);
        assertEquals(request, Json.fromJson(Json.toJson(request), DetectionContextRequest.class));
    }

    @Test
    void should_deserialize_detection_generated_response_from_json() {

        var JSON = """
            {
                "detector_id": "granite_guardian_3_2_5b",
                "detection_type": "answer_relevance",
                "detection": "relevant",
                "score": 0.91
            }""";

        var EXPECTED = new DetectionGeneratedResponse("granite_guardian_3_2_5b", "answer_relevance", "relevant", 0.91);

        var response = Json.fromJson(JSON, DetectionGeneratedResponse.class);
        assertEquals(EXPECTED, response);
        assertEquals("granite_guardian_3_2_5b", response.detectorId());
    }

    @Test
    void should_map_detection_type_snake_case_in_generated_response() {

        var JSON = """
            {
                "detection_type": "answer_relevance",
                "detection": "not_relevant",
                "score": 0.2
            }""";

        var response = Json.fromJson(JSON, DetectionGeneratedResponse.class);

        assertEquals("answer_relevance", response.detectionType());
        assertNull(response.text());
    }

    @Test
    void should_serialize_text_detection_generated_detectors_properties() {

        var EXPECTED = """
            {
                "prompt": "What is IBM?",
                "generated_text": "IBM is a company.",
                "detectors": {
                    "answer_relevance": { "threshold": 0.5 }
                },
                "project_id": "project-123",
                "space_id": "space-456"
            }""";

        var carrier = new TextDetectionGeneratedDetectors(
            "What is IBM?",
            "IBM is a company.",
            Map.of("answer_relevance", Map.of("threshold", 0.5)),
            "project-123",
            "space-456");

        JSONAssert.assertEquals(EXPECTED, Json.toJson(carrier), true);
    }

    @Test
    void should_round_trip_detection_generated_request_via_builder() {

        var EXPECTED = """
            {
                "prompt": "What is IBM?",
                "generated_text": "IBM is a company.",
                "detectors": {
                    "answer_relevance": {}
                },
                "project_id": "project-123",
                "space_id": "space-456",
                "transaction_id": "txn-xyz"
            }""";

        var request = DetectionGeneratedRequest.builder()
            .prompt("What is IBM?")
            .generatedText("IBM is a company.")
            .detectors(AnswerRelevance.ofDefaults())
            .projectId("project-123")
            .spaceId("space-456")
            .transactionId("txn-xyz")
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(request), true);
        assertEquals(request, Json.fromJson(Json.toJson(request), DetectionGeneratedRequest.class));
    }

    @Test
    void should_round_trip_detection_generated_request_with_service_instance_id() {

        var EXPECTED = """
            {
                "prompt": "What is IBM?",
                "generated_text": "IBM is a company.",
                "detectors": { "answer_relevance": {} },
                "service_instance_id": "4165a2fb-3b57-4b19-8a2b-037b9b0f7b08"
            }""";

        var request = DetectionGeneratedRequest.builder()
            .prompt("What is IBM?")
            .generatedText("IBM is a company.")
            .detectors(AnswerRelevance.ofDefaults())
            .serviceInstanceId("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08")
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(request), true);
        assertEquals(request, Json.fromJson(Json.toJson(request), DetectionGeneratedRequest.class));
    }
}
