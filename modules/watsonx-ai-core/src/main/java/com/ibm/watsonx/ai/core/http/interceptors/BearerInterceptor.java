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
import java.util.concurrent.Executor;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;

/**
 * Interceptor that adds a Bearer token to outgoing requests.
 */
public class BearerInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

  private final AuthenticationProvider authenticator;

  /**
   * Constructs a new BearerInterceptor with the given authenticator.
   *
   * @param authenticator the authenticator used to retrieve bearer tokens
   */
  public BearerInterceptor(AuthenticationProvider authenticator) {
    this.authenticator = authenticator;
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
    Executor executor, int index, AsyncChain chain) {
    return authenticator.getTokenAsync()
      .thenComposeAsync(token -> chain.proceed(requestWithBearer(request, token), bodyHandler, executor), executor);
  }

  @Override
  public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
    throws WatsonxException, IOException, InterruptedException {
    var token = authenticator.getToken();
    return chain.proceed(requestWithBearer(request, token), bodyHandler);
  }

  // Creates a copy of the given request with the Authorization header set to use the Bearer token.
  private HttpRequest requestWithBearer(HttpRequest request, String token) {
    return HttpRequest.newBuilder(request, (key, value) -> true)
      .header("Authorization", "Bearer %s".formatted(token))
      .build();
  }
}
