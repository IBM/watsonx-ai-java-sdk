/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.core.http;

import java.net.http.HttpClient;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * The abstract base class for all HTTP client
 *
 * @see SyncHttpClient
 * @see AsyncHttpClient
 */
public abstract class BaseHttpClient {

  final HttpClient delegate;

  /**
   * Constructs a new instance of BaseHttpClient with the provided HttpClient delegate.
   *
   * @param httpClient {@link HttpClient} instance.
   */
  public BaseHttpClient(HttpClient httpClient) {
    this.delegate = httpClient;
  }

  /**
   * Returns the {@code Executor} of the HttpClient.
   *
   * @return {@link Executor}
   */
  public Executor executor() {
    return delegate.executor().orElse(ForkJoinPool.commonPool());
  }
}
