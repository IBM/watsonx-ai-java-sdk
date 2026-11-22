/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.ServiceConfigurationError;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLog;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;
import com.ibm.watsonx.ai.core.internal.logging.LogSanitizer;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

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
 * Diagnostic events mask secrets in data and headers and omit other SSE field values. Each received line is forwarded unchanged to the wrapped
 * downstream subscriber.
 */
public final class SseEventLogger implements Subscriber<String> {

    private static final Logger logger = LoggerFactory.getLogger(SseEventLogger.class);
    private final Flow.Subscriber<String> subscriber;
    private final int statusCode;
    private final HttpHeaders headers;
    private final String formattedHeaders;
    private final HttpResponseLogger responseLogger;
    private final Level responseLogLevel;
    private final AtomicReference<CompletableFuture<Void>> pendingLog = new AtomicReference<>(CompletableFuture.completedFuture(null));
    private StringBuilder data;

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
        this.headers = LogSanitizer.headers(headers);
        this.formattedHeaders = formatHeaders(this.headers);
        this.responseLogger = responseLogger;
        this.responseLogLevel = requireNonNullElse(responseLogLevel, Level.INFO);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(String item) {
        if (item.isBlank()) {
            StringBuilder eventData = data;
            data = null;
            logSafely(() -> logEvent(eventData));
        } else {
            collectData(item);
        }
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        logSafely(() -> logger.error("SSE stream failed", LogSanitizer.error(throwable)));
        pendingLog.get().whenComplete((v, e) -> subscriber.onError(throwable));
    }

    @Override
    public void onComplete() {
        pendingLog.get().whenComplete((v, e) -> subscriber.onComplete());
    }

    private void logEvent(StringBuilder eventData) {
        boolean enabled = nonNull(responseLogger) ? logger.isEnabledForLevel(responseLogLevel) : logger.isInfoEnabled();
        if (!enabled || isNull(eventData))
            return;
        String diagnostic = "data: " + LogSanitizer.body(eventData.toString());
        if (nonNull(responseLogger)) {
            HttpResponseLog entry = event(statusCode, headers, diagnostic);
            scheduleLog(() -> {
                try {
                    responseLogger.log(entry);
                } catch (Exception | LinkageError | ServiceConfigurationError | StackOverflowError e) {
                    logger.warn("Failed to log response", LogSanitizer.error(e));
                }
            });
        } else {
            logger.info("Response:\n- status code: {}\n- headers: {}\n- body: {}", statusCode, formattedHeaders, diagnostic);
        }
    }

    private void collectData(String item) {
        int start = 0;
        while (start < item.length()) {
            int end = item.indexOf('\n', start);
            if (end < 0)
                end = item.length();
            int valueStart = start + 4;
            if (valueStart <= end && item.startsWith("data", start) && (valueStart == end || item.charAt(valueStart) == ':')) {
                if (valueStart < end) {
                    valueStart++;
                    if (valueStart < end && item.charAt(valueStart) == ' ')
                        valueStart++;
                }
                if (isNull(data))
                    data = new StringBuilder(end - valueStart);
                else
                    data.append('\n');
                data.append(item, valueStart, end);
            }
            start = end + 1;
        }
    }

    private static HttpResponseLog event(int statusCode, HttpHeaders headers, String body) {
        return new HttpResponseLog() {
            @Override
            public String requestId() {
                return null;
            }

            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpHeaders headers() {
                return headers;
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Throwable error() {
                return null;
            }
        };
    }

    private static String formatHeaders(HttpHeaders headers) {
        if (isNull(headers))
            return null;
        StringJoiner result = new StringJoiner(", ");
        headers.map().forEach((key, values) -> result.add("[" + key + ": " + String.join(" ", values) + "]"));
        return result.toString();
    }

    private void logSafely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException | LinkageError | ServiceConfigurationError | StackOverflowError e) {
            logger.warn("SSE logging failed");
        }
    }

    /**
     * Schedules {@code task} on {@link ExecutorProvider#callbackExecutor()} as a continuation of this instance's previous dispatch, so consecutive
     * log calls never overlap and complete in emission order.
     *
     * @param task the logging work to run
     */
    private void scheduleLog(Runnable task) {
        var delivered = new CompletableFuture<Void>();
        var previous = pendingLog.getAndSet(delivered);
        try {
            previous.thenRunAsync(task, ExecutorProvider.callbackExecutor()).whenComplete((value, failure) -> {
                try {
                    if (nonNull(failure))
                        logger.warn("Failed to log response", LogSanitizer.error(failure));
                } finally {
                    delivered.complete(null);
                }
            });
        } catch (RuntimeException | LinkageError | ServiceConfigurationError | StackOverflowError e) {
            delivered.complete(null);
            throw e;
        }
    }
}
