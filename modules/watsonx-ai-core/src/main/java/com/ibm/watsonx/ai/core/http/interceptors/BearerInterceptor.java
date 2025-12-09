/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.interceptors;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.cp4d.AuthMode;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;

/**
 * Interceptor that adds a Bearer token to outgoing requests.
 */
public final class BearerInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

    private final Authenticator authenticator;

    /**
     * Constructs a new BearerInterceptor with the given authenticator.
     *
     * @param authenticator the authenticator used to retrieve bearer tokens
     */
    public BearerInterceptor(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, AsyncChain chain) {
        return authenticator.asyncToken()
            .thenCompose(token -> chain.proceed(requestWithBearer(request, token), bodyHandler));
    }

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
        throws WatsonxException, IOException, InterruptedException {
        var token = authenticator.token();
        return chain.proceed(requestWithBearer(request, token), bodyHandler);
    }

    // Creates a copy of the given request with the Authorization header set to use the Bearer token.
    private HttpRequest requestWithBearer(HttpRequest request, String token) {
        var authorization = authenticator instanceof CP4DAuthenticator auth && auth.isAuthMode(AuthMode.ZEN_API_KEY)
            ? "ZenApiKey %s".formatted(token)
            : "Bearer %s".formatted(token);
        return HttpRequest.newBuilder(request, (key, value) -> true)
            .header("Authorization", authorization)
            .build();
    }
}
