/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DRestClient;
import com.ibm.watsonx.ai.core.auth.cp4d.TokenRequest;
import com.ibm.watsonx.ai.core.auth.cp4d.TokenResponse;

public class CustomCP4DLegacyRestClient extends CP4DRestClient {

    CustomCP4DLegacyRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public CompletableFuture<TokenResponse> tokenAsync(TokenRequest request) {
        return completedFuture(new TokenResponse("custom-cp4d-legacy-token", null, "Bearer", null, null, null));
    }

    public static final class CustomCP4DLegacyRestClientBuilderFactory implements CP4DRestClient.CP4DLegacyRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomCP4DLegacyRestClient.Builder();
        }
    }

    static final class Builder extends CP4DRestClient.Builder<CustomCP4DLegacyRestClient, Builder> {
        @Override
        public CustomCP4DLegacyRestClient build() {
            return new CustomCP4DLegacyRestClient(this);
        }
    }
}
