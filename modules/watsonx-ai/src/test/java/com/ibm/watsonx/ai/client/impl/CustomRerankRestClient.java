/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import com.ibm.watsonx.ai.rerank.RerankRequest;
import com.ibm.watsonx.ai.rerank.RerankResponse;
import com.ibm.watsonx.ai.rerank.RerankRestClient;

public class CustomRerankRestClient extends RerankRestClient {

    CustomRerankRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public RerankResponse rerank(String transactionId, RerankRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'rerank'");
    }

    public static final class CustomRerankRestClientBuilderFactory implements RerankRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomRerankRestClient.Builder();
        }
    }

    static final class Builder extends RerankRestClient.Builder {
        @Override
        public RerankRestClient build() {
            return new CustomRerankRestClient(this);
        }
    }
}
