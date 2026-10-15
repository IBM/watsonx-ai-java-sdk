/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

/**
 * Default implementation of the {@link DetectionRestClient} abstract class.
 */
final class DefaultRestClient extends DetectionRestClient {
    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticator, "authenticator is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses), requestLogger,
            requestLogLevel, responseLogger, responseLogLevel);
    }

    @Override
    public DetectionResponse<DetectionTextResponse> detect(String transactionId, String serviceInstanceId, TextDetectionContentDetectors request) {
        return doPost("/ml/v1/text/detection?version=%s".formatted(version), transactionId, serviceInstanceId, request,
            new TypeToken<DetectionResponse<DetectionTextResponse>>() {});
    }

    @Override
    public DetectionResponse<DetectionContextResponse> detectContext(String transactionId, String serviceInstanceId,
        TextDetectionContextDetectors request) {
        return doPost("/ml/v1/text/detection/context?version=%s".formatted(version), transactionId, serviceInstanceId, request,
            new TypeToken<DetectionResponse<DetectionContextResponse>>() {});
    }

    @Override
    public DetectionResponse<DetectionGeneratedResponse> detectGenerated(String transactionId, String serviceInstanceId,
        TextDetectionGeneratedDetectors request) {
        return doPost("/ml/v1/text/detection/generated?version=%s".formatted(version), transactionId, serviceInstanceId, request,
            new TypeToken<DetectionResponse<DetectionGeneratedResponse>>() {});
    }

    private <T extends BaseDetectionResponse> DetectionResponse<T> doPost(String path, String transactionId, String serviceInstanceId,
        Object request, TypeToken<DetectionResponse<T>> typeToken) {

        var httpRequest = HttpRequest
            .newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(toJson(request)))
            .timeout(timeout);

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        if (nonNull(serviceInstanceId))
            httpRequest.header(GOVERNANCE_INSTANCE_ID_HEADER, serviceInstanceId);

        try {

            var httpResponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpResponse.body(), typeToken);

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
    public final static class Builder extends DetectionRestClient.Builder {

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
