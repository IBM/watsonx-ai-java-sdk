/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;

/**
 * A functional interface representing a synchronous HTTP interceptor.
 *
 * @see LoggerInterceptor
 */
public interface SyncHttpInterceptor {

  /**
   * Intercepts the given HTTP request.
   *
   * @param request the HTTP request to intercept
   * @param bodyHandler the body handler for processing the response
   * @param index the current index in the interceptor chain
   * @param chain the chain used to proceed with the execution of the request
   * @param <T> the type of the response body
   * @return the HTTP response
   * @throws WatsonxException if an error occurs during the request api
   * @throws IOException if an I/O error occurs during interception or execution
   * @throws InterruptedException if the operation is interrupted
   */
  <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
    throws WatsonxException, IOException, InterruptedException;

  /**
   * Chain interface for proceeding with the HTTP request.
   */
  interface Chain {

    /**
     * Proceeds with the execution of the HTTP request.
     *
     * @param request the HTTP request to execute
     * @param bodyHandler the body handler for processing the response
     * @param <T> the type of the response body
     * @return the HTTP response
     * @throws WatsonxException if an error occurs during the request api
     * @throws IOException if an I/O error occurs during execution
     * @throws InterruptedException if the operation is interrupted
     */
    <T> HttpResponse<T> proceed(HttpRequest request, BodyHandler<T> bodyHandler)
      throws WatsonxException, IOException, InterruptedException;

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
