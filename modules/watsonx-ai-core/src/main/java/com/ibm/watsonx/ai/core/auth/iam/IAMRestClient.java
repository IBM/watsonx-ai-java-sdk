/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.iam;

import java.net.URI;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.core.auth.IdentityTokenResponse;

/**
 * Abstraction of a REST client for interacting with the IBM Cloud Identity and Access Management service.
 */
public abstract class IAMRestClient {

    protected final URI baseUrl;
    protected final Duration timeout;

    protected IAMRestClient(Builder<?, ?> builder) {
        this.baseUrl = builder.baseUrl;
        this.timeout = builder.timeout;
    }

    /**
     * Performs a synchronous REST call to the IAM service to obtain an identity token.
     *
     * @param apiKey the IBM Cloud API key to exchange for an access token
     * @param grantType the grant type to use
     * @return an {@link IdentityTokenResponse} containing the access token and related metadata
     */
    public abstract IdentityTokenResponse token(String apiKey, String grantType);

    /**
     * Performs an asynchronous REST call to the IAM service to obtain an identity token.
     *
     * @param apiKey the IBM Cloud API key to exchange for an access token
     * @param grantType the grant type to use
     * @return a {@link CompletableFuture} that completes with the IAM response or exceptionally on error
     */
    public abstract CompletableFuture<IdentityTokenResponse> asyncToken(String apiKey, String grantType);

    /**
     * Creates a new {@link Builder} using the first available {@link IAMRestClientBuilderFactory} discovered via {@link ServiceLoader}.
     * <p>
     * If no factory is found, falls back to the default {@link DefaultRestClient}.
     */
    @SuppressWarnings("rawtypes")
    static IAMRestClient.Builder builder() {
        return ServiceLoader.load(IAMRestClientBuilderFactory.class).findFirst()
            .map(Supplier::get)
            .orElse(DefaultRestClient.builder());
    }

    /**
     * Builder abstract class for constructing {@link IAMRestClient} instances with configurable parameters.
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends IAMRestClient, B extends Builder<T, B>> {
        private URI baseUrl;
        private Duration timeout;

        public abstract T build();

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
     * This allows frameworks (e.g., Quarkus, Spring) to provide their own client implementations.
     */
    @SuppressWarnings("rawtypes")
    public interface IAMRestClientBuilderFactory extends Supplier<IAMRestClient.Builder> {}
}
