/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.ibmcloud;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Default implementation of the {@link IBMCloudRestClient}.
 */
final class DefaultRestClient extends IBMCloudRestClient {
    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        syncHttpClient = HttpClientFactory.createSync(null, httpClient, null);
        asyncHttpClient = HttpClientFactory.createAsync(null, httpClient, null);
    }

    @Override
    public TokenResponse token(String apiKey, String grantType) {

        try {

            var response = syncHttpClient.send(createHttpRequest(apiKey, grantType), BodyHandlers.ofString());
            return fromJson(response.body(), TokenResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(String apiKey, String grantType) {
        return asyncHttpClient.send(createHttpRequest(apiKey, grantType), BodyHandlers.ofString())
            .thenApplyAsync(response -> fromJson(response.body(), TokenResponse.class), ExecutorProvider.cpuExecutor())
            .thenApplyAsync(Function.identity(), ExecutorProvider.ioExecutor());
    }

    /*
     * Returns a new {@link Builder} instance.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create an HTTP request to retrieve the token.
     */
    private HttpRequest createHttpRequest(String apiKey, String grantType) {
        BodyPublisher body = BodyPublishers.ofString("grant_type=%s&apikey=%s".formatted(encode(grantType), encode(apiKey)));
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl.toString() + "/identity/token"))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(body).build();
    }

    /**
     * Translates a value into application/x-www-form-urlencoded.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Builder class for constructing {@link DefaultRestClient} instances with configurable parameters.
     */
    static final class Builder extends IBMCloudRestClient.Builder<DefaultRestClient, Builder> {

        @Override
        public DefaultRestClient build() {
            return new DefaultRestClient(this);
        }
    }
}
