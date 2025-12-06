/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DRestClient;
import com.ibm.watsonx.ai.core.auth.cp4d.TokenRequest;

public class CustomCP4DRestClient extends CP4DRestClient {

    CustomCP4DRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public com.ibm.watsonx.ai.core.auth.cp4d.TokenResponse token(TokenRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'token'");
    }

    @Override
    public CompletableFuture<com.ibm.watsonx.ai.core.auth.cp4d.TokenResponse> asyncToken(TokenRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'asyncToken'");
    }

    public static final class CustomCP4DRestClientBuilderFactory implements CP4DRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomCP4DRestClient.Builder();
        }
    }

    static final class Builder extends CP4DRestClient.Builder<CustomCP4DRestClient, Builder> {
        @Override
        public CustomCP4DRestClient build() {
            return new CustomCP4DRestClient(this);
        }
    }
}
