/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

import static com.ibm.watsonx.ai.core.auth.cp4d.AuthMode.IAM;
import static com.ibm.watsonx.ai.core.auth.cp4d.AuthMode.LEGACY;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;

/**
 * The {@code CP4DAuthenticator} class is an implementation of the {@link AuthenticationProvider} interface, responsible for authenticating with Cloud
 * Pack for Data supporting both IAM-based authentication and legacy username/password or API key authentication.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * CP4DAuthenticator.builder()
 *     .baseUrl("https://...")
 *     .username("username")
 *     .apiKey("api-key")
 *     .authMode(AuthMode.LEGACY)
 *     .build();
 * }</pre>
 */
public final class CP4DAuthenticator implements AuthenticationProvider {
    private final URI baseUrl;
    private final String username;
    private final String password;
    private final String apiKey;
    private final Duration timeout;
    private final CP4DRestClient client;
    private final AuthMode authMode;
    private final AtomicReference<TokenResponse> token;

    private CP4DAuthenticator(Builder builder) {
        apiKey = builder.apiKey;
        password = builder.password;
        token = new AtomicReference<>();
        authMode = requireNonNullElse(builder.authMode, LEGACY);
        username = requireNonNull(builder.username, "The username parameter is mandatory");

        if (authMode.equals(IAM) && isNull(password))
            throw new NullPointerException("IAM authentication requires a password");

        if (authMode.equals(LEGACY) && isNull(password) && isNull(apiKey))
            throw new NullPointerException("Either password or apiKey must be provided");

        baseUrl = requireNonNull(builder.baseUrl, "The baseUrl parameter is mandatory");
        timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(60));
        client = CP4DRestClient.builder()
            .baseUrl(baseUrl)
            .timeout(timeout)
            .authMode(authMode)
            .build();
    }

    @Override
    public String token() {

        TokenResponse currentToken = token.get();

        if (!isExpired(currentToken))
            return currentToken.accessToken();

        var tokenResponse = client.token(new TokenRequest(username, password, apiKey));
        token.getAndSet(tokenResponse);
        return tokenResponse.accessToken();
    }

    @Override
    public CompletableFuture<String> asyncToken() {

        TokenResponse currentToken = token.get();

        if (!isExpired(currentToken))
            return completedFuture(token.get().accessToken());

        return client.asyncToken(new TokenRequest(username, password, apiKey)).thenApply(identityTokenResponse -> {
            token.getAndSet(identityTokenResponse);
            return identityTokenResponse.accessToken();
        });
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * CP4DAuthenticator.builder()
     *     .baseUrl("https://...")
     *     .username("username")
     *     .apiKey("api-key")
     *     .authMode(AuthMode.LEGACY)
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Check whether the token has expired.
     */
    private boolean isExpired(TokenResponse token) {
        if (isNull(token))
            return true;

        if (authMode.equals(LEGACY))
            return false;

        Date expiration = new Date(TimeUnit.SECONDS.toMillis(token.expiration()));
        return expiration.after(new Date()) ? false : true;
    }

    /**
     * The builder class for constructing IBMCloudAuthenticator instances.
     */
    public final static class Builder {
        private URI baseUrl;
        private String username;
        private String password;
        private String apiKey;
        private AuthMode authMode;
        private Duration timeout;

        /**
         * Prevents direct instantiation of the {@code Builder}.
         */
        private Builder() {}

        /**
         * Sets the base URL for the Cloud Pak for Data token endpoint.
         *
         * @param baseUrl The base URL for the token endpoint.
         */
        public Builder baseUrl(String baseUrl) {
            return baseUrl(URI.create(baseUrl));
        }


        /**
         * Sets the base URL for the Cloud Pak for Data token endpoint.
         *
         * @param baseUrl The base URL for the token endpoint.
         */
        public Builder baseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the username used for authentication.
         *
         * @param username The username for authentication.
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the password used for authentication.
         *
         * @param password The password for authentication.
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the API key used for authentication.
         *
         * @param apiKey The API key for authentication.
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the timeout duration for the token request.
         *
         * @param timeout The timeout duration for the token request.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the authentication mode.
         *
         * @param authMode the authentication mode to apply
         */
        public Builder authMode(AuthMode authMode) {
            this.authMode = authMode;
            return this;
        }

        /**
         * Builds and returns an CP4DAuthenticator instance.
         *
         * @return {@link CP4DAuthenticator} instance
         */
        public CP4DAuthenticator build() {
            return new CP4DAuthenticator(this);
        }
    }
}
