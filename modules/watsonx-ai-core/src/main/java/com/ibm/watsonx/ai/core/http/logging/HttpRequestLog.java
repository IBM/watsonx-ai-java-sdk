/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.logging;

import java.net.http.HttpRequest;

/**
 * Structured, already-processed data for a single outgoing HTTP request, passed to a {@link HttpRequestLogger}.
 */
public interface HttpRequestLog {

    /**
     * Returns the outgoing HTTP request (method, URI, headers).
     *
     * @return the request
     */
    HttpRequest request();

    /**
     * Returns the request body, with secrets masked and base64 payloads truncated.
     *
     * @return the processed body, or {@code null} if the request has no body
     */
    String body();

    /**
     * Creates a new {@code HttpRequestLog}.
     *
     * @param request the outgoing HTTP request
     * @param body the processed request body
     * @return a new {@code HttpRequestLog}
     */
    static HttpRequestLog of(HttpRequest request, String body) {
        return new HttpRequestLog() {
            @Override
            public HttpRequest request() {
                return request;
            }

            @Override
            public String body() {
                return body;
            }
        };
    }
}
