/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.embedding.EmbeddingPayload;
import com.ibm.watsonx.ai.embedding.EmbeddingResponse;
import com.ibm.watsonx.ai.embedding.EmbeddingRestClient;

public class CustomEmbeddingRestClient extends EmbeddingRestClient {

    CustomEmbeddingRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public EmbeddingResponse embedding(String transactionId, EmbeddingPayload request) {
        throw new UnsupportedOperationException("Unimplemented method 'embedding'");
    }

    @Override
    public CompletableFuture<EmbeddingResponse> embeddingAsync(String transactionId, EmbeddingPayload embeddingPayload) {
        throw new UnsupportedOperationException("Unimplemented method 'embeddingAsync'");
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
