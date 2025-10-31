/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.tokenization.TokenizationRequest;
import com.ibm.watsonx.ai.tokenization.TokenizationResponse;
import com.ibm.watsonx.ai.tokenization.TokenizationRestClient;

public class CustomTokenizationRestClient extends TokenizationRestClient {

    CustomTokenizationRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public TokenizationResponse tokenize(String transactionId, TokenizationRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'tokenize'");
    }

    @Override
    public CompletableFuture<TokenizationResponse> asyncTokenize(String transactionId, TokenizationRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'asyncTokenize'");
    }

    public static final class CustomTokenizationRestClientBuilderFactory implements TokenizationRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomTokenizationRestClient.Builder();
        }
    }

    static final class Builder extends TokenizationRestClient.Builder {
        @Override
        public TokenizationRestClient build() {
            return new CustomTokenizationRestClient(this);
        }
    }
}
