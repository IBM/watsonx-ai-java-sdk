/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

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
 * Abstraction of a REST client for interacting with the IBM Cloud Pak for Data
 */
public abstract class CP4DRestClient {
    protected final URI baseUrl;
    protected final Duration timeout;
    protected final HttpClient httpClient;

    protected CP4DRestClient(Builder<?, ?> builder) {
        baseUrl = requireNonNull(builder.baseUrl, "The baseUrl is mandatory");
        timeout = builder.timeout;
        httpClient = requireNonNullElse(builder.httpClient, HttpClientProvider.httpClient());
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
     * Creates a new {@link Builder} by loading the first available {@code CP4D*RestClientBuilderFactory} discovered via {@link ServiceLoader},
     * selecting the appropriate factory based on the specified {@link AuthMode}.
     * <p>
     * The behavior of this method varies depending on the chosen authentication mode:
     * <ul>
     * <li><b>{@link AuthMode#IAM}</b> → loads a {@link CP4DIAMRestClientBuilderFactory}</li>
     * <li><b>{@link AuthMode#LEGACY}</b> → loads a {@link CP4DLegacyRestClientBuilderFactory}</li>
     * <li><b>{@link AuthMode#ZEN_API_KEY}</b> → loads a {@link CP4DZenRestClientBuilderFactory}</li>
     * </ul>
     * If no implementation is found for the selected mode, the method falls back to the corresponding default REST client builder.
     *
     * @param authMode the authentication mode used to select which type of REST client builder to load
     * @return a {@link Builder} instance appropriate for the selected authentication mode
     */
    @SuppressWarnings("rawtypes")
    static Builder builder(AuthMode authMode) {
        authMode = requireNonNullElse(authMode, AuthMode.LEGACY);
        return switch(authMode) {
            case IAM -> ServiceLoader.load(CP4DIAMRestClientBuilderFactory.class).findFirst()
                .map(Supplier::get)
                .orElse(DefaultIAMRestClient.builder());
            case LEGACY -> ServiceLoader.load(CP4DLegacyRestClientBuilderFactory.class).findFirst()
                .map(Supplier::get)
                .orElse(DefaultLegacyRestClient.builder());
            case ZEN_API_KEY -> ServiceLoader.load(CP4DZenRestClientBuilderFactory.class).findFirst()
                .map(Supplier::get)
                .orElse(DefaultZenRestClient.builder());
        };
    }

    /**
     * Builder abstract class for constructing {@link CP4DRestClient} instances with configurable parameters.
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends CP4DRestClient, B extends Builder<T, B>> {
        private URI baseUrl;
        private Duration timeout;
        private HttpClient httpClient;

        protected abstract T build();

        public B baseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
            return (B) this;
        }

        public B timeout(Duration timeout) {
            this.timeout = timeout;
            return (B) this;
        }

        public B httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return (B) this;
        }
    }

    /**
     * Service Provider Interface for supplying custom {@link Builder} for Legacy implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    @SuppressWarnings("rawtypes")
    public interface CP4DLegacyRestClientBuilderFactory extends Supplier<CP4DRestClient.Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} for IAM implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    @SuppressWarnings("rawtypes")
    public interface CP4DIAMRestClientBuilderFactory extends Supplier<CP4DRestClient.Builder> {}

    /**
     * Service Provider Interface for supplying custom {@link Builder} for Zen implementations.
     * <p>
     * This allows frameworks to provide their own client implementations.
     */
    @SuppressWarnings("rawtypes")
    public interface CP4DZenRestClientBuilderFactory extends Supplier<CP4DRestClient.Builder> {}
}
