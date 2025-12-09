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
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

/**
 * Default implementation of the {@link CP4DRestClient}.
 */
class DefaultIAMRestClient extends CP4DRestClient {
    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultIAMRestClient(Builder builder) {
        super(builder);
        syncHttpClient = HttpClientFactory.createSync(null, null);
        asyncHttpClient = HttpClientFactory.createAsync(null, null);
    }

    @Override
    public TokenResponse token(TokenRequest request) {
        try {

            var response = syncHttpClient.send(createTokenRequest(request), BodyHandlers.ofString());
            var tokenResponse = parseTokenResponse(response);
            var httpRequest = createValidationRequest(request.username(), tokenResponse.accessToken());
            return createTokenResponse(syncHttpClient.send(httpRequest, BodyHandlers.ofString()), tokenResponse);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<TokenResponse> asyncToken(TokenRequest request) {
        return asyncHttpClient
            .send(createTokenRequest(request), BodyHandlers.ofString())
            .thenComposeAsync(response -> {
                var tokenResponse = parseTokenResponse(response);
                var httpRequest = createValidationRequest(request.username(), tokenResponse.accessToken());
                return asyncHttpClient
                    .send(httpRequest, BodyHandlers.ofString())
                    .thenApplyAsync(httpResponse -> createTokenResponse(httpResponse, tokenResponse), ExecutorProvider.ioExecutor());
            })
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
        return fromJson(response.body(), TokenResponse.class);
    }

    /*
     * Builds the HTTP request used to validate an IAM identity token.
     */
    private HttpRequest createValidationRequest(String username, String accessToken) {
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
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl.toString().concat("/idprovider/v1/auth/identitytoken")))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(BodyPublishers.ofString(
                "grant_type=password&username=%s&password=%s&scope=openid".formatted(
                    encode(request.username()), encode(request.password())
                )))
            .build();
    }

    /**
     * Creates a {@link TokenResponse} for IAM authentication.
     *
     * @param httpResponse The HTTP response containing the IAM authentication information.
     * @param tokenResponse The original {@link TokenResponse} that contains some pre-existing token data like refresh token.
     * @return A {@link TokenResponse} populated with the access token and other token details.
     */
    private TokenResponse createTokenResponse(HttpResponse<String> httpResponse, TokenResponse tokenResponse) {
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
     * Builder class for constructing {@link DefaultIAMRestClient} instances with configurable parameters.
     */
    static final class Builder extends CP4DRestClient.Builder<DefaultIAMRestClient, Builder> {

        @Override
        public DefaultIAMRestClient build() {
            return new DefaultIAMRestClient(this);
        }
    }
}
