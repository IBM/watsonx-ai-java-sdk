/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.catalog;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link ModelGatewayCatalogRestClient} abstract class.
 */
final class DefaultRestClient extends ModelGatewayCatalogRestClient {

    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses));
    }

    @Override
    public List<ModelGatewayModel> listModels() {
        var url = URI.create(baseUrl + "/ml/gateway/v1/models?version=%s".formatted(version));
        var httpRequest = HttpRequest.newBuilder(url)
            .header("Accept", "application/json")
            .GET()
            .build();
        try {
            var httpResponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpResponse.body(), ModelGatewayListModelsResponse.class).data();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ModelGatewayModel getModel(String modelUuid) {
        requireNonNull(modelUuid, "modelUuid cannot be null");
        var url = URI.create(baseUrl + "/ml/gateway/v1/models/%s?version=%s".formatted(modelUuid, version));
        var httpRequest = HttpRequest.newBuilder(url)
            .header("Accept", "application/json")
            .GET()
            .build();
        try {
            var httpResponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpResponse.body(), ModelGatewayModel.class);
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
    public static final class Builder extends ModelGatewayCatalogRestClient.Builder {

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
