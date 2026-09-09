/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.http.HttpHeaders;
import java.util.StringJoiner;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLog;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;

/**
 * {@code SseEventLogger} is a {@link Flow.Subscriber} wrapper designed to intercept and log Server-Sent Events (SSE) line-by-line from an HTTP
 * response body.
 * <p>
 * It aggregates lines belonging to the same SSE event (separated by an empty line) and logs the full event as a single structured log message,
 * including:
 * <ul>
 * <li>HTTP response status code</li>
 * <li>HTTP response headers</li>
 * <li>Event body content</li>
 * </ul>
 * <p>
 * After logging, each received line is forwarded to the wrapped downstream subscriber.
 */
public final class SseEventLogger implements Subscriber<String> {

    private static final Logger logger = LoggerFactory.getLogger(SseEventLogger.class);
    private final Flow.Subscriber<String> subscriber;
    private final int statusCode;
    private final HttpHeaders headers;
    private final HttpResponseLogger responseLogger;
    private final Level responseLogLevel;
    private StringJoiner dataJoiner;

    /**
     * Constructs a new {@code SseEventLogger} with the given downstream subscriber, HTTP status code, and response headers.
     *
     * @param subscriber the downstream subscriber to forward events to
     * @param statusCode the HTTP response status code
     * @param headers the HTTP response headers
     */
    public SseEventLogger(Flow.Subscriber<String> subscriber, int statusCode, HttpHeaders headers) {
        this(subscriber, statusCode, headers, null, Level.INFO);
    }

    /**
     * Constructs a new {@code SseEventLogger} with the given downstream subscriber, HTTP status code, response headers, and custom logger.
     *
     * @param subscriber the downstream subscriber to forward events to
     * @param statusCode the HTTP response status code
     * @param headers the HTTP response headers
     * @param responseLogger the custom response logger, or {@code null} to use the default SLF4J behavior
     * @param responseLogLevel the level that enables {@code responseLogger}
     */
    public SseEventLogger(Flow.Subscriber<String> subscriber, int statusCode, HttpHeaders headers, HttpResponseLogger responseLogger,
        Level responseLogLevel) {
        this.subscriber = requireNonNull(subscriber);
        this.statusCode = statusCode;
        this.headers = headers;
        this.responseLogger = responseLogger;
        this.responseLogLevel = requireNonNullElse(responseLogLevel, Level.INFO);
        dataJoiner = new StringJoiner("\n");
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(String item) {

        if (item.isBlank()) {
            if (dataJoiner.length() > 0) {
                if (nonNull(responseLogger)) {
                    if (logger.isEnabledForLevel(responseLogLevel)) {
                        try {
                            responseLogger.log(HttpResponseLog.of(null, statusCode, headers, null, dataJoiner.toString(), null));
                        } catch (Exception e) {
                            logger.warn("Failed to log response", e);
                        }
                    }
                } else {
                    logger.info(
                        "Response:\n- status code: {}\n- headers: {}\n- body: {}",
                        statusCode,
                        nonNull(headers) ? HttpUtils.inOneLine(headers.map()) : null,
                        dataJoiner.toString());
                }
            }
            dataJoiner = new StringJoiner("\n");
        } else {
            dataJoiner.add(item);
        }

        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
