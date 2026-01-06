/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.ibmcloud;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

/**
 * Abstraction of a REST client for interacting with the IBM Cloud Identity and Access Management service.
 */
public abstract class IBMCloudRestClient {
    protected final URI baseUrl;
    protected final Duration timeout;
    protected final HttpClient httpClient;

    protected IBMCloudRestClient(Builder<?, ?> builder) {
        baseUrl = requireNonNull(builder.baseUrl, "The baseUrl is mandatory");
        timeout = builder.timeout;
        httpClient = requireNonNullElse(builder.httpClient, HttpClientProvider.httpClient(builder.verifySsl));
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
        private HttpClient httpClient;
        private boolean verifySsl = true;

        /**
         * Builds and returns the configured REST client instance.
         *
         * @return the constructed REST client
         */
        protected abstract T build();

        /**
         * Sets the endpoint URL to which the chat request will be sent.
         *
         * @param baseUrl the endpoint URL
         */
        public B baseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
            return (B) this;
        }

        /**
         * Sets the request timeout.
         *
         * @param timeout {@link Duration} timeout.
         */
        public B timeout(Duration timeout) {
            this.timeout = timeout;
            return (B) this;
        }

        /**
         * Sets a custom {@link HttpClient} to be used for HTTP communication.
         * <p>
         * This allows customization of the underlying HTTP client, such as configuring a custom {@link javax.net.ssl.SSLContext} for TLS/SSL
         * settings, proxy configuration, connection timeouts, or other HTTP client properties. If not specified, a default {@link HttpClient} will be
         * created automatically.
         *
         * @param httpClient the custom {@link HttpClient} to use
         */
        public B httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return (B) this;
        }

        /**
         * Sets whether SSL/TLS certificate verification should be performed.
         * <p>
         * When set to {@code true} (default), the client validates server certificates against trusted Certificate Authorities. When set to
         * {@code false}, all certificates are accepted without validation, including self-signed certificates.
         * <p>
         * This setting is ignored if a custom {@link HttpClient} is provided via {@link #httpClient(HttpClient)}.
         *
         * @param verifySsl {@code true} to enable certificate verification, {@code false} to accept all certificates
         */
        public B verifySsl(boolean verifySsl) {
            this.verifySsl = verifySsl;
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
