/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.detection;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.detection.DetectionTextRequest;
import com.ibm.watsonx.ai.detection.DetectionTextResponse;
import com.ibm.watsonx.ai.detection.detector.Hap;
import com.ibm.watsonx.ai.detection.detector.Pii;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        try {

            var url = URI.create(config.getValue("WATSONX_URL", String.class));
            var apiKey = config.getValue("WATSONX_API_KEY", String.class);
            var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

            DetectionService detectionService = DetectionService.builder()
                .apiKey(apiKey)
                .projectId(projectId)
                .timeout(Duration.ofSeconds(60))
                .baseUrl(url)
                .build();

            DetectionTextRequest request = DetectionTextRequest.builder()
                .input("I kill you with my phone number 1234567890")
                .detectors(Pii.create(), Hap.builder().threshold(0.3f).build())
                .build();

            List<DetectionTextResponse> response = detectionService.detect(request).detections();

            System.out.println(response);
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
