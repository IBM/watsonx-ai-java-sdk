/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.detection.DetectionTextRequest;
import com.ibm.watsonx.ai.detection.DetectionTextResponse;
import com.ibm.watsonx.ai.detection.detector.GraniteGuardian;
import com.ibm.watsonx.ai.detection.detector.Hap;
import com.ibm.watsonx.ai.detection.detector.Pii;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
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

        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

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
                    GraniteGuardian.builder().threshold(0.2).addProperty("val", "test").build())
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
                                "threshold": 0.2,
                                "val": "test"
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

        when(mockHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));

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
}
