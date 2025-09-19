/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.iam;

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
import com.ibm.watsonx.ai.core.auth.IdentityTokenResponse;

/**
 * The {@code IAMAuthenticator} class is an implementation of the {@link AuthenticationProvider} interface, responsible for authenticating with IBM
 * Cloud Identity and Access Management using an API key.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * AuthenticationProvider authenticator = IAMAuthenticator.builder()
 *     .apiKey("api-key")
 *     .build();
 * }</pre>
 *
 * <pre>{@code
 * String accessToken = authenticator.getToken();
 * }</pre>
 *
 * For more information, see the <a href="https://cloud.ibm.com/docs/account?topic=account-iamtoken_from_apikey#iamtoken_from_apikey" target="_blank">
 * official documentation</a>.
 */
public final class IAMAuthenticator implements AuthenticationProvider {

    private final URI baseUrl;
    private final String apiKey;
    private final String grantType;
    private final Duration timeout;
    private final IAMRestClient client;
    private final AtomicReference<IdentityTokenResponse> token;

    /**
     * Constructs an IAMAuthenticator instance using the provided builder.
     *
     * @param builder the builder instance
     */
    private IAMAuthenticator(Builder builder) {
        token = new AtomicReference<>();
        apiKey = requireNonNull(builder.apiKey);
        baseUrl = requireNonNullElse(builder.baseUrl, URI.create("https://iam.cloud.ibm.com"));
        grantType = requireNonNullElse(builder.grantType, "urn:ibm:params:oauth:grant-type:apikey");
        timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(10));
        client = IAMRestClient.builder()
            .baseUrl(baseUrl)
            .timeout(timeout)
            .build();
    }

    @Override
    public String token() {

        IdentityTokenResponse currentToken = token.get();

        if (!isExpired(currentToken))
            return currentToken.accessToken();

        var identityTokenResponse = client.token(apiKey, grantType);
        token.getAndSet(identityTokenResponse);
        return identityTokenResponse.accessToken();
    }

    @Override
    public CompletableFuture<String> asyncToken() {

        IdentityTokenResponse currentToken = token.get();

        if (!isExpired(currentToken))
            return completedFuture(token.get().accessToken());

        return client.asyncToken(apiKey, grantType).thenApply(identityTokenResponse -> {
            token.getAndSet(identityTokenResponse);
            return identityTokenResponse.accessToken();
        });
    }

    /**
     * A builder class for constructing IAMAuthenticator instances. *
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * AuthenticationProvider authenticator = IAMAuthenticator.builder()
     *     .apiKey("api-key")
     *     .build();
     * }</pre>
     *
     * <pre>{@code
     * String accessToken = authenticator.getToken();
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
    private boolean isExpired(IdentityTokenResponse token) {
        if (isNull(token))
            return true;

        Date expiration = new Date(TimeUnit.SECONDS.toMillis(token.expiration()));
        Date now = new Date();

        if (expiration.after(now)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * The builder class for constructing IAMAuthenticator instances.
     */
    public static class Builder {
        private URI baseUrl;
        private String apiKey;
        private String grantType;
        private Duration timeout;

        /**
         * Prevents direct instantiation of the {@code Builder}.
         */
        protected Builder() {}

        /**
         * Sets the base URL for the IBM Cloud IAM token endpoint.
         *
         * @param baseUrl The base URL for the token endpoint.
         */
        public Builder baseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
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
         * Sets the grant type used for authentication.
         *
         * @param grantType The grant type for authentication.
         */
        public Builder grantType(String grantType) {
            this.grantType = grantType;
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
         * Builds and returns an IAMAuthenticator instance.
         *
         * @return {@link IAMAuthenticator} instance
         */
        public IAMAuthenticator build() {
            return new IAMAuthenticator(this);
        }
    }
}
