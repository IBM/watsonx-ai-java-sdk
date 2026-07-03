/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;

/**
 * A functional interface representing an asynchronous HTTP interceptor.
 *
 * @see LoggerInterceptor
 */
public interface AsyncHttpInterceptor {

    /**
     * Intercepts the given HTTP request.
     *
     * @param request the HTTP request to intercept
     * @param bodyHandler the body handler for processing the response
     * @param chain the chain used to proceed with the execution of the request
     * @param index the current index in the interceptor chain
     * @param <T> the type of the response body
     * @return the {@link CompletableFuture} of the HTTP response
     * @throws WatsonxException if an error occurs during the request api
     */
    <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, AsyncChain chain);

    /**
     * Chain interface for proceeding with the asynchronous HTTP request.
     */
    interface AsyncChain {

        /**
         * Proceeds with the execution of the async HTTP request.
         *
         * @param request the HTTP request to execute
         * @param handler the body handler for processing the response
         * @param <T> the type of the response body
         * @return the {@link CompletableFuture} of the HTTP response
         * @throws WatsonxException if an error occurs during the request api
         */
        <T> CompletableFuture<HttpResponse<T>> proceed(HttpRequest request, BodyHandler<T> handler);

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
