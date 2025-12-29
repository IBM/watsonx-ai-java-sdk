/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.UUID;

/**
 * The abstract base class for all HTTP clients
 *
 * @see SyncHttpClient
 * @see AsyncHttpClient
 */
public abstract class BaseHttpClient {

    public static final String REQUEST_ID_HEADER = "Watsonx-AI-SDK-Request-Id";

    final HttpClient delegate;

    /**
     * Constructs a new instance of BaseHttpClient with the provided HttpClient delegate.
     *
     * @param httpClient {@link HttpClient} instance.
     */
    protected BaseHttpClient(HttpClient httpClient) {
        this.delegate = httpClient;
    }

    /**
     * Adds a {@code Watsonx-AI-SDK-Request-Id} header to the given HTTP request if it is not already present.
     *
     * @param request the HTTP request to which the header will be added
     * @return the HTTP request with the added or existing request ID header
     */
    protected HttpRequest addRequestIdHeaderIfNotPresent(HttpRequest request) {
        return request.headers()
            .firstValue(REQUEST_ID_HEADER).map(h -> request)
            .orElse(HttpRequest.newBuilder(request, (k, v) -> true)
                .header(REQUEST_ID_HEADER, UUID.randomUUID().toString())
                .build());
    }
}
