/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.timeseries;

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

/**
 * Default implementation of the {@link TimeSeriesRestClient} interface.
 */
final class DefaultRestClient extends TimeSeriesRestClient {

    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        requireNonNull(authenticationProvider, "authenticationProvider is mandatory");
        syncHttpClient = HttpClientFactory.createSync(authenticationProvider, LogMode.of(logRequests, logResponses));
    }

    @Override
    public ForecastResponse forecast(String transactionId, ForecastRequest request) {

        var httpRequest = HttpRequest
            .newBuilder(URI.create(baseUrl + "/ml/v1/time_series/forecast?version=%s".formatted(version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(timeout)
            .POST(BodyPublishers.ofString(toJson(request)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), ForecastResponse.class);

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
    public static class Builder extends TimeSeriesRestClient.Builder {

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
