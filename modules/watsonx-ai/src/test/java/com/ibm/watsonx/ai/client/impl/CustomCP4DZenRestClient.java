/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DRestClient;
import com.ibm.watsonx.ai.core.auth.cp4d.TokenRequest;
import com.ibm.watsonx.ai.core.auth.cp4d.TokenResponse;

public class CustomCP4DZenRestClient extends CP4DRestClient {

    CustomCP4DZenRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public TokenResponse token(TokenRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'token'");
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(TokenRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'asyncToken'");
    }

    public static final class CustomCP4DZenRestClientBuilderFactory implements CP4DRestClient.CP4DZenRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomCP4DZenRestClient.Builder();
        }
    }

    static final class Builder extends CP4DRestClient.Builder<CustomCP4DZenRestClient, Builder> {
        @Override
        public CustomCP4DZenRestClient build() {
            return new CustomCP4DZenRestClient(this);
        }
    }
}
