/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.embedding.EmbeddingRequest;
import com.ibm.watsonx.ai.embedding.EmbeddingResponse;
import com.ibm.watsonx.ai.embedding.EmbeddingRestClient;

public class CustomEmbeddingRestClient extends EmbeddingRestClient {

    CustomEmbeddingRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public EmbeddingResponse embedding(String transactionId, EmbeddingRequest embeddingRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'embedding'");
    }

    public static final class CustomEmbeddingRestClientBuilderFactory implements EmbeddingRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomEmbeddingRestClient.Builder();
        }
    }

    static final class Builder extends EmbeddingRestClient.Builder {
        @Override
        public EmbeddingRestClient build() {
            return new CustomEmbeddingRestClient(this);
        }
    }
}
