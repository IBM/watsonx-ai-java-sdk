/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Default implementation of the {@link TokenizationRestClient} abstract class.
 */
final class DefaultRestClient extends TokenizationRestClient {

    private final SyncHttpClient syncHttpClient;
    private final AsyncHttpClient asyncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticationProvider, "authenticationProvider is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticationProvider, LogMode.of(logRequests, logResponses));
        asyncHttpClient = HttpClientFactory.createAsync(authenticationProvider, LogMode.of(logRequests, logResponses));
    }

    @Override
    public TokenizationResponse tokenize(String transactionId, TokenizationRequest request) {

        var httpRequest = HttpRequest
            .newBuilder(URI.create(baseUrl + "/ml/v1/text/tokenization?version=%s".formatted(version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(toJson(request)))
            .timeout(timeout);

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TokenizationResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<TokenizationResponse> asyncTokenize(String transactionId, TokenizationRequest request) {

        var httpRequest = HttpRequest
            .newBuilder(URI.create(baseUrl + "/ml/v1/text/tokenization?version=%s".formatted(version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(toJson(request)))
            .timeout(timeout);

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        return asyncHttpClient.send(httpRequest.build(), BodyHandlers.ofString())
            .thenApplyAsync(r -> Json.fromJson(r.body(), TokenizationResponse.class), ExecutorProvider.cpuExecutor())
            .thenApplyAsync(r -> r, ExecutorProvider.ioExecutor());
    }

    /**
     * Returns a new {@link Builder} instance.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link DefaultRestClient} instances with configurable parameters.
     */
    public final static class Builder extends TokenizationRestClient.Builder {

        private Builder() {}

        /**
         * Builds a {@link DefaultRestClient} instance using the configured parameters.
         *
         * @return a new instance of {@link DefaultRestClient}
         */
        public DefaultRestClient build() {
            return new DefaultRestClient(this);
        }
    }
}
