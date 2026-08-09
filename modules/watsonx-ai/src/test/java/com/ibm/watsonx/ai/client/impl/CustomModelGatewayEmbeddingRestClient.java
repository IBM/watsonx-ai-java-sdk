/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingPayload;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingResponse;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingRestClient;

public class CustomModelGatewayEmbeddingRestClient extends ModelGatewayEmbeddingRestClient {

    CustomModelGatewayEmbeddingRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public ModelGatewayEmbeddingResponse embed(ModelGatewayEmbeddingPayload request) {
        throw new UnsupportedOperationException("Unimplemented method 'embed'");
    }

    public static final class CustomModelGatewayEmbeddingRestClientBuilderFactory
        implements ModelGatewayEmbeddingRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomModelGatewayEmbeddingRestClient.Builder();
        }
    }

    static final class Builder extends ModelGatewayEmbeddingRestClient.Builder {
        @Override
        public ModelGatewayEmbeddingRestClient build() {
            return new CustomModelGatewayEmbeddingRestClient(this);
        }
    }
}
