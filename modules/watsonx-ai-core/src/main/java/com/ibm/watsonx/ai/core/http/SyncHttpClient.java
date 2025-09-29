/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.ibm.watsonx.ai.core.HttpUtils;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;

/**
 * Synchronous HTTP client.
 *
 * @see SyncHttpInterceptor
 * @see HttpClient
 */
public final class SyncHttpClient extends BaseHttpClient {

    final List<SyncHttpInterceptor> interceptors;

    /**
     * Constructs an {@code SyncHttpClient} with the given underlying {@link HttpClient} and interceptors.
     *
     * @param httpClient the HTTP client to use; if {@code null}, a default client is used
     * @param interceptors a list of synchronous HTTP interceptors; may be {@code null}
     */
    SyncHttpClient(HttpClient httpClient, List<SyncHttpInterceptor> interceptors) {
        super(requireNonNullElse(httpClient, HttpClient.newHttpClient()));
        this.interceptors = requireNonNullElse(interceptors, List.of());
    }

    /**
     * Constructs an SyncHttpClient instance using the provided builder.
     *
     * @param builder the builder instance
     */
    public SyncHttpClient(Builder builder) {
        this(builder.httpClient, builder.interceptors);
    }

    /**
     * Sends an synchronous HTTP request.
     *
     * @param request the HTTP request to send
     * @param bodyHandler the body handler for the response
     * @param <T> the type of the response body
     * @return a HTTP response
     */
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler)
        throws WatsonxException, IOException, InterruptedException {
        return new InterceptorChain(delegate, interceptors).proceed(addRequestIdHeaderIfNotPresent(request), bodyHandler);
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Internal chain implementation used to process interceptors in order.
     */
    private final static class InterceptorChain implements SyncHttpInterceptor.Chain {
        private final HttpClient client;
        private final List<SyncHttpInterceptor> interceptors;
        private int index;

        InterceptorChain(HttpClient client, List<SyncHttpInterceptor> interceptors) {
            this.client = client;
            this.interceptors = interceptors;
            this.index = 0;
        }

        @Override
        public <T> HttpResponse<T> proceed(HttpRequest request, BodyHandler<T> bodyHandler)
            throws WatsonxException, IOException, InterruptedException {

            if (index < interceptors.size()) {
                int current = index++;
                return interceptors.get(current).intercept(request, bodyHandler, current, this);
            }

            HttpResponse<T> httpResponse = client.send(request, bodyHandler);
            int statusCode = httpResponse.statusCode();

            if (statusCode >= 200 && statusCode < 300)
                return httpResponse;

            Optional<String> bodyOpt = HttpUtils.extractBodyAsString(httpResponse);

            if (bodyOpt.isEmpty())
                throw new WatsonxException("Status code: " + statusCode, statusCode, null);

            String body = bodyOpt.get();
            String contentType = httpResponse.headers().firstValue("Content-Type")
                .orElseThrow(() -> new WatsonxException(body, statusCode, null));

            WatsonxError details = HttpUtils.parseErrorBody(statusCode, body, contentType);
            throw new WatsonxException(body, statusCode, details);
        }

        @Override
        public void resetToIndex(int index) {
            this.index = index;
        }
    }

    /**
     * Builder for {@link SyncHttpClient}.
     */
    public final static class Builder {

        private HttpClient httpClient;
        private List<SyncHttpInterceptor> interceptors;

        /**
         * Prevents direct instantiation of the {@code Builder}.
         */
        protected Builder() {}

        /**
         * Sets the {@link HttpClient}.
         *
         * @param httpClient the HTTP client instance.
         * @return {@code Builder} instance for method chaining.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Adds a single {@link SyncHttpInterceptor}.
         *
         * @param interceptor the interceptor to add.
         * @return {@code Builder} instance for method chaining.
         */
        public Builder interceptor(SyncHttpInterceptor interceptor) {
            requireNonNull(interceptor, "The interceptor cannot be null");
            interceptors = requireNonNullElse(interceptors, new ArrayList<>());
            interceptors.add(interceptor);
            return this;
        }

        /**
         * Sets the list of {@link SyncHttpInterceptor}s.
         *
         * @param interceptors the list of interceptors.
         * @return {@code Builder} instance for method chaining.
         */
        public Builder interceptors(List<SyncHttpInterceptor> interceptors) {
            this.interceptors = interceptors;
            return this;
        }

        /**
         * Builds a new {@link SyncHttpClient} instance.
         *
         * @return {@link SyncHttpClient} instance.
         */
        public SyncHttpClient build() {
            return new SyncHttpClient(this);
        }
    }
}
