/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageGenerationRequest;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageResponse;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageRestClient;

public class CustomModelGatewayImageRestClient extends ModelGatewayImageRestClient {

    CustomModelGatewayImageRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public ModelGatewayImageResponse generate(ModelGatewayImageGenerationRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'generate'");
    }

    public static final class CustomModelGatewayImageRestClientBuilderFactory
        implements ModelGatewayImageRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomModelGatewayImageRestClient.Builder();
        }
    }

    static final class Builder extends ModelGatewayImageRestClient.Builder {
        @Override
        public ModelGatewayImageRestClient build() {
            return new CustomModelGatewayImageRestClient(this);
        }
    }
}
