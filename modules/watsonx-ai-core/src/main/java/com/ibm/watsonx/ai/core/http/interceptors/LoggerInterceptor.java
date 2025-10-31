/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.interceptors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.core.HttpUtils;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Interceptor that logs HTTP requests and responses.
 */
public final class LoggerInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggerInterceptor.class);
    private static final Pattern BASE64_IMAGE_PATTERN =
        Pattern.compile("(data:[\\w\\/+]+;base64,)(.{15})([^\"]+)");
    private static final Pattern API_KEY_PATTERN =
        Pattern.compile("\"(api-key|apiKey)\"\\s*:\\s*\"([^\"]+\")", Pattern.CASE_INSENSITIVE);

    private final boolean logRequest;
    private final boolean logResponse;

    /**
     * Constructs a LoggerInterceptor with default log mode (BOTH).
     */
    public LoggerInterceptor() {
        this.logRequest = true;
        this.logResponse = true;
    }

    /**
     * Constructs a LoggerInterceptor with custom logging behavior.
     *
     * @param logRequest {@code true} to enable logging of outgoing requests, {@code false} to disable
     * @param logResponse {@code true} to enable logging of incoming responses, {@code false} to disable
     */
    public LoggerInterceptor(boolean logRequest, boolean logResponse) {
        this.logRequest = logRequest;
        this.logResponse = logResponse;
    }

    /**
     * Constructs a LoggerInterceptor with the specified log mode.
     *
     * @param mode The log mode.
     */
    public LoggerInterceptor(LogMode mode) {
        mode = requireNonNullElse(mode, LogMode.BOTH);
        switch(mode) {
            case BOTH -> {
                this.logRequest = true;
                this.logResponse = true;
            }
            case REQUEST -> {
                this.logRequest = true;
                this.logResponse = false;
            }
            case RESPONSE -> {
                this.logRequest = false;
                this.logResponse = true;
            }
            default -> throw new RuntimeException("Unknown log mode: " + mode);
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, AsyncChain chain) {
        return CompletableFuture
            .runAsync(() -> logRequest(request), ExecutorProvider.ioExecutor())
            .thenComposeAsync(v -> chain.proceed(request, bodyHandler), ExecutorProvider.ioExecutor())
            .whenComplete((response, exception) -> {
                var watsonxSDKRequestId = request.headers().firstValue("Watsonx-AI-SDK-Request-Id").orElse("");
                if (isNull(exception))
                    logResponse(watsonxSDKRequestId, response);
                else
                    logResponse(watsonxSDKRequestId, exception);
            });
    }

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
        throws WatsonxException, IOException, InterruptedException {
        logRequest(request);
        var watsonxSDKRequestId = request.headers().firstValue("Watsonx-AI-SDK-Request-Id").orElse("");
        try {
            var response = chain.proceed(request, bodyHandler);
            logResponse(watsonxSDKRequestId, response);
            return response;
        } catch (RuntimeException e) {
            logResponse(watsonxSDKRequestId, e);
            throw e;
        }
    }

    private void logRequest(HttpRequest request) {
        if (!logRequest)
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
            private StringBuilder builder;

            @Override
            public void onSubscribe(Subscription subscription) {
                builder = new StringBuilder();
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                String body = StandardCharsets.UTF_8.decode(item).toString();
                builder.append(body);
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {
                logRequest(request, builder.toString());
            }
        });
    }

    private <T> void logResponse(String watsonxAISDKRequestId, Throwable exception) {
        if (!logResponse)
            return;

        StringJoiner joiner = new StringJoiner("\n", "Response:\n", "");
        joiner.add("- Watsonx-AI-SDK-Request-Id: " + watsonxAISDKRequestId);

        if (exception instanceof WatsonxException e) {
            joiner.add("- status code: " + e.statusCode());
            joiner.add("- body: " + Json.prettyPrint(exception.getMessage()));
        } else {
            joiner.add("- body: " + exception.getMessage());
        }

        logger.info(joiner.toString());
    }

    private <T> void logResponse(String watsonxAISDKRequestId, HttpResponse<T> response) {
        if (!logResponse)
            return;

        try {

            String body = null;
            String headers = null;
            boolean prettyPrint = false;

            T responseBody = response.body();
            boolean isStream = responseBody instanceof InputStream;

            if (!isStream)
                body = HttpUtils.extractBodyAsString(response).orElse(null);

            StringJoiner joiner = new StringJoiner("\n", "Response:\n", "");
            joiner.add("- Watsonx-AI-SDK-Request-Id: " + watsonxAISDKRequestId);
            joiner.add("- url: " + response.uri());
            joiner.add("- status code: " + response.statusCode());

            if (nonNull(response.headers())) {
                headers = HttpUtils.inOneLine(response.headers().map());
                joiner.add("- headers: " + headers);

                var headersMap = response.headers().map();
                var contentType = Optional.<String>empty();

                if (headersMap.containsKey("Content-Type"))
                    contentType = response.headers().firstValue("Content-Type");
                else if (headersMap.containsKey("content-type"))
                    contentType = response.headers().firstValue("content-type");

                if (contentType.isPresent() && contentType.get().contains("application/json"))
                    prettyPrint = true;
            }

            if (nonNull(body)) {
                body = prettyPrint ? Json.prettyPrint(body) : body;
                joiner.add("- body: " + body);
            }

            logger.info(joiner.toString());

        } catch (Exception e) {
            logger.warn("Failed to log response", e);
        }
    }

    private void logRequest(HttpRequest request, String body) {
        String headers = null;
        StringJoiner joiner = new StringJoiner("\n", "Request:\n", "");
        joiner.add("- method: " + request.method());
        joiner.add("- url: " + request.uri());

        if (nonNull(request.headers())) {
            headers = HttpUtils.inOneLine(request.headers().map());
            joiner.add("- headers: " + headers);
            if (nonNull(body)) {
                body = formatBase64Image(body);
                body = maskApiKeysInJsonBody(body);

                var headersMap = request.headers().map();
                var contentType = Optional.<String>empty();

                if (headersMap.containsKey("Content-Type"))
                    contentType = request.headers().firstValue("Content-Type");
                else if (headersMap.containsKey("content-type"))
                    contentType = request.headers().firstValue("content-type");

                if (contentType.isPresent() && contentType.get().contains("application/json"))
                    body = Json.prettyPrint(body);

                joiner.add("- body: " + body);
            }
        }

        logger.info(joiner.toString());
    }

    private boolean isNonRepeatablePublisher(HttpRequest.BodyPublisher publisher) {
        String className = publisher.getClass().getName();
        return className.contains("StreamPublisher") ||
            className.contains("FilePublisher") ||
            className.contains("InputStream") ||
            className.contains("BufferedInputStream");
    }

    private String maskApiKeysInJsonBody(String body) {

        if (body == null || body.isBlank())
            return body;

        Matcher matcher = API_KEY_PATTERN.matcher(body);

        StringBuilder sb = new StringBuilder();
        while (matcher.find())
            matcher.appendReplacement(sb, "\"" + matcher.group(1) + "\": \"***\"");

        matcher.appendTail(sb);
        return sb.isEmpty() ? body : sb.toString();
    }

    private String formatBase64Image(String body) {

        if (body == null || body.isBlank())
            return body;

        Matcher matcher = BASE64_IMAGE_PATTERN.matcher(body);
        StringBuilder sb = new StringBuilder();

        while (matcher.find())
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2) + "...");

        matcher.appendTail(sb);
        return sb.toString();
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
