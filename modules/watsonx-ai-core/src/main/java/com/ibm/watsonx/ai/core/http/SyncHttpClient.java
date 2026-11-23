/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import com.ibm.watsonx.ai.core.HttpUtils;
import com.ibm.watsonx.ai.core.exception.WatsonxException;

/**
 * Synchronous HTTP client.
 *
 * @see SyncHttpInterceptor
 * @see HttpClient
 */
public final class SyncHttpClient extends BaseHttpClient {

    final List<SyncHttpInterceptor> interceptors;
    private final LongSupplier nanoTime;
    private final Sleeper sleeper;

    /**
     * Constructs an {@code SyncHttpClient} with the given underlying {@link HttpClient} and interceptors.
     *
     * @param httpClient the HTTP client to use, if {@code null}, a default client is used
     * @param interceptors a list of synchronous HTTP interceptors; may be {@code null}
     */
    SyncHttpClient(HttpClient httpClient, List<SyncHttpInterceptor> interceptors) {
        this(httpClient, interceptors, System::nanoTime, TimeUnit.NANOSECONDS::sleep);
    }

    SyncHttpClient(HttpClient httpClient, List<SyncHttpInterceptor> interceptors, LongSupplier nanoTime, Sleeper sleeper) {
        super(requireNonNull(httpClient, "The HTTP client cannot be null"));
        this.interceptors = (interceptors == null) ? List.of() : List.copyOf(interceptors);
        this.nanoTime = requireNonNull(nanoTime);
        this.sleeper = requireNonNull(sleeper);
    }

    /**
     * Constructs an SyncHttpClient instance using the provided builder.
     *
     * @param builder the builder instance
     */
    private SyncHttpClient(Builder builder) {
        this(builder.httpClient, builder.interceptors);
    }

    /**
     * Sends an synchronous HTTP request.
     *
     * @param request the HTTP request to send
     * @param bodyHandler the body handler for the response
     * @param <T> the type of the response body
     * @return a HTTP response
     * @throws IOException if an I/O error occurs when sending or receiving the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler)
        throws WatsonxException, IOException, InterruptedException {
        return new InterceptorChain(delegate, interceptors, null).proceed(addRequestIdHeaderIfNotPresent(request), bodyHandler);
    }

    /**
     * Sends a synchronous HTTP request with a time budget shared by all attempts and retry delays.
     * <p>
     * Each HTTP attempt uses the smaller of its configured timeout and the remaining budget. Successful responses are returned so callers can inspect
     * their metadata before enforcing an operation deadline. Custom interceptors must use {@link SyncHttpInterceptor.Chain#pause(Duration)} for retry
     * delays to respect the budget.
     * <p>
     * Authentication runs synchronously on the calling thread and uses the authenticator's own timeout. Its elapsed time is deducted from the
     * remaining budget after it returns. The budget does not interrupt authentication or other synchronous interceptor work.
     * <p>
     * HTTP timeouts are enforced by the underlying {@link HttpClient}. Response body reads, including error bodies consumed internally, are not
     * interrupted by this budget and may delay the method's return beyond it.
     *
     * @param request the HTTP request to send
     * @param bodyHandler the body handler for the response
     * @param budget the time available for HTTP attempts and retry delays
     * @param <T> the type of the response body
     * @return the HTTP response
     * @throws HttpTimeoutException if an HTTP attempt times out or the budget expires
     * @throws IllegalArgumentException if the budget is zero or negative
     * @throws IOException if an I/O error occurs when sending or receiving the request
     * @throws InterruptedException if the operation is interrupted
     */
    public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler, Duration budget)
        throws WatsonxException, IOException, InterruptedException {
        requireNonNull(budget, "The time budget cannot be null");
        if (budget.isZero() || budget.isNegative())
            throw new IllegalArgumentException("The time budget must be positive");
        var deadline = new Deadline(budget, nanoTime, sleeper);
        return new InterceptorChain(delegate, interceptors, deadline).proceed(addRequestIdHeaderIfNotPresent(request), bodyHandler);
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
        private final Deadline deadline;
        private int index;

        InterceptorChain(HttpClient client, List<SyncHttpInterceptor> interceptors, Deadline deadline) {
            this.client = client;
            this.interceptors = interceptors;
            this.deadline = deadline;
            this.index = 0;
        }

        @Override
        public <T> HttpResponse<T> proceed(HttpRequest request, BodyHandler<T> bodyHandler)
            throws WatsonxException, IOException, InterruptedException {

            checkDeadline();
            try {
                return execute(request, bodyHandler);
            } catch (IOException | RuntimeException e) {
                if (e instanceof HttpTimeoutException timeout)
                    throw timeout;
                try {
                    checkDeadline();
                } catch (HttpTimeoutException timeout) {
                    timeout.initCause(e);
                    throw timeout;
                }
                throw e;
            }
        }

        private <T> HttpResponse<T> execute(HttpRequest request, BodyHandler<T> bodyHandler)
            throws IOException, InterruptedException {

            if (index < interceptors.size()) {
                int current = index++;
                return interceptors.get(current).intercept(request, bodyHandler, current, this);
            }

            if (deadline != null) {
                var remaining = Duration.ofNanos(deadline.remainingNanos());
                var attemptTimeout = request.timeout().filter(timeout -> timeout.compareTo(remaining) < 0).orElse(remaining);
                request = HttpRequest.newBuilder(request, (name, value) -> true).timeout(attemptTimeout).build();
            }

            var errorBody = new AtomicReference<String>();
            var httpResponse = client.send(request, responseInfo -> {

                int status = responseInfo.statusCode();
                if (status >= 200 && status < 300)
                    return bodyHandler.apply(responseInfo);

                return BodySubscribers.mapping(BodyHandlers.ofString().apply(responseInfo), body -> {
                    errorBody.set(body);
                    return null;
                });
            });

            int statusCode = httpResponse.statusCode();

            if (statusCode >= 200 && statusCode < 300)
                return httpResponse;

            String body = errorBody.get();
            if (body == null) {
                try {
                    body = extractErrorBody(httpResponse).orElse(null);
                } catch (IOException | RuntimeException cause) {
                    throw new WatsonxException("Status code: " + statusCode, statusCode, null, cause);
                }
            }
            throw HttpErrorMapper.toException(statusCode, httpResponse.headers(), body);
        }

        private static Optional<String> extractErrorBody(HttpResponse<?> response) throws IOException {
            if (response.body() instanceof String body)
                return Optional.of(body);

            if (response.body() instanceof InputStream stream) {
                try (stream) {
                    return HttpUtils.extractBodyAsString(response);
                }
            }

            if (response.body() instanceof Stream<?> stream) {
                try (stream) {
                    return HttpUtils.extractBodyAsString(response);
                }
            }

            return HttpUtils.extractBodyAsString(response);
        }

        @Override
        public void resetToIndex(int index) {
            this.index = index;
        }

        @Override
        public void pause(Duration duration) throws IOException, InterruptedException {
            if (deadline == null)
                SyncHttpInterceptor.Chain.super.pause(duration);
            else
                deadline.pause(duration);
        }

        @Override
        public void checkDeadline() throws HttpTimeoutException {
            if (deadline != null)
                deadline.remainingNanos();
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long nanos) throws InterruptedException;
    }

    private static final class Deadline {
        private final long startedAt;
        private final long budgetNanos;
        private final LongSupplier nanoTime;
        private final Sleeper sleeper;

        Deadline(Duration budget, LongSupplier nanoTime, Sleeper sleeper) {
            this.nanoTime = nanoTime;
            this.sleeper = sleeper;
            this.startedAt = nanoTime.getAsLong();
            long nanos;
            try {
                nanos = budget.toNanos();
            } catch (ArithmeticException e) {
                nanos = Long.MAX_VALUE;
            }
            this.budgetNanos = nanos;
        }

        long remainingNanos() throws HttpTimeoutException {
            var remaining = budgetNanos - (nanoTime.getAsLong() - startedAt);
            if (remaining <= 0)
                throw new HttpTimeoutException("The HTTP operation exceeded its time budget");
            return remaining;
        }

        void pause(Duration duration) throws HttpTimeoutException, InterruptedException {
            var remaining = remainingNanos();
            var delay = duration.compareTo(Duration.ofNanos(remaining)) < 0 ? duration.toNanos() : remaining;
            sleeper.sleep(delay);
            remainingNanos();
        }
    }

    /**
     * Builder for {@link SyncHttpClient}.
     */
    public final static class Builder {

        private HttpClient httpClient;
        private List<SyncHttpInterceptor> interceptors;

        private Builder() {}

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
            this.interceptors = isNull(interceptors) ? null : new ArrayList<>(interceptors);
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
