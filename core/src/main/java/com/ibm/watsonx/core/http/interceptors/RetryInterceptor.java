package com.ibm.watsonx.core.http.interceptors;

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
import com.ibm.watsonx.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.core.http.SyncHttpInterceptor;

/**
 * An HTTP interceptor that performs automatic retries when configured exceptions are thrown.
 */
public class RetryInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

    private record RetryOn(Class<? extends Throwable> clazz, Optional<Function<Throwable, Boolean>> predicate) {}

    private final Duration retryInterval;
    private final List<RetryOn> retryOn;
    private Integer maxRetries;

    /**
     * Creates a new {@code RetryInterceptor} using the provided builder.
     *
     * @param builder the builder instance
     */
    public RetryInterceptor(Builder builder) {
        requireNonNull(builder);
        this.retryInterval = requireNonNullElse(builder.retryInterval, Duration.ofMillis(0));
        this.maxRetries = requireNonNullElse(builder.maxRetries, 1);
        this.retryOn = builder.retryOn;
        if (isNull(retryOn) || retryOn.isEmpty())
            throw new RuntimeException("At least one exception must be specified");
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
        int index, AsyncChain chain) {
        return executeWithRetry(request, bodyHandler, index, 0, chain);
    }

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
        throws IOException, InterruptedException {

        Throwable exception = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {

            try {

                if (attempt > 0)
                    Thread.sleep(retryInterval.toMillis());

                return chain.proceed(request, bodyHandler);

            } catch (Exception e) {
                exception = e;

                var shouldRetry =
                    retryOn.stream().anyMatch(retryOn -> {
                        if (!retryOn.clazz().equals(e.getClass()))
                            return false;
                        return retryOn.predicate().map(p -> p.apply(e)).orElse(true);
                    });

                if (shouldRetry) {
                    chain.resetToIndex(index + 1);
                    continue;
                }

                throw e;
            }
        }

        throw new RuntimeException("Max retries reached", isNull(exception) ? new Exception() : exception);
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    private <T> CompletableFuture<HttpResponse<T>> executeWithRetry(HttpRequest request, BodyHandler<T> bodyHandler,
        int index, int attempt, AsyncChain chain) {

        return chain.proceed(request, bodyHandler)
            .handleAsync((response, throwable) -> {
                if (throwable == null) {
                    return CompletableFuture.completedFuture(response);
                }

                Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

                var shouldRetry =
                    retryOn.stream().anyMatch(retryOn -> {
                        if (!retryOn.clazz().equals(cause.getClass()))
                            return false;
                        return retryOn.predicate().map(p -> p.apply(cause)).orElse(true);
                    });

                if (!shouldRetry || attempt >= maxRetries - 1) {
                    CompletableFuture<HttpResponse<T>> failed = new CompletableFuture<>();
                    failed.completeExceptionally(new RuntimeException("Max retries reached", cause));
                    return failed;
                }

                return CompletableFuture.supplyAsync(
                    () -> {
                        chain.resetToIndex(index + 1);
                        return executeWithRetry(request, bodyHandler, index, attempt + 1, chain);
                    },
                    CompletableFuture.delayedExecutor(retryInterval.toMillis(), TimeUnit.MILLISECONDS)
                ).thenCompose(Function.identity());

            }).thenCompose(Function.identity());
    }

    /**
     * Builder for {@link RetryInterceptor}.
     */
    public static class Builder {
        private Duration retryInterval;
        private Integer maxRetries;
        private List<RetryOn> retryOn;

        /**
         * Sets the delay between retry attempts.
         *
         * @param retryInterval the duration to wait between retries
         */
        public Builder retryInterval(Duration retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        /**
         * Sets the maximum number of retry attempts.
         *
         * @param maxRetries the number of retries
         */
        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Adds a retry condition based on exception class.
         *
         * @param clazz the exception type to retry on
         */
        public Builder retryOn(Class<? extends Throwable> clazz) {
            retryOn(clazz, null);
            return this;
        }

        /**
         * Adds a retry condition based on exception class and optional predicate.
         *
         * @param clazz the exception type to retry on
         * @param predicate optional predicate to evaluate retry eligibility
         */
        public Builder retryOn(Class<? extends Throwable> clazz, Function<Throwable, Boolean> predicate) {
            requireNonNull(clazz);
            retryOn = requireNonNullElse(retryOn, new ArrayList<>());
            retryOn.add(new RetryOn(clazz, Optional.ofNullable(predicate)));
            return this;
        }

        /**
         * Builds a new {@code RetryInterceptor} with the configured parameters.
         *
         * @return a new {@code RetryInterceptor}
         */
        public RetryInterceptor build() {
            return new RetryInterceptor(this);
        }
    }
}
