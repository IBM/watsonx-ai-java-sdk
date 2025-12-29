/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

/**
 * Default implementation of the {@link CP4DRestClient}.
 */
class DefaultLegacyRestClient extends CP4DRestClient {
    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultLegacyRestClient(Builder builder) {
        super(builder);
        syncHttpClient = HttpClientFactory.createSync(null, httpClient, null);
        asyncHttpClient = HttpClientFactory.createAsync(null, httpClient, null);
    }

    @Override
    public TokenResponse token(TokenRequest request) {
        try {

            var response = syncHttpClient.send(createTokenRequest(request), BodyHandlers.ofString());
            return parseTokenResponse(response);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(TokenRequest request) {
        return asyncHttpClient
            .send(createTokenRequest(request), BodyHandlers.ofString())
            .thenApplyAsync(this::parseTokenResponse, ExecutorProvider.cpuExecutor())
            .thenApplyAsync(Function.identity(), ExecutorProvider.ioExecutor());
    }

    /*
    * Returns a new {@link Builder} instance.
    */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Parses the HTTP response returned by the authentication endpoint and converts it into a {@link TokenResponse}.
     */
    private TokenResponse parseTokenResponse(HttpResponse<String> response) {
        var json = fromJson(response.body(), new TypeToken<Map<String, Object>>() {});
        return new TokenResponse(String.valueOf(json.get("token")), null, null, null, null, null);
    }

    /**
     * Builds the HTTP request used to obtain an authentication token.
     */
    private HttpRequest createTokenRequest(TokenRequest request) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl.toString().concat("/icp4d-api/v1/authorize")))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(toJson(request)))
            .build();
    }

    /**
     * Builder class for constructing {@link DefaultLegacyRestClient} instances with configurable parameters.
     */
    static final class Builder extends CP4DRestClient.Builder<DefaultLegacyRestClient, Builder> {

        @Override
        public DefaultLegacyRestClient build() {
            return new DefaultLegacyRestClient(this);
        }
    }
}
