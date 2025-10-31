/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.detection.DetectionResponse;
import com.ibm.watsonx.ai.detection.DetectionRestClient;
import com.ibm.watsonx.ai.detection.DetectionTextResponse;
import com.ibm.watsonx.ai.detection.TextDetectionContentDetectors;

public class CustomDetectionRestClient extends DetectionRestClient {

    CustomDetectionRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public DetectionResponse<DetectionTextResponse> detect(String transactionId, TextDetectionContentDetectors request) {
        throw new UnsupportedOperationException("Unimplemented method 'detect'");
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
