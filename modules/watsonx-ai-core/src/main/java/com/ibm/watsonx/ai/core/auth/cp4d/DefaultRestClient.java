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

            var response = syncHttpClient.send(createTokenRequest(request), BodyHandlers.ofString());
            var tokenResponse = parseTokenResponse(response);

            return switch(authMode) {
                case LEGACY -> tokenResponse;
                case IAM -> {
                    response =
                        syncHttpClient.send(
                            createIamValidationRequest(request.username(), tokenResponse.accessToken()),
                            BodyHandlers.ofString());

                    var json = Json.fromJson(response.body(), new TypeToken<Map<String, Object>>() {});
                    yield new TokenResponse(
                        String.valueOf(json.containsKey("accessToken") ? json.get("accessToken") : json.get("token")),
                        tokenResponse.refreshToken(),
                        tokenResponse.tokenType(),
                        tokenResponse.expiresIn(),
                        tokenResponse.expiration(),
                        tokenResponse.scope()
                    );
                }
            };

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(TokenRequest request) {
        return asyncHttpClient
            .send(createTokenRequest(request), BodyHandlers.ofString())
            .thenApplyAsync(this::parseTokenResponse, ExecutorProvider.cpuExecutor())
            .thenComposeAsync(tokenResponse -> {
                return switch(authMode) {
                    case LEGACY -> CompletableFuture.completedFuture(tokenResponse);
                    case IAM -> asyncHttpClient
                        .send(createIamValidationRequest(request.username(), tokenResponse.accessToken()), BodyHandlers.ofString())
                        .thenApplyAsync(validateResp -> {
                            var json = Json.fromJson(validateResp.body(), new TypeToken<Map<String, Object>>() {});
                            return new TokenResponse(
                                String.valueOf(json.containsKey("accessToken") ? json.get("accessToken") : json.get("token")),
                                tokenResponse.refreshToken(),
                                tokenResponse.tokenType(),
                                tokenResponse.expiresIn(),
                                tokenResponse.expiration(),
                                tokenResponse.scope()
                            );
                        }, ExecutorProvider.cpuExecutor());
                };
            }, ExecutorProvider.ioExecutor());
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
        return switch(authMode) {
            case IAM -> fromJson(response.body(), TokenResponse.class);
            case LEGACY -> {
                var json = Json.fromJson(response.body(), new TypeToken<Map<String, Object>>() {});
                yield new TokenResponse(String.valueOf(json.get("token")), null, null, null, null, null);
            }
        };
    }

    /*
     * Builds the HTTP request used to validate an IAM identity token.
     */
    private HttpRequest createIamValidationRequest(String username, String accessToken) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/v1/preauth/validateAuth"))
            .timeout(timeout)
            .header("username", username)
            .header("iam-token", accessToken)
            .GET().build();
    }

    /**
     * Builds the HTTP request used to obtain an authentication token.
     */
    private HttpRequest createTokenRequest(TokenRequest request) {

        String path = null;
        String contentType = null;
        String body = null;

        switch(authMode) {
            case IAM -> {
                path = "/idprovider/v1/auth/identitytoken";
                contentType = "application/x-www-form-urlencoded";
                body = "grant_type=password&username=%s&password=%s&scope=openid".formatted(encode(request.username()), encode(request.password()));
            }
            case LEGACY -> {
                path = "/icp4d-api/v1/authorize";
                contentType = "application/json";
                body = Json.toJson(request);
            }
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
