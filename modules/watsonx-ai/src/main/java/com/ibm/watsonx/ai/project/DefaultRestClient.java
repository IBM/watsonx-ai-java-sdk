/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.factory.HttpClientFactory;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;

/**
 * Default implementation of the {@link ProjectRestClient} abstract class.
 */
final class DefaultRestClient extends ProjectRestClient {

    private final SyncHttpClient syncHttpClient;

    DefaultRestClient(Builder builder) {
        super(builder);
        syncHttpClient = HttpClientFactory.createSync(authenticator, httpClient, LogMode.of(logRequests, logResponses), requestLogger,
            requestLogLevel, responseLogger, responseLogLevel);
    }

    @Override
    public Optional<Project> findProject(String projectId) {
        try {
            var httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/v2/projects/" + projectId))
                .GET()
                .header("Accept", "application/json")
                .timeout(timeout)
                .build();

            var response = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return Optional.ofNullable(fromJson(response.body(), Project.class));

        } catch (WatsonxException e) {
            if (e.statusCode() == 404)
                return Optional.empty();
            throw e;
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
     * Builder class for constructing {@link DefaultRestClient} instances.
     */
    public static final class Builder extends ProjectRestClient.Builder {

        private Builder() {}

        @Override
        public DefaultRestClient build() {
            return new DefaultRestClient(this);
        }
    }
}
