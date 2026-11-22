/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.interceptors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.logging.HttpRequestLog;
import com.ibm.watsonx.ai.core.http.logging.HttpRequestLogger;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLog;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;
import com.ibm.watsonx.ai.core.internal.logging.LogSanitizer;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Interceptor that logs HTTP requests and responses.
 */
public final class LoggerInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggerInterceptor.class);
    private final boolean logRequest;
    private final boolean logResponse;
    private final HttpRequestLogger requestLogger;
    private final Level requestLogLevel;
    private final HttpResponseLogger responseLogger;
    private final Level responseLogLevel;

    /**
     * Constructs a LoggerInterceptor with default log mode (BOTH).
     */
    public LoggerInterceptor() {
        this(LogMode.BOTH, null, Level.INFO, null, Level.INFO);
    }

    /**
     * Constructs a LoggerInterceptor with custom logging behavior.
     *
     * @param logRequest {@code true} to enable logging of outgoing requests, {@code false} to disable
     * @param logResponse {@code true} to enable logging of incoming responses, {@code false} to disable
     */
    public LoggerInterceptor(boolean logRequest, boolean logResponse) {
        this(LogMode.of(logRequest, logResponse), null, Level.INFO, null, Level.INFO);
    }

    /**
     * Constructs a LoggerInterceptor with the specified log mode.
     *
     * @param mode The log mode.
     */
    public LoggerInterceptor(LogMode mode) {
        this(mode, null, Level.INFO, null, Level.INFO);
    }

    /**
     * Constructs a LoggerInterceptor with the specified log mode and custom loggers.
     *
     * @param mode the log mode
     * @param requestLogger the custom request logger, or {@code null} to use the default SLF4J behavior
     * @param requestLogLevel the level that enables {@code requestLogger}
     * @param responseLogger the custom response logger, or {@code null} to use the default SLF4J behavior
     * @param responseLogLevel the level that enables {@code responseLogger}
     */
    public LoggerInterceptor(LogMode mode, HttpRequestLogger requestLogger, Level requestLogLevel, HttpResponseLogger responseLogger,
        Level responseLogLevel) {
        mode = requireNonNullElse(mode, LogMode.BOTH);
        this.logRequest = switch(mode) {
            case BOTH, REQUEST -> true;
            case RESPONSE, DISABLED -> false;
        };
        this.logResponse = switch(mode) {
            case BOTH, RESPONSE -> true;
            case REQUEST, DISABLED -> false;
        };
        this.requestLogger = requestLogger;
        this.requestLogLevel = requireNonNullElse(requestLogLevel, Level.INFO);
        this.responseLogger = responseLogger;
        this.responseLogLevel = requireNonNullElse(responseLogLevel, Level.INFO);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, AsyncChain chain) {
        return CompletableFuture
            .runAsync(() -> logSafely(() -> logRequest(request)), ExecutorProvider.ioExecutor())
            .thenCompose(v -> chain.proceed(request, bodyHandler))
            .whenComplete((response, exception) -> {
                var watsonxSDKRequestId = request.headers().firstValue("Watsonx-AI-SDK-Request-Id").orElse("");
                if (isNull(exception))
                    logSafely(() -> logResponse(watsonxSDKRequestId, response));
                else
                    logSafely(() -> logResponse(watsonxSDKRequestId, exception));
            });
    }

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
        throws WatsonxException, IOException, InterruptedException {
        logSafely(() -> logRequest(request));
        var watsonxSDKRequestId = request.headers().firstValue("Watsonx-AI-SDK-Request-Id").orElse("");
        try {
            var response = chain.proceed(request, bodyHandler);
            logSafely(() -> logResponse(watsonxSDKRequestId, response));
            return response;
        } catch (IOException | InterruptedException | RuntimeException e) {
            logSafely(() -> logResponse(watsonxSDKRequestId, e));
            throw e;
        }
    }

    private void logRequest(HttpRequest request) {
        boolean enabled = nonNull(requestLogger) ? logger.isEnabledForLevel(requestLogLevel) : logger.isInfoEnabled();
        if (!logRequest || !enabled)
            return;

        Optional<BodyPublisher> maybePublisher = request.bodyPublisher();
        if (maybePublisher.isEmpty()) {
            logRequest(request, null);
            return;
        }

        BodyPublisher publisher = maybePublisher.get();

        if (isNonRepeatablePublisher(publisher)) {
            logRequest(request, "[non-repeatable body skipped]");
            return;
        }

        publisher.subscribe(new Subscriber<>() {
            private ByteArrayOutputStream buffer;

            @Override
            public void onSubscribe(Subscription subscription) {
                buffer = new ByteArrayOutputStream();
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                ByteBuffer copy = item.asReadOnlyBuffer();
                byte[] bytes = new byte[copy.remaining()];
                copy.get(bytes);
                buffer.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                logger.warn("Error reading request body for logging", LogSanitizer.error(throwable));
            }

            @Override
            public void onComplete() {
                logSafely(() -> logRequest(request, buffer.toString(StandardCharsets.UTF_8)));
            }
        });
    }

    private void logResponse(String watsonxAISDKRequestId, Throwable exception) {
        if (!responseLoggingEnabled())
            return;
        Throwable cause = unwrap(exception);
        int statusCode = cause instanceof WatsonxException e ? e.statusCode() : -1;
        HttpResponseLog entry = HttpResponseLog.of(watsonxAISDKRequestId, statusCode, null, null, cause.getMessage(), cause);
        logResponse(entry);
    }

    private void logResponse(String watsonxAISDKRequestId, HttpResponse<?> response) {
        if (!responseLoggingEnabled())
            return;
        try {
            HttpResponseLog entry = HttpResponseLog.of(watsonxAISDKRequestId, response.statusCode(), response.headers(), response.uri(),
                diagnosticBody(response), null);
            logResponse(entry);
        } catch (Exception e) {
            logger.warn("Failed to log response", LogSanitizer.error(e));
        }
    }

    private boolean responseLoggingEnabled() {
        return logResponse && (nonNull(responseLogger) ? logger.isEnabledForLevel(responseLogLevel) : logger.isInfoEnabled());
    }

    private void logResponse(HttpResponseLog entry) {
        if (nonNull(responseLogger)) {
            dispatch(() -> responseLogger.log(entry), "Failed to log response");
            return;
        }
        StringJoiner joiner = new StringJoiner("\n", "Response:\n", "");
        joiner.add("- Watsonx-AI-SDK-Request-Id: " + entry.requestId());
        if (nonNull(entry.uri()))
            joiner.add("- url: " + entry.uri());
        if (entry.statusCode() >= 0)
            joiner.add("- status code: " + entry.statusCode());
        if (nonNull(entry.headers()))
            joiner.add("- headers: " + formatHeaders(entry.headers()));
        if (nonNull(entry.body()))
            joiner.add("- body: " + prettyPrint(entry.body()));
        logger.info(joiner.toString());
    }

    private void logRequest(HttpRequest request, String body) {
        HttpRequestLog entry = HttpRequestLog.of(request, body);
        if (nonNull(requestLogger)) {
            dispatch(() -> requestLogger.log(entry), "Failed to log request");
            return;
        }
        HttpRequest snapshot = entry.request();
        StringJoiner joiner = new StringJoiner("\n", "Request:\n", "");
        joiner.add("- method: " + snapshot.method());
        joiner.add("- url: " + snapshot.uri());
        if (nonNull(snapshot.headers()))
            joiner.add("- headers: " + formatHeaders(snapshot.headers()));
        if (nonNull(entry.body()))
            joiner.add("- body: " + prettyPrint(entry.body()));
        logger.info(joiner.toString());
    }

    private void dispatch(Runnable task, String failureMessage) {
        ExecutorProvider.callbackExecutor().execute(() -> {
            try {
                task.run();
            } catch (Exception | LinkageError | ServiceConfigurationError | StackOverflowError e) {
                logger.warn(failureMessage, LogSanitizer.error(e));
            }
        });
    }

    private void logSafely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException | LinkageError | ServiceConfigurationError | StackOverflowError e) {
            logger.warn("HTTP logging failed");
        }
    }

    private Throwable unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException) && nonNull(error.getCause())
            && error.getCause() != error)
            error = error.getCause();
        return error;
    }

    private String formatHeaders(HttpHeaders headers) {
        StringJoiner result = new StringJoiner(", ");
        headers.map().forEach((key, values) -> result.add("[" + key + ": " + String.join(" ", values) + "]"));
        return result.toString();
    }

    private boolean isNonRepeatablePublisher(BodyPublisher publisher) {
        return switch(publisher.getClass().getName()) {
            case "jdk.internal.net.http.RequestPublishers$StringPublisher",
                "jdk.internal.net.http.RequestPublishers$ByteArrayPublisher",
                "jdk.internal.net.http.RequestPublishers$EmptyPublisher" -> false;
            default -> true;
        };
    }

    private String diagnosticBody(HttpResponse<?> response) {
        Object body = response.body();
        if (body instanceof String text)
            return text;
        if (body instanceof byte[] bytes)
            return new String(bytes, StandardCharsets.UTF_8);
        return null;
    }

    private String prettyPrint(String body) {
        try {
            return Json.prettyPrintObject(body);
        } catch (RuntimeException | LinkageError | ServiceConfigurationError | StackOverflowError e) {
            return body;
        }
    }

    /**
     * Specifies which parts of the HTTP transaction should be logged.
     */
    public enum LogMode {

        /**
         * No log.
         */
        DISABLED,

        /**
         * Log only the HTTP request.
         */
        REQUEST,

        /**
         * Log only the HTTP response.
         */
        RESPONSE,

        /**
         * Log both the HTTP request and the HTTP response.
         */
        BOTH;

        /**
         * Creates a LogMode enum value based on the given logRequest and logResponse booleans.
         *
         * @param logRequest true if request logging is desired, false otherwise
         * @param logResponse true if response logging is desired, false otherwise
         * @return The appropriate LogMode enum value based on the input booleans
         */
        public static LogMode of(boolean logRequest, boolean logResponse) {
            if (logRequest && logResponse)
                return BOTH;

            if (logRequest)
                return REQUEST;

            if (logResponse)
                return RESPONSE;

            return DISABLED;
        }
    }
}
