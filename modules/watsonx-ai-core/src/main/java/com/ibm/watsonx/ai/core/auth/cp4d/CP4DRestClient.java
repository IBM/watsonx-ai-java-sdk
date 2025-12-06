/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Abstraction of a REST client for interacting with the IBM Cloud Pak for Data
 */
public abstract class CP4DRestClient {
    protected final URI baseUrl;
    protected final Duration timeout;
    protected final AuthMode authMode;

    protected CP4DRestClient(Builder<?, ?> builder) {
        baseUrl = requireNonNull(builder.baseUrl, "The baseUrl is mandatory");
        authMode = requireNonNullElse(builder.authMode, AuthMode.LEGACY);
        timeout = builder.timeout;
    }

    /**
     * Performs a synchronous REST call to Cloud Pak for Data to obtain a token.
     *
     * @return an {@link TokenResponse} containing the token and related metadata
     */
    public abstract TokenResponse token(TokenRequest request);

    /**
     * Performs a synchronous REST call to Cloud Pak for Data to obtain a token.
     *
     * @return a {@link CompletableFuture} that contains the token and related metadata
     */
    public abstract CompletableFuture<TokenResponse> asyncToken(TokenRequest request);

    /**
     * Creates a new {@link Builder} using the first available {@link CP4DRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    @SuppressWarnings("rawtypes")
    static CP4DRestClient.Builder builder() {
        return ServiceLoader.load(CP4DRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link CP4DRestClient} instances with configurable parameters.
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends CP4DRestClient, B extends Builder<T, B>> {
        private URI baseUrl;
        private Duration timeout;
        private AuthMode authMode;

        protected abstract T build();

        public B baseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
            return (B) this;
        }

        public B timeout(Duration timeout) {
            this.timeout = timeout;
            return (B) this;
        }

        public B authMode(AuthMode authMode) {
            this.authMode = authMode;
            return (B) this;
        }
    }

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    @SuppressWarnings("rawtypes")
    public interface CP4DRestClientBuilderFactory extends Supplier<CP4DRestClient.Builder> {}
}
