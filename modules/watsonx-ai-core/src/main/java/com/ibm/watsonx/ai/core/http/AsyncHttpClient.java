/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.HttpUtils;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;

/**
 * Asynchronous HTTP client.
 *
 * @see AsyncHttpInterceptor
 * @see HttpClient
 */
public final class AsyncHttpClient extends BaseHttpClient {

    final List<AsyncHttpInterceptor> interceptors;

    /**
     * Constructs an {@code AsyncHttpClient} with the given underlying {@link HttpClient} and interceptors.
     *
     * @param httpClient the HTTP client to use; if {@code null}, a default client is used
     * @param interceptors a list of asynchronous HTTP interceptors; may be {@code null}
     */
    AsyncHttpClient(HttpClient httpClient, List<AsyncHttpInterceptor> interceptors) {
        super(requireNonNullElse(httpClient, HttpClient.newHttpClient()));
        this.interceptors = requireNonNullElse(interceptors, List.of());
    }

    /**
     * Constructs an AsyncHttpClient instance using the provided builder.
     *
     * @param builder the builder instance
     */
    public AsyncHttpClient(Builder builder) {
        this(builder.httpClient, builder.interceptors);
    }

    /**
     * Sends an asynchronous HTTP request.
     *
     * @param request the HTTP request to send
     * @param handler the body handler for the response
     * @param <T> the type of the response body
     * @return a {@link CompletableFuture} of the HTTP response
     */
    public <T> CompletableFuture<HttpResponse<T>> send(HttpRequest request, BodyHandler<T> handler) {
        return new InterceptorChain(delegate, interceptors).proceed(addRequestIdHeaderIfNotPresent(request), handler);
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
    private final static class InterceptorChain implements AsyncHttpInterceptor.AsyncChain {
        private final HttpClient httpClient;
        private final List<AsyncHttpInterceptor> interceptors;
        private int index;

        public InterceptorChain(HttpClient httpClient, List<AsyncHttpInterceptor> interceptors) {
            this.httpClient = httpClient;
            this.interceptors = interceptors;
            this.index = 0;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> proceed(HttpRequest request, BodyHandler<T> handler) {
            if (index < interceptors.size()) {
                var interceptorIndex = index++;
                return interceptors.get(interceptorIndex).intercept(request, handler, interceptorIndex, this);
            } else {
                return httpClient.sendAsync(request, responseInfo -> {

                    int statusCode = responseInfo.statusCode();
                    if (statusCode >= 200 && statusCode < 300) {
                        return handler.apply(responseInfo);
                    }

                    return BodySubscribers.mapping(
                        BodySubscribers.ofString(StandardCharsets.UTF_8),
                        body -> {
                            if (body.isEmpty())
                                throw new WatsonxException("Status code: " + statusCode, statusCode, null);

                            String contentType = responseInfo.headers().firstValue("Content-Type")
                                .orElseThrow(() -> new WatsonxException(body, statusCode, null));

                            WatsonxError details = HttpUtils.parseErrorBody(statusCode, body, contentType);
                            throw new WatsonxException(body, statusCode, details);
                        }
                    );
                });
            }
        }

        @Override
        public void resetToIndex(int index) {
            this.index = index;
        }
    }

    /**
     * Builder for {@link AsyncHttpClient}.
     */
    public final static class Builder {

        private HttpClient httpClient;
        private List<AsyncHttpInterceptor> interceptors;

        /**
         * Prevents direct instantiation of the {@code Builder}.
         */
        protected Builder() {}

        /**
         * Sets the {@link HttpClient}.
         *
         * @param httpClient the HTTP client instance
         * @return {@code Builder} instance for method chaining
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Adds a single {@link AsyncHttpInterceptor}.
         *
         * @param interceptor the interceptor to add
         * @return {@code Builder} instance for method chaining
         */
        public Builder interceptor(AsyncHttpInterceptor interceptor) {
            requireNonNull(interceptor, "The interceptor cannot be null");
            interceptors = requireNonNullElse(interceptors, new ArrayList<>());
            interceptors.add(interceptor);
            return this;
        }

        /**
         * Sets the list of {@link AsyncHttpInterceptor}s.
         *
         * @param interceptors the list of interceptors
         * @return {@code Builder} instance for method chaining
         */
        public Builder interceptors(List<AsyncHttpInterceptor> interceptors) {
            this.interceptors = interceptors;
            return this;
        }

        /**
         * Builds a new {@link AsyncHttpClient} instance.
         *
         * @return {@link AsyncHttpClient} instance
         */
        public AsyncHttpClient build() {
            return new AsyncHttpClient(this);
        }
    }
}
