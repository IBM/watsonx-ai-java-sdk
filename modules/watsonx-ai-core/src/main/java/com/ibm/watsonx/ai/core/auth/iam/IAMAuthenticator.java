/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.iam;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;

/**
 * The {@code IAMAuthenticator} class is an implementation of the {@link AuthenticationProvider} interface, responsible for authenticating with IBM
 * Cloud Identity and Access Management (IAM) using an API key. It manages token acquisition and caching, and handles automatic refresh if the token
 * expires.
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

    private final URI url;
    private final String apiKey;
    private final String grantType;
    private final Duration timeout;
    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;
    private final AtomicReference<IdentityTokenResponse> token;

    /**
     * Constructs an IAMAuthenticator instance using the provided builder.
     *
     * @param builder the builder instance
     */
    protected IAMAuthenticator(Builder builder) {
        token = new AtomicReference<>();
        apiKey = encode(requireNonNull(builder.apiKey));
        url = requireNonNullElse(builder.url, URI.create("https://iam.cloud.ibm.com/identity/token"));
        grantType = encode(requireNonNullElse(builder.grantType, "urn:ibm:params:oauth:grant-type:apikey"));
        timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(10));
        var httpClient = HttpClientProvider.httpClient();
        syncHttpClient = SyncHttpClient.builder().httpClient(httpClient).build();
        asyncHttpClient = AsyncHttpClient.builder().httpClient(httpClient).build();
    }

    @Override
    public String token() {

        try {

            IdentityTokenResponse currentToken = token.get();

            if (!isExpired(currentToken))
                return currentToken.accessToken();

            var request = createHttpRequest();
            var response = syncHttpClient.send(request, BodyHandlers.ofString());
            var statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                token.getAndSet(fromJson(response.body(), IdentityTokenResponse.class));
                return token.get().accessToken();
            }

            // The status code is not 2xx.
            throw new RuntimeException(response.body());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<String> asyncToken() {

        IdentityTokenResponse currentToken = token.get();
        var request = createHttpRequest();

        if (!isExpired(currentToken)) {
            return completedFuture(token.get().accessToken());
        }

        return asyncHttpClient.send(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> {

                var statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    token.getAndSet(fromJson(response.body(), IdentityTokenResponse.class));
                    return token.get().accessToken();
                }

                // The status code is not 2xx.
                throw new RuntimeException(response.body());
            }, ExecutorProvider.cpuExecutor())
            .thenApplyAsync(r -> r, ExecutorProvider.ioExecutor());
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
     * Create an HTTP request to retrieve the token.
     */
    private HttpRequest createHttpRequest() {
        BodyPublisher body = BodyPublishers.ofString("grant_type=%s&apikey=%s".formatted(grantType, apiKey));
        return HttpRequest.newBuilder()
            .uri(url).timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(body).build();
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
     * Translates a value into application/x-www-form-urlencoded.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * The builder class for constructing IAMAuthenticator instances.
     */
    public static class Builder {

        private URI url;
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
         * @param url The base URL for the token endpoint.
         * @return {@code Builder} instance for method chaining
         */
        public Builder url(URI url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the API key used for authentication.
         *
         * @param apiKey The API key for authentication.
         * @return {@code Builder} instance for method chaining
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the grant type used for authentication.
         *
         * @param grantType The grant type for authentication.
         * @return {@code Builder} instance for method chaining
         */
        public Builder grantType(String grantType) {
            this.grantType = grantType;
            return this;
        }

        /**
         * Sets the timeout duration for the token request.
         *
         * @param timeout The timeout duration for the token request.
         * @return {@code Builder} instance for method chaining
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
