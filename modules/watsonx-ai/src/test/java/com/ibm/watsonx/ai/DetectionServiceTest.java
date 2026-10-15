/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.ibm.watsonx.ai.detection.DetectionRestClient.GOVERNANCE_INSTANCE_ID_HEADER;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.detection.DetectionContextRequest;
import com.ibm.watsonx.ai.detection.DetectionContextResponse;
import com.ibm.watsonx.ai.detection.DetectionGeneratedRequest;
import com.ibm.watsonx.ai.detection.DetectionGeneratedResponse;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.detection.DetectionTextRequest;
import com.ibm.watsonx.ai.detection.DetectionTextResponse;
import com.ibm.watsonx.ai.detection.detector.AnswerRelevance;
import com.ibm.watsonx.ai.detection.detector.ContextRelevance;
import com.ibm.watsonx.ai.detection.detector.Faithfulness;
import com.ibm.watsonx.ai.detection.detector.GraniteGuardian;
import com.ibm.watsonx.ai.detection.detector.Hap;
import com.ibm.watsonx.ai.detection.detector.Pii;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisabledInNativeImage
public class DetectionServiceTest extends AbstractWatsonxTest {

    @Test
    void should_build_request_with_correct_parameters() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            {
                "detections": [{
                    "start": 20,
                    "end": 24,
                    "detection_type": "pii",
                    "detection": "xxxx",
                    "score": 0.846
                }]
            }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.FRANKFURT)
                .projectId("project-id")
                .spaceId("space-id")
                .build();

            DetectionTextRequest request = DetectionTextRequest.builder()
                .input("input")
                .detectors(Pii.builder().build())
                .build();

            List<DetectionTextResponse> response = service.detect(request).detections();
            assertEquals(1, response.size());

            DetectionTextResponse detection = response.get(0);
            assertEquals(20, detection.start());
            assertEquals(24, detection.end());
            assertEquals("pii", detection.detectionType());
            assertEquals("xxxx", detection.detection());
            assertEquals(0.846, detection.score(), 0.001);

            JSONAssert.assertEquals(
                """
                    {
                        "input": "input",
                        "project_id": "project-id",
                        "space_id": "space-id",
                        "detectors": {
                            "pii": {}
                        }
                    }
                    """,
                bodyPublisherToString(mockHttpRequest),
                true);

            request = DetectionTextRequest.builder()
                .input("input")
                .projectId("new-project-id")
                .spaceId("new-space-id")
                .detectors(
                    Pii.builder().build(),
                    Hap.builder().threshold(0.1).build(),
                    GraniteGuardian.builder().threshold(0.2).build())
                .build();

            service.detect(request);

            JSONAssert.assertEquals(
                """
                    {
                        "input": "input",
                        "project_id": "new-project-id",
                        "space_id": "new-space-id",
                        "detectors": {
                            "pii": {},
                            "hap": {
                                "threshold": 0.1
                            },
                            "granite_guardian": {
                                "threshold": 0.2
                            }
                        }
                    }
                    """,
                bodyPublisherToString(mockHttpRequest),
                true);

            request = DetectionTextRequest.builder()
                .input("input")
                .spaceId("new-space-id")
                .transactionId("transaction-id")
                .detectors(Pii.ofDefaults(), Hap.ofDefaults(), GraniteGuardian.ofDefaults())
                .build();

            service.detect(request);

            assertEquals("transaction-id", mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "input": "input",
                        "space_id": "new-space-id",
                        "detectors": {
                            "pii": {},
                            "hap": {},
                            "granite_guardian": {},
                        }
                    }
                    """,
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }

    @Test
    void should_detect_pii_and_hap_entities_request() {

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.scheme()).thenReturn("Bearer");

        wireMock.stubFor(post("/ml/v1/text/detection?version=%s".formatted(API_VERSION))
            .withRequestBody(equalToJson("""
                {
                  "input": "input",
                  "project_id": "project-id",
                  "detectors": {
                    "pii": {},
                    "hap": {
                        "threshold": 0.4
                    },
                    "granite_guardian": {
                        "threshold": 2.4567
                    }
                  }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(TRANSACTION_ID_HEADER, "transaction-id")
                .withBody("""
                    {
                      "detections": [
                        {
                          "start": 20,
                          "end": 24,
                          "text": "text",
                          "detection_type": "pii",
                          "detection": "xxxx",
                          "score": 0.846
                        },
                        {
                          "start": 122,
                          "end": 239,
                          "text": "text",
                          "detection_type": "hap",
                          "detection": "xxxxxxxxxxxxxxxxxxxxxxxxxx",
                          "score": 0.846
                        }
                      ]
                    }""")));


        DetectionService service = DetectionService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl("http://localhost:%s".formatted(wireMock.getPort()))
            .projectId("project-id")
            .build();

        DetectionTextRequest request = DetectionTextRequest.builder()
            .input("input")
            .transactionId("transaction-id")
            .detectors(
                Pii.builder().build(),
                Hap.builder().threshold(0.4).build(),
                GraniteGuardian.builder().threshold(2.4567).build()
            )
            .build();

        List<DetectionTextResponse> response = service.detect(request).detections();
        assertEquals(2, response.size());

        DetectionTextResponse detection = response.get(0);
        assertEquals(20, detection.start());
        assertEquals(24, detection.end());
        assertEquals("text", detection.text());
        assertEquals("pii", detection.detectionType());
        assertEquals("xxxx", detection.detection());
        assertEquals(0.846, detection.score(), 0.001);

        detection = response.get(1);
        assertEquals(122, detection.start());
        assertEquals(239, detection.end());
        assertEquals("text", detection.text());
        assertEquals("hap", detection.detectionType());
        assertEquals("xxxxxxxxxxxxxxxxxxxxxxxxxx", detection.detection());
        assertEquals(0.846, detection.score(), 0.001);
        assertTrue(detection.toString().contains("hap"));
    }

    @Test
    void should_catch_io_exception() throws Exception {

        when(mockSecureHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));

        withWatsonxServiceMock(() -> {
            DetectionService service = DetectionService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .build();

            DetectionTextRequest request = DetectionTextRequest.builder()
                .input("input")
                .detectors(Pii.builder().build())
                .build();

            assertThrows(RuntimeException.class, () -> service.detect(request), "IOException");
        });
    }

    @Test
    void should_catch_watsonx_exception() throws Exception {

        var EXCEPTION = """
            {
                "errors": [
                    {
                        "code": "invalid_request_entity",
                        "message": "Invalid request: `detectors` is a required field and must contain at least one supported detector",
                        "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-detection-content"
                    }
                ],
                "trace": "004ca103d58aee42ec64f3d7a428b698",
                "status_code": 400
            }""";

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.scheme()).thenReturn("Bearer");

        wireMock.stubFor(post("/ml/v1/text/detection?version=%s".formatted(API_VERSION))
            .withRequestBody(equalToJson("""
                {
                  "input": "input",
                  "project_id": "project-id",
                  "detectors": {}
                }"""))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader(TRANSACTION_ID_HEADER, "transaction-id")
                .withHeader("Content-Type", "application/json")
                .withBody(EXCEPTION)));

        DetectionService service = DetectionService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl("http://localhost:%s".formatted(wireMock.getPort()))
            .projectId("project-id")
            .build();

        DetectionTextRequest request = DetectionTextRequest.builder()
            .input("input")
            .transactionId("transaction-id")
            .detectors(List.of())
            .build();

        var ex = assertThrows(WatsonxException.class, () -> service.detect(request));
        assertEquals(400, ex.statusCode());
        assertEquals(
            "Invalid request: `detectors` is a required field and must contain at least one supported detector",
            ex.details().orElse(null).errors().get(0).message());
    }

    @Test
    void should_detect_context_relevance_and_faithfulness() {

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.scheme()).thenReturn("Bearer");

        wireMock.stubFor(post(urlPathMatching("/ml/v1/text/detection/context"))
            .withRequestBody(equalToJson("""
                {
                  "input": "What is IBM?",
                  "context_type": "docs",
                  "context": ["https://en.wikipedia.org/wiki/IBM"],
                  "project_id": "project-id",
                  "detectors": {
                    "context_relevance": {},
                    "faithfulness": { "threshold": 0.5 }
                  }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "detections": [
                        {
                          "detector_id": "context_relevance_125m_v1_en",
                          "detection_type": "context_relevance",
                          "detection": "relevant",
                          "score": 0.87,
                          "evidence": [
                            {
                              "name": "sentence_1",
                              "value": "IBM is a technology company.",
                              "score": 0.92,
                              "evidence": []
                            }
                          ]
                        }
                      ]
                    }""")));

        DetectionService service = DetectionService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl("http://localhost:%s".formatted(wireMock.getPort()))
            .projectId("project-id")
            .build();

        DetectionContextRequest request = DetectionContextRequest.builder()
            .input("What is IBM?")
            .context(List.of("https://en.wikipedia.org/wiki/IBM"))
            .detectors(ContextRelevance.ofDefaults(), Faithfulness.ofThreshold(0.5))
            .build();

        List<DetectionContextResponse> detections = service.detectContext(request).detections();
        assertEquals(1, detections.size());

        DetectionContextResponse detection = detections.get(0);
        assertEquals("context_relevance_125m_v1_en", detection.detectorId());
        assertEquals("context_relevance", detection.detectionType());
        assertEquals("relevant", detection.detection());
        assertEquals(0.87, detection.score(), 0.001);
        assertEquals(1, detection.evidence().size());
        assertEquals("sentence_1", detection.evidence().get(0).name());
        assertEquals("IBM is a technology company.", detection.evidence().get(0).value());
        assertEquals(0.92, detection.evidence().get(0).score(), 0.001);
        assertTrue(detection.toString().contains("context_relevance"));
    }

    @Test
    void should_detect_answer_relevance_in_generated_text() {

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.scheme()).thenReturn("Bearer");

        wireMock.stubFor(post(urlPathMatching("/ml/v1/text/detection/generated"))
            .withRequestBody(equalToJson("""
                {
                  "prompt": "What is IBM?",
                  "generated_text": "IBM is a technology company.",
                  "project_id": "project-id",
                  "detectors": {
                    "answer_relevance": {}
                  }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "detections": [
                        {
                          "detector_id": "answer_relevance_125m_v1_en",
                          "detection_type": "answer_relevance",
                          "detection": "relevant",
                          "score": 0.91
                        }
                      ]
                    }""")));

        DetectionService service = DetectionService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl("http://localhost:%s".formatted(wireMock.getPort()))
            .projectId("project-id")
            .build();

        DetectionGeneratedRequest request = DetectionGeneratedRequest.builder()
            .prompt("What is IBM?")
            .generatedText("IBM is a technology company.")
            .detectors(AnswerRelevance.ofDefaults())
            .build();

        List<DetectionGeneratedResponse> detections = service.detectGenerated(request).detections();
        assertEquals(1, detections.size());

        DetectionGeneratedResponse detection = detections.get(0);
        assertEquals("answer_relevance_125m_v1_en", detection.detectorId());
        assertEquals("answer_relevance", detection.detectionType());
        assertEquals("relevant", detection.detection());
        assertEquals(0.91, detection.score(), 0.001);
        assertTrue(detection.toString().contains("answer_relevance"));
    }

    @Test
    void should_pass_transaction_id_header_for_detect_context() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .build();

            DetectionContextRequest request = DetectionContextRequest.builder()
                .input("test input")
                .context(List.of("https://example.com"))
                .transactionId("ctx-txn-id")
                .detectors(ContextRelevance.ofDefaults())
                .build();

            service.detectContext(request);

            assertEquals("ctx-txn-id", mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "input": "test input",
                        "context_type": "docs",
                        "context": ["https://example.com"],
                        "project_id": "project-id",
                        "detectors": { "context_relevance": {} }
                    }""",
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }

    @Test
    void should_pass_transaction_id_header_for_detect_generated() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .build();

            DetectionGeneratedRequest request = DetectionGeneratedRequest.builder()
                .prompt("prompt text")
                .generatedText("generated text")
                .transactionId("gen-txn-id")
                .detectors(AnswerRelevance.ofDefaults())
                .build();

            service.detectGenerated(request);

            assertEquals("gen-txn-id", mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "prompt": "prompt text",
                        "generated_text": "generated text",
                        "project_id": "project-id",
                        "detectors": { "answer_relevance": {} }
                    }""",
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }

    @Test
    void should_use_default_service_instance_id_configured_on_the_service() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .governanceInstanceId("default-instance-id")
                .build();

            DetectionTextRequest request = DetectionTextRequest.builder()
                .input("my text to check")
                .detectors(Pii.ofDefaults())
                .build();

            service.detect(request);

            assertEquals("default-instance-id", mockHttpRequest.getValue().headers().firstValue(GOVERNANCE_INSTANCE_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "input": "my text to check",
                        "detectors": { "pii": {} }
                    }""",
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }

    @Test
    void should_override_default_service_instance_id_with_request_value() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .governanceInstanceId("default-instance-id")
                .build();

            DetectionTextRequest request = DetectionTextRequest.builder()
                .input("my text to check")
                .serviceInstanceId("request-instance-id")
                .detectors(Pii.ofDefaults())
                .build();

            service.detect(request);

            assertEquals("request-instance-id", mockHttpRequest.getValue().headers().firstValue(GOVERNANCE_INSTANCE_ID_HEADER).orElse(null));
        });
    }

    @Test
    void should_use_default_service_instance_id_configured_on_the_service_for_detect_context() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .governanceInstanceId("default-instance-id")
                .build();

            DetectionContextRequest request = DetectionContextRequest.builder()
                .input("test input")
                .context(List.of("https://example.com"))
                .detectors(ContextRelevance.ofDefaults())
                .build();

            service.detectContext(request);

            assertEquals("default-instance-id", mockHttpRequest.getValue().headers().firstValue(GOVERNANCE_INSTANCE_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "input": "test input",
                        "context_type": "docs",
                        "context": ["https://example.com"],
                        "detectors": { "context_relevance": {} }
                    }""",
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }

    @Test
    void should_use_default_service_instance_id_configured_on_the_service_for_detect_generated() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .governanceInstanceId("default-instance-id")
                .build();

            DetectionGeneratedRequest request = DetectionGeneratedRequest.builder()
                .prompt("prompt text")
                .generatedText("generated text")
                .detectors(AnswerRelevance.ofDefaults())
                .build();

            service.detectGenerated(request);

            assertEquals("default-instance-id", mockHttpRequest.getValue().headers().firstValue(GOVERNANCE_INSTANCE_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "prompt": "prompt text",
                        "generated_text": "generated text",
                        "detectors": { "answer_relevance": {} }
                    }""",
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }

    @Test
    void should_use_service_instance_id_header_instead_of_project_id_for_detect() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .build();

            DetectionTextRequest request = DetectionTextRequest.builder()
                .input("my text to check")
                .serviceInstanceId("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08")
                .detectors(Pii.ofDefaults())
                .build();

            service.detect(request);

            assertEquals("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08",
                mockHttpRequest.getValue().headers().firstValue(GOVERNANCE_INSTANCE_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "input": "my text to check",
                        "detectors": { "pii": {} }
                    }""",
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }

    @Test
    void should_use_service_instance_id_header_instead_of_project_id_for_detect_context() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .build();

            DetectionContextRequest request = DetectionContextRequest.builder()
                .input("test input")
                .context(List.of("https://example.com"))
                .serviceInstanceId("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08")
                .detectors(ContextRelevance.ofDefaults())
                .build();

            service.detectContext(request);

            assertEquals("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08",
                mockHttpRequest.getValue().headers().firstValue(GOVERNANCE_INSTANCE_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "input": "test input",
                        "context_type": "docs",
                        "context": ["https://example.com"],
                        "detectors": { "context_relevance": {} }
                    }""",
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }

    @Test
    void should_use_service_instance_id_header_instead_of_project_id_for_detect_generated() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            { "detections": [] }""");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            DetectionService service = DetectionService.builder()
                .apiKey("api-key")
                .baseUrl(CloudRegion.DALLAS)
                .projectId("project-id")
                .build();

            DetectionGeneratedRequest request = DetectionGeneratedRequest.builder()
                .prompt("prompt text")
                .generatedText("generated text")
                .serviceInstanceId("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08")
                .detectors(AnswerRelevance.ofDefaults())
                .build();

            service.detectGenerated(request);

            assertEquals("4165a2fb-3b57-4b19-8a2b-037b9b0f7b08",
                mockHttpRequest.getValue().headers().firstValue(GOVERNANCE_INSTANCE_ID_HEADER).orElse(null));

            JSONAssert.assertEquals(
                """
                    {
                        "prompt": "prompt text",
                        "generated_text": "generated text",
                        "detectors": { "answer_relevance": {} }
                    }""",
                bodyPublisherToString(mockHttpRequest),
                true);
        });
    }
}
