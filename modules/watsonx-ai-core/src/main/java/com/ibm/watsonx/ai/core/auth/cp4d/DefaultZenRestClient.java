/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of the {@link CP4DRestClient}.
 */
class DefaultZenRestClient extends CP4DRestClient {

    DefaultZenRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public TokenResponse token(TokenRequest request) {
        return createTokenResponse(request);
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(TokenRequest request) {
        return completedFuture(createTokenResponse(request));
    }

    /*
    * Returns a new {@link Builder} instance.
    */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@link TokenResponse} for Zen API Key authentication.
     *
     * @param request The {@link TokenRequest} containing the username, API key, or password.
     * @return A {@link TokenResponse} containing the base64-encoded access token.
     */
    private TokenResponse createTokenResponse(TokenRequest request) {
        var username = request.username();
        var password = nonNull(request.apiKey()) ? request.apiKey() : request.password();
        var accessToken = Base64.getEncoder().encodeToString("%s:%s".formatted(username, password).getBytes());
        return new TokenResponse(accessToken, null, null, null, null, null);
    }

    /**
     * Builder class for constructing {@link DefaultZenRestClient} instances with configurable parameters.
     */
    static final class Builder extends CP4DRestClient.Builder<DefaultZenRestClient, Builder> {

        @Override
        public DefaultZenRestClient build() {
            return new DefaultZenRestClient(this);
        }
    }
}
