/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.core.http;

import static com.ibm.watsonx.core.Json.fromJson;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import com.ibm.watsonx.core.HttpUtils;
import com.ibm.watsonx.core.exeception.WatsonxException;
import com.ibm.watsonx.core.exeception.model.WatsonxError;

/**
 * Asynchronous HTTP client.
 *
 * @see AsyncHttpInterceptor
 * @see HttpClient
 */
public class AsyncHttpClient extends BaseHttpClient {

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
    return send(request, handler, executor());
  }

  /**
   * Sends an asynchronous HTTP request.
   *
   * @param request the HTTP request to send
   * @param handler the body handler for the response
   * @param executor the executor that is used for executing asynchronous and dependent tasks.
   * @param <T> the type of the response body
   * @return a {@link CompletableFuture} of the HTTP response
   */
  public <T> CompletableFuture<HttpResponse<T>> send(HttpRequest request, BodyHandler<T> handler, Executor executor) {
    return new InterceptorChain(delegate, interceptors).proceed(request, handler,
      requireNonNullElse(executor, executor()));
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
    public <T> CompletableFuture<HttpResponse<T>> proceed(HttpRequest request, BodyHandler<T> handler,
      Executor executor) {
      if (index < interceptors.size()) {
        var interceptorIndex = index++;
        return interceptors.get(interceptorIndex).intercept(request, handler, executor, interceptorIndex, this);
      } else {
        return httpClient.sendAsync(request, handler)
          .handleAsync((response, exception) -> {
            if (exception != null)
              throw new CompletionException(exception);


            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
              return response;
            }

            var bodyOpt = HttpUtils.extractBodyAsString(response);

            if (bodyOpt.isEmpty())
              throw new CompletionException(new WatsonxException(statusCode));

            String body = bodyOpt.get();
            WatsonxError details;

            details = fromJson(body, WatsonxError.class);
            throw new CompletionException(new WatsonxException(body, statusCode, details));
          }, requireNonNullElse(executor, httpClient.executor().orElse(ForkJoinPool.commonPool())));
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
     * Sets the {@link HttpClient}.
     *
     * @param httpClient the HTTP client instance
     */
    public Builder httpClient(HttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    /**
     * Adds a single {@link AsyncHttpInterceptor}.
     *
     * @param interceptor the interceptor to add
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
