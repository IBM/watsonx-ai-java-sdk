/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.logging;

import java.net.URI;
import java.net.http.HttpHeaders;
import com.ibm.watsonx.ai.core.internal.logging.LogSanitizer;

/**
 * Structured, already-processed data for a single HTTP response or SSE event, passed to a {@link HttpResponseLogger}.
 */
public interface HttpResponseLog {

    /**
     * Returns the {@code Watsonx-AI-SDK-Request-Id} header value of the originating request.
     *
     * @return the request id, or {@code null} if not tracked in this context
     */
    String requestId();

    /**
     * Returns the HTTP status code.
     *
     * @return the status code, or {@code -1} when {@link #error()} is non-null and no response was received
     */
    int statusCode();

    /**
     * Returns a snapshot of the response headers with sensitive values replaced in full.
     *
     * @return the headers, or {@code null} when {@link #error()} is non-null or when not tracked in this context
     */
    HttpHeaders headers();

    /**
     * Returns the request URI with user information, fragments and sensitive query values removed or masked.
     *
     * @return the URI, or {@code null} when {@link #error()} is non-null or when not tracked in this context
     */
    URI uri();

    /**
     * Returns the response body, with secrets masked.
     *
     * @return the processed body, or {@code null} for bodies that cannot be inspected safely. SSE entries contain the processed event
     */
    String body();

    /**
     * Returns a diagnostic snapshot of the transport/API error. Its message is sanitized and its original type, cause, suppressed exceptions and
     * subtype-specific accessors are not retained. Code locations from the original stack trace are copied. The error delivered to the caller is
     * unchanged.
     *
     * @return the error, or {@code null} on success
     */
    Throwable error();

    /**
     * Creates a new {@code HttpResponseLog}.
     *
     * @param requestId the {@code Watsonx-AI-SDK-Request-Id} header value
     * @param statusCode the HTTP status code
     * @param headers the response headers
     * @param uri the request URI
     * @param body the processed response body
     * @param error the transport/API error, if any
     * @return a new {@code HttpResponseLog}
     */
    static HttpResponseLog of(String requestId, int statusCode, HttpHeaders headers, URI uri, String body, Throwable error) {
        var processedHeaders = LogSanitizer.headers(headers);
        var processedUri = LogSanitizer.uri(uri);
        var processedBody = LogSanitizer.body(body);
        var processedError = LogSanitizer.error(error);
        return new HttpResponseLog() {
            @Override
            public String requestId() {
                return requestId;
            }

            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpHeaders headers() {
                return processedHeaders;
            }

            @Override
            public URI uri() {
                return processedUri;
            }

            @Override
            public String body() {
                return processedBody;
            }

            @Override
            public Throwable error() {
                return processedError;
            }
        };
    }
}
