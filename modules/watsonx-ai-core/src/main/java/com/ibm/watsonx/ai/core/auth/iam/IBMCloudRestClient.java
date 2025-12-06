/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.iam;

import static java.util.Objects.requireNonNull;
import java.net.URI;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Abstraction of a REST client for interacting with the IBM Cloud Identity and Access Management service.
 */
public abstract class IBMCloudRestClient {
    protected final URI baseUrl;
    protected final Duration timeout;

    protected IBMCloudRestClient(Builder<?, ?> builder) {
        baseUrl = requireNonNull(builder.baseUrl, "The baseUrl is mandatory");
        timeout = builder.timeout;
    }

    /**
     * Performs a synchronous REST call to the IAM service to obtain an identity token.
     *
     * @param apiKey the IBM Cloud API key to exchange for an access token
     * @param grantType the grant type to use
     * @return an {@link TokenResponse} containing the access token and related metadata
     */
    public abstract TokenResponse token(String apiKey, String grantType);

    /**
     * Performs an asynchronous REST call to the IAM service to obtain an identity token.
     *
     * @param apiKey the IBM Cloud API key to exchange for an access token
     * @param grantType the grant type to use
     * @return a {@link CompletableFuture} that completes with the IAM response or exceptionally on error
     */
    public abstract CompletableFuture<TokenResponse> asyncToken(String apiKey, String grantType);

    /**
     * Creates a new {@link Builder} using the first available {@link IBMCloudRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    @SuppressWarnings("rawtypes")
    static IBMCloudRestClient.Builder builder() {
        return ServiceLoader.load(IBMCloudRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link IBMCloudRestClient} instances with configurable parameters.
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends IBMCloudRestClient, B extends Builder<T, B>> {
        private URI baseUrl;
        private Duration timeout;

        protected abstract T build();

        public B baseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
            return (B) this;
        }

        public B timeout(Duration timeout) {
            this.timeout = timeout;
            return (B) this;
        }
    }

    /**
     * Service Provider Interface for supplying custom {@link Builder} implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    @SuppressWarnings("rawtypes")
    public interface IBMCloudRestClientBuilderFactory extends Supplier<IBMCloudRestClient.Builder> {}
}
