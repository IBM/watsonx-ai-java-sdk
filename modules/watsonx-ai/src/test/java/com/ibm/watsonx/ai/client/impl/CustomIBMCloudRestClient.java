/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudRestClient;
import com.ibm.watsonx.ai.core.auth.ibmcloud.TokenResponse;

public class CustomIBMCloudRestClient extends IBMCloudRestClient {

    CustomIBMCloudRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public TokenResponse token(String apiKey, String grantType) {
        throw new UnsupportedOperationException("Unimplemented method 'token'");
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(String apiKey, String grantType) {
        throw new UnsupportedOperationException("Unimplemented method 'asyncToken'");
    }

    public static final class CustomIBMCloudRestClientBuilderFactory implements IBMCloudRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomIBMCloudRestClient.Builder();
        }
    }

    static final class Builder extends IBMCloudRestClient.Builder<CustomIBMCloudRestClient, Builder> {
        @Override
        public CustomIBMCloudRestClient build() {
            return new CustomIBMCloudRestClient(this);
        }
    }
}
