/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.detection.DetectionContextRequest;
import com.ibm.watsonx.ai.detection.DetectionGeneratedRequest;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.detection.DetectionTextRequest;
import com.ibm.watsonx.ai.detection.detector.AnswerRelevance;
import com.ibm.watsonx.ai.detection.detector.ContextRelevance;
import com.ibm.watsonx.ai.detection.detector.Hap;
import com.ibm.watsonx.ai.detection.detector.Pii;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class DetectionServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");
    static final String GOVERNANCE_INSTANCE_ID = System.getenv("WATSONX_GOVERNANCE_INSTANCE_ID");

    static final Authenticator authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final DetectionService detectionService = DetectionService.builder()
        .baseUrl(URL)
        .projectId(PROJECT_ID)
        .governanceInstanceId(GOVERNANCE_INSTANCE_ID)
        .authenticator(authentication)
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_detect_hap() {
        var response = detectionService.detect(
            DetectionTextRequest.builder()
                .input("I kill you")
                .detectors(Pii.ofDefaults(), Hap.builder().threshold(0.3).build())
                .build()
        );

        assertNotNull(response.detections().get(0));
        assertEquals(0, response.detections().get(0).start());
        assertEquals(10, response.detections().get(0).end());
        assertEquals("I kill you", response.detections().get(0).text());
        assertEquals("hap", response.detections().get(0).detectionType());
        assertEquals("has_HAP", response.detections().get(0).detection());
        assertEquals(0.97, response.detections().get(0).score(), 0.01);
    }

    @Test
    void should_detect_pii() {
        var response = detectionService.detect(
            DetectionTextRequest.builder()
                .input("My name is George and my phone number is 1234567890")
                .detectors(Pii.ofDefaults(), Hap.builder().threshold(0.3).build())
                .build()
        );

        assertNotNull(response.detections().get(0));
        assertEquals(41, response.detections().get(0).start());
        assertEquals(51, response.detections().get(0).end());
        assertEquals("1234567890", response.detections().get(0).text());
        assertEquals("pii", response.detections().get(0).detectionType());
        assertEquals("PhoneNumber", response.detections().get(0).detection());
        assertEquals(0.8, response.detections().get(0).score(), 0.1);
    }

    @Test
    void should_detect_hap_and_pii() {
        var response = detectionService.detect(
            DetectionTextRequest.builder()
                .input("I kill you with my phone number 1234567890")
                .detectors(Pii.ofDefaults(), Hap.builder().threshold(0.3).build())
                .build()
        );

        assertNotNull(response.detections().get(0));
        assertEquals(0, response.detections().get(0).start());
        assertEquals(42, response.detections().get(0).end());
        assertEquals("I kill you with my phone number 1234567890", response.detections().get(0).text());
        assertEquals("hap", response.detections().get(0).detectionType());
        assertEquals("has_HAP", response.detections().get(0).detection());
        assertEquals(0.93, response.detections().get(0).score(), 0.01);

        assertNotNull(response.detections().get(1));
        assertEquals(32, response.detections().get(1).start());
        assertEquals(42, response.detections().get(1).end());
        assertEquals("1234567890", response.detections().get(1).text());
        assertEquals("pii", response.detections().get(1).detectionType());
        assertEquals("PhoneNumber", response.detections().get(1).detection());
        assertEquals(0.8, response.detections().get(1).score(), 0.1);
    }


    @Test
    void should_throw_when_context_relevance_detector_is_used_without_governance_instance_id() {

        DetectionService detectionService = DetectionService.builder()
            .baseUrl(URL)
            .projectId(PROJECT_ID)
            .authenticator(authentication)
            .logRequests(true)
            .logResponses(true)
            .build();

        var request = DetectionContextRequest.builder()
            .input("Test")
            .context(List.of("test"))
            .detectors(ContextRelevance.ofDefaults())
            .build();

        var ex = assertThrows(WatsonxException.class, () -> detectionService.detectContext(request));
        var error = ex.details().orElseThrow().errors().get(0);
        assertEquals("detector_not_accessible", error.code());
        assertEquals("Detectors 'context_relevance' is not accessible for authorization type 'project_id'", error.message());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WATSONX_GOVERNANCE_INSTANCE_ID", matches = ".+")
    void should_detect_context_relevance_with_url_context() {

        var request = DetectionContextRequest.builder()
            .input("Internation Business Machine")
            .context(List.of(
                "https://en.wikipedia.org/wiki/IBM",
                "https://research.ibm.com/"
            ))
            .detectors(ContextRelevance.ofDefaults())
            .build();

        var detections = detectionService.detectContext(request).detections();
        assertEquals(1, detections.size());
        assertEquals("context_relevance", detections.get(0).detectionType());
        assertEquals("relevant", detections.get(0).detection());
        assertNotNull(detections.get(0).detectorId());
        assertNotNull(detections.get(0).score());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "WATSONX_GOVERNANCE_INSTANCE_ID", matches = ".+")
    void should_detect_answer_relevance_for_generated_text() {

        var request = DetectionGeneratedRequest.builder()
            .prompt("What is IBM?")
            .generatedText("IBM is a technology company.")
            .detectors(AnswerRelevance.ofDefaults())
            .build();

        var detections = detectionService.detectGenerated(request).detections();
        assertEquals(1, detections.size());
        assertEquals("answer_relevance", detections.get(0).detectionType());
        assertEquals("relevant", detections.get(0).detection());
        assertNotNull(detections.get(0).detectorId());
        assertNotNull(detections.get(0).score());
    }
}
