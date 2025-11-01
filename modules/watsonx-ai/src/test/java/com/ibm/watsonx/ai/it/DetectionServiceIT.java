/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.detection.DetectionTextRequest;
import com.ibm.watsonx.ai.detection.detector.Hap;
import com.ibm.watsonx.ai.detection.detector.Pii;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class DetectionServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final AuthenticationProvider authentication = IAMAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final DetectionService detectionService = DetectionService.builder()
        .baseUrl(URL)
        .projectId(PROJECT_ID)
        .authenticationProvider(authentication)
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_detect_hap() {
        var response = detectionService.detect(
            DetectionTextRequest.builder()
                .input("I kill you")
                .detectors(Pii.ofDefaults(), Hap.builder().threshold(0.3f).build())
                .build()
        );

        assertNotNull(response.detections().get(0));
        assertEquals(0, response.detections().get(0).getStart());
        assertEquals(10, response.detections().get(0).getEnd());
        assertEquals("I kill you", response.detections().get(0).getText());
        assertEquals("hap", response.detections().get(0).getDetectionType());
        assertEquals("has_HAP", response.detections().get(0).getDetection());
        assertEquals(0.97, response.detections().get(0).getScore(), 0.01);
    }

    @Test
    void should_detect_pii() {
        var response = detectionService.detect(
            DetectionTextRequest.builder()
                .input("My name is George and my phone number is 1234567890")
                .detectors(Pii.ofDefaults(), Hap.builder().threshold(0.3f).build())
                .build()
        );

        assertNotNull(response.detections().get(0));
        assertEquals(41, response.detections().get(0).getStart());
        assertEquals(51, response.detections().get(0).getEnd());
        assertEquals("1234567890", response.detections().get(0).getText());
        assertEquals("pii", response.detections().get(0).getDetectionType());
        assertEquals("PhoneNumber", response.detections().get(0).getDetection());
        assertEquals(0.8, response.detections().get(0).getScore(), 0.1);
    }

    @Test
    void should_detect_hap_and_pii() {
        var response = detectionService.detect(
            DetectionTextRequest.builder()
                .input("I kill you with my phone number 1234567890")
                .detectors(Pii.ofDefaults(), Hap.builder().threshold(0.3f).build())
                .build()
        );

        assertNotNull(response.detections().get(0));
        assertEquals(0, response.detections().get(0).getStart());
        assertEquals(42, response.detections().get(0).getEnd());
        assertEquals("I kill you with my phone number 1234567890", response.detections().get(0).getText());
        assertEquals("hap", response.detections().get(0).getDetectionType());
        assertEquals("has_HAP", response.detections().get(0).getDetection());
        assertEquals(0.93, response.detections().get(0).getScore(), 0.01);

        assertNotNull(response.detections().get(1));
        assertEquals(32, response.detections().get(1).getStart());
        assertEquals(42, response.detections().get(1).getEnd());
        assertEquals("1234567890", response.detections().get(1).getText());
        assertEquals("pii", response.detections().get(1).getDetectionType());
        assertEquals("PhoneNumber", response.detections().get(1).getDetection());
        assertEquals(0.8, response.detections().get(1).getScore(), 0.1);
    }
}
