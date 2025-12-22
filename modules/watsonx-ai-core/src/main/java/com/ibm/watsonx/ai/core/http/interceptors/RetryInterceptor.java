/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.interceptors;

import static com.ibm.watsonx.ai.core.http.BaseHttpClient.REQUEST_ID_HEADER;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.core.RetryConfig;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * An HTTP interceptor that performs automatic retries.
 */
public final class RetryInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RetryInterceptor.class);

    public record RetryOn(Class<? extends Throwable> clazz, Optional<Predicate<Throwable>> predicate) {}

    private final Duration retryInterval;
    private final List<RetryOn> retryOn;
    private final boolean exponentialBackoff;
    private final Integer maxRetries;

    /**
     * Checks whether a {@link WatsonxException} is retryable due to an expired authentication token.
     */
    public static final RetryInterceptor ON_TOKEN_EXPIRED = RetryInterceptor.builder()
        .maxRetries(RetryConfig.tokenExpiredMaxRetries())
        .retryOn(
            WatsonxException.class,
            ex -> {
                var e = (WatsonxException) ex;
                boolean watsonxTokenExpired = e.statusCode() == 401 && e.details().map(detail -> detail.errors().stream()
                    .anyMatch(err -> err.is(WatsonxError.Code.AUTHENTICATION_TOKEN_EXPIRED))).orElse(false);
                boolean cosTokenExpired = e.statusCode() == 403 && e.details().map(detail -> detail.errors().stream()
                    .anyMatch(err -> err.is(WatsonxError.Code.COS_ACCESS_DENIED))).orElse(false);
                return watsonxTokenExpired || cosTokenExpired;
            }
        ).build();

    /**
     * A {@link RetryInterceptor} that retries requests when a {@link WatsonxException} is thrown with one of the following transient HTTP status
     * codes:
     * <ul>
     * <li><code>429</code> — Too Many Requests</li>
     * <li><code>503</code> — Service Unavailable</li>
     * <li><code>504</code> — Gateway Timeout</li>
     * <li><code>520</code> — Unknown Error</li>
     * </ul>
     */
    public static final RetryInterceptor ON_RETRYABLE_STATUS_CODES = RetryInterceptor.builder()
        .maxRetries(RetryConfig.statusCodesMaxRetries())
        .exponentialBackoff(RetryConfig.statusCodesExponentialBackoffEnabled())
        .retryInterval(RetryConfig.statusCodesInitialRetryInterval())
        .retryOn(
            WatsonxException.class,
            ex -> {
                var statusCode = ((WatsonxException) ex).statusCode();
                return statusCode == 429 || statusCode == 503 || statusCode == 504 || statusCode == 520
                    ? true
                    : false;
            }
        ).build();

    /**
     * Creates a new {@code RetryInterceptor} using the provided builder.
     *
     * @param builder the builder instance
     */
    private RetryInterceptor(Builder builder) {
        requireNonNull(builder);
        retryInterval = requireNonNullElse(builder.retryInterval, Duration.ofMillis(0));
        maxRetries = requireNonNullElse(builder.maxRetries, 1);
        retryOn = requireNonNull(builder.retryOn, "At least one exception must be specified");
        exponentialBackoff = builder.exponentialBackoff;
        if (exponentialBackoff && retryInterval.isZero())
            throw new IllegalArgumentException("Retry interval must be positive when exponential backoff is enabled");
    }

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
        throws WatsonxException, IOException, InterruptedException {

        Throwable exception = null;

        String requestId = request.headers()
            .firstValue(REQUEST_ID_HEADER)
            .orElseThrow(); // This should never happen. The SyncHttpClient and AsyncHttpClient add this header if it is not present.

        Duration timeout = Duration.from(retryInterval);

        for (int attempt = 0; attempt <= maxRetries; attempt++) {

            try {

                if (attempt > 0) {
                    logger.debug("Retrying request \"{}\" ({}/{}) after failure: {}", requestId, attempt, maxRetries,
                        exception.getMessage());
                }

                if (attempt > 0 && !timeout.isZero()) {
                    logger.debug("Retry request \"{}\" after {} ms", requestId, timeout.toMillis());
                    Thread.sleep(timeout.toMillis());
                }

                var res = chain.proceed(request, bodyHandler);
                timeout = Duration.from(retryInterval);
                return res;

            } catch (Exception e) {
                exception = e;

                var shouldRetry =
                    retryOn.stream().anyMatch(retryOn -> {
                        if (!retryOn.clazz().equals(e.getClass()))
                            return false;
                        return retryOn.predicate()
                            .map(p -> p.test(e))
                            .orElse(true);
                    });

                if (shouldRetry) {
                    if (exponentialBackoff && attempt > 0) {
                        timeout = timeout.multipliedBy(2);
                    }
                    chain.resetToIndex(index + 1);
                    continue;
                }

                timeout = Duration.from(retryInterval);
                throw e;
            }
        }

        timeout = Duration.from(retryInterval);
        throw new RuntimeException("Max retries reached for request [%s]".formatted(requestId), isNull(exception) ? new Exception() : exception);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, AsyncChain chain) {
        return executeWithRetry(request, bodyHandler, index, 0, Duration.from(retryInterval), chain);
    }

    private <T> CompletableFuture<HttpResponse<T>> executeWithRetry(HttpRequest request, BodyHandler<T> bodyHandler, int index, int attempt,
        Duration timeout, AsyncChain chain) {

        return chain.proceed(request, bodyHandler)
            .exceptionallyComposeAsync(throwable -> {

                String requestId = request.headers()
                    .firstValue(REQUEST_ID_HEADER)
                    .orElseThrow(); // This should never happen. The SyncHttpClient and AsyncHttpClient add this header if it is not present.

                Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

                var shouldRetry = retryOn.stream().anyMatch(retry -> retry.clazz().equals(cause.getClass()) &&
                    retry.predicate().map(p -> p.test(cause)).orElse(true)
                );

                var shouldFail = !shouldRetry || attempt >= maxRetries;

                if (shouldFail) {

                    if (attempt >= maxRetries)
                        logger.debug("Max retries ({}) reached for request \"{}\"", maxRetries, requestId);

                    CompletableFuture<HttpResponse<T>> failed = new CompletableFuture<>();
                    failed.completeExceptionally(cause);
                    return failed;
                }

                Duration nextTimeout = exponentialBackoff ? timeout.multipliedBy(2) : timeout;

                if (!timeout.isZero())
                    logger.debug("Retry request \"{}\" after {} ms", requestId, nextTimeout.toMillis());

                return CompletableFuture.supplyAsync(
                    () -> {
                        logger.debug("Retrying request \"{}\" ({}/{}) after failure: {}", requestId, attempt + 1, maxRetries, cause.getMessage());
                        chain.resetToIndex(index + 1);
                        return executeWithRetry(request, bodyHandler, index, attempt + 1, nextTimeout, chain);
                    },
                    CompletableFuture.delayedExecutor(nextTimeout.toMillis(), TimeUnit.MILLISECONDS, ExecutorProvider.ioExecutor())
                ).thenCompose(Function.identity());
            }, ExecutorProvider.ioExecutor());
    }

    public List<RetryOn> retryOn() {
        return retryOn;
    }

    public int maxRetries() {
        return maxRetries;
    }

    public Duration retryInterval() {
        return retryInterval;
    }

    public boolean exponentialBackoff() {
        return exponentialBackoff;
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
     * Builder for {@link RetryInterceptor}.
     */
    public static class Builder {
        private Duration retryInterval;
        private Integer maxRetries;
        private List<RetryOn> retryOn;
        private boolean exponentialBackoff = false;

        /**
         * Prevents direct instantiation of the {@code Builder}.
         */
        private Builder() {}

        /**
         * Sets the delay between retry attempts.
         *
         * @param retryInterval the duration to wait between retries.
         * @return {@code Builder} instance for method chaining.
         */
        public Builder retryInterval(Duration retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        /**
         * Sets the maximum number of retry attempts.
         *
         * @param maxRetries the number of retries.
         * @return {@code Builder} instance for method chaining.
         */
        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Adds a retry condition based on exception class.
         *
         * @param clazz the exception type to retry on.
         * @return {@code Builder} instance for method chaining.
         */
        public Builder retryOn(Class<? extends Throwable> clazz) {
            retryOn(clazz, null);
            return this;
        }

        /**
         * Adds a retry condition based on exception class and optional predicate.
         *
         * @param clazz the exception type to retry on.
         * @param predicate optional predicate to evaluate retry eligibility.
         * @return {@code Builder} instance for method chaining.
         */
        public Builder retryOn(Class<? extends Throwable> clazz, Predicate<Throwable> predicate) {
            requireNonNull(clazz);
            retryOn = requireNonNullElse(retryOn, new ArrayList<>());
            retryOn.add(new RetryOn(clazz, Optional.ofNullable(predicate)));
            return this;
        }

        /**
         * Wether to use exponential backoff in retries or not
         *
         * @param enable wether to enable exponential backoff.
         * @return {@code Builder} instance for method chaining.
         */
        public Builder exponentialBackoff(boolean enable) {
            exponentialBackoff = enable;
            return this;
        }

        /**
         * Builds a new {@code RetryInterceptor} with the configured parameters.
         *
         * @return a new {@code RetryInterceptor} instance.
         */
        public RetryInterceptor build() {
            return new RetryInterceptor(this);
        }
    }
}
