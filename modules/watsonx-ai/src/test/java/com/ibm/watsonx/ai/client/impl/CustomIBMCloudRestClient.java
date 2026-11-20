/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudRestClient;
import com.ibm.watsonx.ai.core.auth.ibmcloud.TokenResponse;

public class CustomIBMCloudRestClient extends IBMCloudRestClient {

    CustomIBMCloudRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public CompletableFuture<TokenResponse> tokenAsync(String apiKey, String grantType) {
        return completedFuture(new TokenResponse("custom-ibm-cloud-token", null, "Bearer", 3600,
            Instant.now().plusSeconds(3600).getEpochSecond(), "ibm openid"));
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
