/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link BatchRestClient} abstract class.
 */
final class DefaultRestClient extends BatchRestClient {

    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public BatchData submit(BatchCreateRequest request) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/batches?version=%s".formatted(version)))
            .POST(BodyPublishers.ofString(Json.toJson(request)))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");

        if (nonNull(request.projectId()))
            httpRequest.header("X-IBM-Project-ID", request.projectId());

        if (nonNull(request.spaceId()))
            httpRequest.header("X-IBM-Space-ID", request.spaceId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), BatchData.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BatchListResponse list(BatchListRequest request) {

        var url = new StringBuilder(baseUrl + "/ml/v1/batches?version=%s".formatted(version));

        if (nonNull(request.limit()))
            url.append("&limit=%s".formatted(request.limit()));

        var httpRequest = HttpRequest.newBuilder(URI.create(url.toString()))
            .GET()
            .timeout(timeout)
            .header("Accept", "application/json");

        if (nonNull(request.projectId()))
            httpRequest.header("X-IBM-Project-ID", request.projectId());

        if (nonNull(request.spaceId()))
            httpRequest.header("X-IBM-Space-ID", request.spaceId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), BatchListResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BatchData retrieve(BatchRetrieveRequest request) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/batches/%s?version=%s".formatted(request.batchId(), version)))
            .GET()
            .timeout(timeout)
            .header("Accept", "application/json");

        if (nonNull(request.projectId()))
            httpRequest.header("X-IBM-Project-ID", request.projectId());

        if (nonNull(request.spaceId()))
            httpRequest.header("X-IBM-Space-ID", request.spaceId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), BatchData.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BatchData cancel(BatchCancelRequest request) {

        var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/ml/v1/batches/%s/cancel?version=%s".formatted(request.batchId(), version)))
            .POST(BodyPublishers.noBody())
            .timeout(timeout)
            .header("Accept", "application/json");

        if (nonNull(request.projectId()))
            httpRequest.header("X-IBM-Project-ID", request.projectId());

        if (nonNull(request.spaceId()))
            httpRequest.header("X-IBM-Space-ID", request.spaceId());

        if (nonNull(request.transactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, request.transactionId());

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), BatchData.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
    public final static class Builder extends BatchRestClient.Builder {

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
