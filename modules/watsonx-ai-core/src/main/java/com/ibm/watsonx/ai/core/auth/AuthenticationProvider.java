/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * This interface defines a contract for obtaining a token that can be used to authorize or identify a request.
 */
public interface AuthenticationProvider {

  /**
   * Returns a token.
   *
   * @return Token.
   */
  String getToken();

  /**
   * Asynchronously retrieves a token.
   *
   * @return a {@link CompletableFuture} that will complete with the token
   */
  default CompletableFuture<String> getTokenAsync() {
    return getTokenAsync(null);
  }

  /**
   * Asynchronously retrieves a token.
   *
   * @param executor the executor that is used for executing asynchronous tasks
   * @return a {@link CompletableFuture} that will complete with the token
   */
  CompletableFuture<String> getTokenAsync(Executor executor);
}
