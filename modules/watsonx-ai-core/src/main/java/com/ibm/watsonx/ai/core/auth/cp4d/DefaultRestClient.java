/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.cp4d;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
            return switch(authMode) {
                case LEGACY -> {
                    var response = syncHttpClient.send(createTokenRequest(request), BodyHandlers.ofString());
                    yield parseTokenResponse(response);
                }
                case IAM -> {
                    var response = syncHttpClient.send(createTokenRequest(request), BodyHandlers.ofString());
                    var tokenResponse = parseTokenResponse(response);
                    var httpRequest = createIamValidationRequest(request.username(), tokenResponse.accessToken());
                    yield createTokenResponseForIAM(syncHttpClient.send(httpRequest, BodyHandlers.ofString()), tokenResponse);
                }
                case ZEN_API_KEY -> createTokenResponseForZenApiKey(request);
            };
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(TokenRequest request) {
        return switch(authMode) {
            case LEGACY -> asyncHttpClient
                .send(createTokenRequest(request), BodyHandlers.ofString())
                .thenApplyAsync(this::parseTokenResponse, ExecutorProvider.cpuExecutor())
                .thenApplyAsync(Function.identity(), ExecutorProvider.ioExecutor());
            case IAM -> asyncHttpClient
                .send(createTokenRequest(request), BodyHandlers.ofString())
                .thenComposeAsync(response -> {
                    var tokenResponse = parseTokenResponse(response);
                    var httpRequest = createIamValidationRequest(request.username(), tokenResponse.accessToken());
                    return asyncHttpClient
                        .send(httpRequest, BodyHandlers.ofString())
                        .thenApplyAsync(httpResponse -> createTokenResponseForIAM(httpResponse, tokenResponse), ExecutorProvider.ioExecutor());
                })
                .thenApplyAsync(Function.identity(), ExecutorProvider.ioExecutor());
            case ZEN_API_KEY -> completedFuture(createTokenResponseForZenApiKey(request));
        };
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
                var json = fromJson(response.body(), new TypeToken<Map<String, Object>>() {});
                yield new TokenResponse(String.valueOf(json.get("token")), null, null, null, null, null);
            }
            case ZEN_API_KEY -> throw new IllegalStateException("parseTokenResponse should not be called when authMode is ZEN_API_KEY");
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
        return switch(authMode) {
            case IAM -> HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.toString().concat("/idprovider/v1/auth/identitytoken")))
                .timeout(timeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(
                    "grant_type=password&username=%s&password=%s&scope=openid".formatted(
                        encode(request.username()), encode(request.password())
                    )))
                .build();
            case LEGACY -> HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.toString().concat("/icp4d-api/v1/authorize")))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(toJson(request)))
                .build();
            case ZEN_API_KEY -> throw new IllegalStateException("createTokenRequest should not be called when authMode is ZEN_API_KEY");
        };
    }

    /**
     * Creates a {@link TokenResponse} for Zen API Key authentication.
     *
     * @param request The {@link TokenRequest} containing the username, API key, or password.
     * @return A {@link TokenResponse} containing the base64-encoded access token.
     */
    private TokenResponse createTokenResponseForZenApiKey(TokenRequest request) {
        var username = request.username();
        var password = nonNull(request.apiKey()) ? request.apiKey() : request.password();
        var accessToken = Base64.getEncoder().encodeToString("%s:%s".formatted(username, password).getBytes());
        return new TokenResponse(accessToken, null, null, null, null, null);
    }

    /**
     * Creates a {@link TokenResponse} for IAM authentication.
     *
     * @param httpResponse The HTTP response containing the IAM authentication information.
     * @param tokenResponse The original {@link TokenResponse} that contains some pre-existing token data like refresh token.
     * @return A {@link TokenResponse} populated with the access token and other token details.
     */
    private TokenResponse createTokenResponseForIAM(HttpResponse<String> httpResponse, TokenResponse tokenResponse) {
        var json = fromJson(httpResponse.body(), new TypeToken<Map<String, Object>>() {});
        return new TokenResponse(
            String.valueOf(json.containsKey("accessToken") ? json.get("accessToken") : json.get("token")),
            tokenResponse.refreshToken(),
            tokenResponse.tokenType(),
            tokenResponse.expiresIn(),
            tokenResponse.expiration(),
            tokenResponse.scope()
        );
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
