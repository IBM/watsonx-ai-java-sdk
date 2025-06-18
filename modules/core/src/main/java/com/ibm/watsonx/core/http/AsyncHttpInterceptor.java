/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.core.http;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import com.ibm.watsonx.core.exeception.WatsonxException;
import com.ibm.watsonx.core.http.interceptors.LoggerInterceptor;

/**
 * A functional interface representing a asynchronous HTTP interceptor.
 *
 * @see LoggerInterceptor
 */
public interface AsyncHttpInterceptor {

  /**
   * Intercepts the given HTTP request.
   *
   * @param request the HTTP request to intercept
   * @param bodyHandler the body handler for processing the response
   * @param executor the executor that is used for executing asynchronous tasks
   * @param chain the chain used to proceed with the execution of the request
   * @param index the current index in the interceptor chain
   * @param <T> the type of the response body
   * @return the {@link CompletableFuture} of the HTTP response
   * @throws WatsonxException if an error occurs during the request api
   * @throws IOException if an I/O error occurs during interception or execution
   * @throws InterruptedException if the operation is interrupted
   */
  <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
    Executor executor, int index, AsyncChain chain);

  /**
   * Chain interface for proceeding with the asynchronous HTTP request.
   */
  interface AsyncChain {

    /**
     * Proceeds with the execution of the async HTTP request.
     *
     * @param request the HTTP request to execute
     * @param bodyHandler the body handler for processing the response
     * @param executor the executor that is used for executing asynchronous tasks
     * @param <T> the type of the response body
     * @return the {@link CompletableFuture} of the HTTP response
     * @throws WatsonxException if an error occurs during the request api
     * @throws IOException if an I/O error occurs during execution
     * @throws InterruptedException if the operation is interrupted
     */
    <T> CompletableFuture<HttpResponse<T>> proceed(HttpRequest request, BodyHandler<T> handler, Executor executor);

    /**
     * Resets the interceptor chain to a specific index.
     * <p>
     * Typically used by interceptors such as retries to restart the chain execution from a given position.
     *
     * @param index the interceptor index to reset to
     */
    void resetToIndex(int index);
  }
}
