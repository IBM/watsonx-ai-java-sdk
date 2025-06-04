package com.ibm.watsonx.core.http;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.ArrayList;
import java.util.List;

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
     * @param handler the body handler for the response
     * @param <T> the type of the response body
     * @return a HTTP response
     */
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler)
        throws IOException, InterruptedException {
        return new InterceptorChain(delegate, interceptors).proceed(request, bodyHandler);
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {link Builder} instance.
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
            throws IOException, InterruptedException {
            if (index < interceptors.size()) {
                var interceptorIndex = index++;
                var response = interceptors.get(interceptorIndex).intercept(request, bodyHandler, interceptorIndex, this);
                return response;
            }
            else
                return client.send(request, bodyHandler);
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
         * Sets the {@link HttpClient}.
         *
         * @param httpClient the HTTP client instance
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Adds a single {@link SyncHttpInterceptor}.
         *
         * @param interceptor the interceptor to add
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
         * @param interceptors the list of interceptors
         */
        public Builder interceptors(List<SyncHttpInterceptor> interceptors) {
            this.interceptors = interceptors;
            return this;
        }

        /**
         * Builds a new {@link SyncHttpClient} instance.
         *
         * @return {@link SyncHttpClient} instance
         */
        public SyncHttpClient build() {
            return new SyncHttpClient(this);
        }
    }
}
