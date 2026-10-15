/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.detection.DetectionContextResponse;
import com.ibm.watsonx.ai.detection.DetectionGeneratedResponse;
import com.ibm.watsonx.ai.detection.DetectionResponse;
import com.ibm.watsonx.ai.detection.DetectionRestClient;
import com.ibm.watsonx.ai.detection.DetectionTextResponse;
import com.ibm.watsonx.ai.detection.TextDetectionContentDetectors;
import com.ibm.watsonx.ai.detection.TextDetectionContextDetectors;
import com.ibm.watsonx.ai.detection.TextDetectionGeneratedDetectors;

public class CustomDetectionRestClient extends DetectionRestClient {

    CustomDetectionRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public DetectionResponse<DetectionTextResponse> detect(String transactionId, String serviceInstanceId, TextDetectionContentDetectors request) {
        throw new UnsupportedOperationException("Unimplemented method 'detect'");
    }

    @Override
    public DetectionResponse<DetectionContextResponse> detectContext(String transactionId, String serviceInstanceId,
        TextDetectionContextDetectors request) {
        throw new UnsupportedOperationException("Unimplemented method 'detectContext'");
    }

    @Override
    public DetectionResponse<DetectionGeneratedResponse> detectGenerated(String transactionId, String serviceInstanceId,
        TextDetectionGeneratedDetectors request) {
        throw new UnsupportedOperationException("Unimplemented method 'detectGenerated'");
    }

    public static final class CustomDetectionRestClientBuilderFactory implements DetectionRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomDetectionRestClient.Builder();
        }
    }

    static final class Builder extends DetectionRestClient.Builder {
        @Override
        public CustomDetectionRestClient build() {
            return new CustomDetectionRestClient(this);
        }
    }
}
