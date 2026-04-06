/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.interceptors;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;

/**
 * Interceptor that adds an Authorization header to outgoing HTTP requests.
 */
public final class AuthenticationInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

    private final Authenticator authenticator;

    /**
     * Constructs a new AuthenticationInterceptor with the given authenticator.
     *
     * @param authenticator the authenticator used to retrieve authentication tokens
     */
    public AuthenticationInterceptor(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, AsyncChain chain) {
        return authenticator.tokenAsync()
            .thenCompose(token -> chain.proceed(requestWithAuthHeader(request, token), bodyHandler));
    }

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
        throws WatsonxException, IOException, InterruptedException {
        var token = authenticator.token();
        return chain.proceed(requestWithAuthHeader(request, token), bodyHandler);
    }

    // Creates a copy of the given request with the appropriate Authorization header.
    private HttpRequest requestWithAuthHeader(HttpRequest request, String token) {
        var authorization = "%s %s".formatted(authenticator.scheme(), token);
        return HttpRequest.newBuilder(request, (key, value) -> true)
            .header("Authorization", authorization)
            .build();
    }
}
