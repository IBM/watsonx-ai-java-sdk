/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

/**
 * Default implementation of the {@link CP4DRestClient}.
 */
class DefaultRestClient extends CP4DRestClient {
    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        syncHttpClient = HttpClientFactory.createSync(null, null);
        asyncHttpClient = HttpClientFactory.createAsync(null, null);
    }

    @Override
    public TokenResponse token(TokenRequest request) {

        try {

            var response = syncHttpClient.send(createHttpRequest(request), BodyHandlers.ofString());
            return parseTokenResponse(response);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(TokenRequest request) {
        return asyncHttpClient.send(createHttpRequest(request), BodyHandlers.ofString())
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
     * Translates a value into application/x-www-form-urlencoded.
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Parses the HTTP response returned by the authentication endpoint and converts it into a {@link TokenResponse}.
     */
    private TokenResponse parseTokenResponse(HttpResponse<String> response) {
        if (iam)
            return fromJson(response.body(), TokenResponse.class);

        var json = Json.fromJson(response.body(), new TypeToken<Map<String, Object>>() {});
        return new TokenResponse(String.valueOf(json.get("token")), null, null, null, null, null);
    }

    /**
     * Create an HTTP request to retrieve the token.
     */
    private HttpRequest createHttpRequest(TokenRequest request) {

        String path;
        String contentType;
        String body;

        if (iam) {
            path = "/idprovider/v1/auth/identitytoken";
            contentType = "application/x-www-form-urlencoded";
            body = "grant_type=password&username=%s&password=%s&scope=openid".formatted(encode(request.username()), encode(request.password()));
        } else {
            path = "/icp4d-api/v1/authorize";
            contentType = "application/json";
            body = Json.toJson(request);
        }

        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl.toString().concat(path)))
            .timeout(timeout)
            .header("Content-Type", contentType)
            .POST(BodyPublishers.ofString(body))
            .build();
    }

    /**
     * Builder class for constructing {@link DefaultRestClient} instances with configurable parameters.
     */
    static final class Builder extends CP4DRestClient.Builder<DefaultRestClient, Builder> {

        @Override
        public DefaultRestClient build() {
            return new DefaultRestClient(this);
        }
    }
}
