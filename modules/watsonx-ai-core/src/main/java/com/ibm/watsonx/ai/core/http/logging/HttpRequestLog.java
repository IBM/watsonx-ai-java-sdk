/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.logging;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Optional;
import com.ibm.watsonx.ai.core.internal.logging.LogSanitizer;

/**
 * Structured, already-processed data for a single outgoing HTTP request, passed to a {@link HttpRequestLogger}.
 */
public interface HttpRequestLog {

    /**
     * Returns a diagnostic snapshot of the outgoing request, with a sanitized URI and headers. The snapshot has no body publisher and cannot expose
     * the original payload. Read the processed body through {@link #body()}.
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
        var method = request.method();
        var uri = LogSanitizer.uri(request.uri());
        var headers = LogSanitizer.headers(request.headers());
        var timeout = request.timeout();
        var version = request.version();
        var expectContinue = request.expectContinue();
        var snapshot = new HttpRequest() {
            @Override
            public Optional<BodyPublisher> bodyPublisher() {
                return Optional.empty();
            }

            @Override
            public String method() {
                return method;
            }

            @Override
            public URI uri() {
                return uri;
            }

            @Override
            public HttpHeaders headers() {
                return headers;
            }

            @Override
            public Optional<Duration> timeout() {
                return timeout;
            }

            @Override
            public Optional<Version> version() {
                return version;
            }

            @Override
            public boolean expectContinue() {
                return expectContinue;
            }
        };
        var processedBody = LogSanitizer.body(body);
        return new HttpRequestLog() {
            @Override
            public HttpRequest request() {
                return snapshot;
            }

            @Override
            public String body() {
                return processedBody;
            }
        };
    }
}
