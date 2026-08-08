/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.embedding;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedHashMap;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link ModelGatewayEmbeddingRestClient} abstract class.
 */
final class DefaultRestClient extends ModelGatewayEmbeddingRestClient {

    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public ModelGatewayEmbeddingResponse embed(String model, ModelGatewayEmbeddingRequest request) {
        var url = URI.create(baseUrl + "/ml/gateway/v1/embeddings?version=%s".formatted(version));

        var body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("input", request.input());

        var parameters = request.parameters();
        if (nonNull(parameters)) {
            if (nonNull(parameters.dimensions()))
                body.put("dimensions", parameters.dimensions());
            if (nonNull(parameters.encodingFormat()))
                body.put("encoding_format", parameters.encodingFormat());
            if (nonNull(parameters.user()))
                body.put("user", parameters.user());
        }

        var httpRequest = HttpRequest.newBuilder(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(toJson(body)))
            .timeout(timeout)
            .build();

        try {
            var httpResponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpResponse.body(), ModelGatewayEmbeddingResponse.class);
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
    public static final class Builder extends ModelGatewayEmbeddingRestClient.Builder {

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
