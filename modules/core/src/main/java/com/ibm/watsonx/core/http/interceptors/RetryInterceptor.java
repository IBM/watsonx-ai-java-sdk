/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.core.exeception.WatsonxException;
import com.ibm.watsonx.core.exeception.model.WatsonxError;
import com.ibm.watsonx.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.core.http.SyncHttpInterceptor;

/**
 * An HTTP interceptor that performs automatic retries when configured exceptions are thrown.
 */
public class RetryInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(RetryInterceptor.class);

  /**
   * Checks whether a {@link WatsonxException} is retryable due to an expired authentication token.
   * <p>
   * This condition is met if the HTTP status code is 401 and at least one error in the exception's details has the code
   * {@code AUTHENTICATION_TOKEN_EXPIRED}.
   */
  public static RetryInterceptor onTokenExpired(int maxRetries) {
    return RetryInterceptor.builder()
      .maxRetries(maxRetries)
      .retryOn(
        WatsonxException.class,
        ex -> {
          var e = (WatsonxException) ex;
          return e.statusCode() == 401 && e.details().map(detail -> detail.errors().stream()
            .anyMatch(err -> err.is(WatsonxError.Code.AUTHENTICATION_TOKEN_EXPIRED))).orElse(false);
        }
      ).build();
  }

  private record RetryOn(Class<? extends Throwable> clazz, Optional<Predicate<Throwable>> predicate) {
  }

  private final Duration retryInterval;
  private final List<RetryOn> retryOn;
  private final boolean exponentialBackoff;
  private Integer maxRetries;
  private Duration timeout;

  /**
   * Creates a new {@code RetryInterceptor} using the provided builder.
   *
   * @param builder the builder instance
   */
  public RetryInterceptor(Builder builder) {
    requireNonNull(builder);
    this.retryInterval = requireNonNullElse(builder.retryInterval, Duration.ofMillis(0));
    this.timeout = this.retryInterval;
    this.maxRetries = requireNonNullElse(builder.maxRetries, 1);
    this.retryOn = builder.retryOn;
    this.exponentialBackoff = builder.exponentialBackoff;
    if (isNull(retryOn) || retryOn.isEmpty())
      throw new RuntimeException("At least one exception must be specified");
  }

  @Override
  public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
    throws WatsonxException, IOException, InterruptedException {

    Throwable exception = null;

    for (int attempt = 0; attempt < maxRetries; attempt++) {

      try {


        if (attempt > 0)
          Thread.sleep(timeout.toMillis());

        var res = chain.proceed(request, bodyHandler);
        this.timeout = this.retryInterval;
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
          logger.debug("Retrying request ({}/{}) after failure: {}", attempt + 1, maxRetries,
            exception.getMessage());
          chain.resetToIndex(index + 1);
          continue;
        }

        this.timeout = this.retryInterval;
        throw e;
      }
    }

    this.timeout = this.retryInterval;
    logger.debug("Max retries reached");

    throw new RuntimeException("Max retries reached", isNull(exception) ? new Exception() : exception);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
    Executor executor, int index, AsyncChain chain) {
    return executeWithRetry(request, bodyHandler, executor, index, 0, chain);
  }

  /**
   * The current timeout interval.
   */
  public Duration getTimeout() {
    return timeout;
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
    Executor executor, int index, int attempt, AsyncChain chain) {

    return chain.proceed(request, bodyHandler, executor)
      .handleAsync((response, throwable) -> {
        if (throwable == null) {
          this.timeout = this.retryInterval;
          return CompletableFuture.completedFuture(response);
        }

        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

        var shouldRetry =
          retryOn.stream().anyMatch(retryOn -> {
            if (!retryOn.clazz().equals(cause.getClass()))
              return false;
            return retryOn.predicate()
              .map(p -> p.test(cause))
              .orElse(true);
          });

        if (!shouldRetry || attempt >= maxRetries - 1) {
          CompletableFuture<HttpResponse<T>> failed = new CompletableFuture<>();
          logger.debug("Retrying request ({}/{}) after failure: {}", attempt + 1, maxRetries, cause.getMessage());
          logger.debug("Max retries reached");
          failed.completeExceptionally(new RuntimeException("Max retries reached", cause));
          this.timeout = this.retryInterval;
          return failed;
        }

        if (attempt > 0) {
          timeout = timeout.multipliedBy(2);
        }
        logger.debug("Retrying request ({}/{}) after failure: {}", attempt + 1, maxRetries, cause.getMessage());

        return CompletableFuture.supplyAsync(
          () -> {
            chain.resetToIndex(index + 1);
            return executeWithRetry(request, bodyHandler, executor, index, attempt + 1, chain);
          },
          CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS, executor)
        ).thenCompose(Function.identity());

      }, executor).thenCompose(Function.identity());
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
    public Builder retryOn(Class<? extends Throwable> clazz, Predicate<Throwable> predicate) {
      requireNonNull(clazz);
      retryOn = requireNonNullElse(retryOn, new ArrayList<>());
      retryOn.add(new RetryOn(clazz, Optional.ofNullable(predicate)));
      return this;
    }

    /**
     * Wether to use exponential backoff in retries or not
     *
     * @param enable wether to enable exponential backoff
     */
    public Builder exponentialBackoff(boolean enable) {
      exponentialBackoff = enable;
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
