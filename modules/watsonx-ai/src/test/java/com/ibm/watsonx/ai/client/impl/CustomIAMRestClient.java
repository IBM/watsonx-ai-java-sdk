/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.auth.IdentityTokenResponse;
import com.ibm.watsonx.ai.core.auth.iam.IAMRestClient;

public class CustomIAMRestClient extends IAMRestClient {

    CustomIAMRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public IdentityTokenResponse token(String apiKey, String grantType) {
        throw new UnsupportedOperationException("Unimplemented method 'token'");
    }

    @Override
    public CompletableFuture<IdentityTokenResponse> asyncToken(String apiKey, String grantType) {
        throw new UnsupportedOperationException("Unimplemented method 'asyncToken'");
    }

    public static final class CustomIAMRestClientBuilderFactory implements IAMRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomIAMRestClient.Builder();
        }
    }

    static final class Builder extends IAMRestClient.Builder<CustomIAMRestClient, Builder> {
        @Override
        public CustomIAMRestClient build() {
            return new CustomIAMRestClient(this);
        }
    }
}
