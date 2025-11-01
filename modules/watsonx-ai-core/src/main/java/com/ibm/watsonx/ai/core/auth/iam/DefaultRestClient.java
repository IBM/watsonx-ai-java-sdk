/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth.iam;

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
import com.ibm.watsonx.ai.core.auth.IdentityTokenResponse;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Default implementation of the {@link IAMRestClient}.
 */
final class DefaultRestClient extends IAMRestClient {

    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        syncHttpClient = HttpClientFactory.createSync(null, null);
        asyncHttpClient = HttpClientFactory.createAsync(null, null);
    }

    @Override
    public IdentityTokenResponse token(String apiKey, String grantType) {

        try {

            var request = createHttpRequest(apiKey, grantType);
            var response = syncHttpClient.send(request, BodyHandlers.ofString());
            var statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300)
                return fromJson(response.body(), IdentityTokenResponse.class);

            // The status code is not 2xx.
            throw new RuntimeException(response.body());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<IdentityTokenResponse> asyncToken(String apiKey, String grantType) {

        var request = createHttpRequest(apiKey, grantType);

        return asyncHttpClient.send(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> {

                var statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300)
                    return fromJson(response.body(), IdentityTokenResponse.class);

                // The status code is not 2xx.
                throw new RuntimeException(response.body());

            }, ExecutorProvider.cpuExecutor())
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
    static final class Builder extends IAMRestClient.Builder<DefaultRestClient, Builder> {

        @Override
        public DefaultRestClient build() {
            return new DefaultRestClient(this);
        }
    }
}
