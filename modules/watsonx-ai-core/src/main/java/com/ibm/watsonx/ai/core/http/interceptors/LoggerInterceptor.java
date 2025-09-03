/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.interceptors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.core.HttpUtils;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;

/**
 * Interceptor that logs HTTP requests and responses.
 */
public final class LoggerInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggerInterceptor.class);
    private static final Pattern BASE64_IMAGE_PATTERN =
        Pattern.compile("(data:.+;base64,)(.{15})([^\"]+)([\\s\\S]*)");

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
    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
        Executor executor, int index, AsyncChain chain) {
        return CompletableFuture
            .runAsync(() -> logRequest(request), executor)
            .thenComposeAsync(v -> chain.proceed(request, bodyHandler, executor), executor)
            .whenCompleteAsync((respose, exception) -> {
                if (isNull(exception))
                    logResponse(request, respose);
                else {
                    var watsonxSDKRequestId = request.headers().firstValue("Watsonx-AI-SDK-Request-Id").orElse("");
                    logResponse(watsonxSDKRequestId, exception);
                }
            }, executor);
    }

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
        throws WatsonxException, IOException, InterruptedException {
        logRequest(request);
        try {
            var response = chain.proceed(request, bodyHandler);
            logResponse(request, response);
            return response;
        } catch (RuntimeException e) {
            var watsonxSDKRequestId = request.headers().firstValue("Watsonx-AI-SDK-Request-Id").orElse("");
            logResponse(watsonxSDKRequestId, e);
            throw e;
        }
    }

    private void logRequest(HttpRequest request) {
        if (!logRequest)
            return;

        request.bodyPublisher().ifPresentOrElse(
            publisher -> publisher.subscribe(new Subscriber<>() {
                private StringBuilder builder;
                private Boolean isImageDetected;

                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    String body = StandardCharsets.UTF_8.decode(item).toString();

                    if (isNull(isImageDetected)) {
                        isImageDetected = BASE64_IMAGE_PATTERN.matcher(body).find();
                        if (isImageDetected) {
                            builder = new StringBuilder();
                            builder.append(body);
                            return;
                        } else {
                            printRequest(request, body);
                            return;
                        }
                    }

                    if (isImageDetected) {
                        builder.append(body);
                    }
                }

                @Override
                public void onError(Throwable throwable) {}

                @Override
                public void onComplete() {
                    if (nonNull(isImageDetected) && isImageDetected) {
                        printRequest(request, builder.toString());
                    }
                }
            }),
            () -> printRequest(request, null)
        );
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

    private <T> void logResponse(HttpRequest request, HttpResponse<T> response) {
        if (!logResponse)
            return;

        try {

            String headers = null;
            boolean prettyPrint = false;
            String body = HttpUtils.extractBodyAsString(response).orElse(null);

            StringJoiner joiner = new StringJoiner("\n", "Response:\n", "");
            joiner.add("- Watsonx-AI-SDK-Request-Id: " + request.headers().firstValue("Watsonx-AI-SDK-Request-Id").orElse(""));
            joiner.add("- url: " + response.uri());
            joiner.add("- status code: " + response.statusCode());

            if (nonNull(response.headers())) {
                headers = HttpUtils.inOneLine(response.headers().map());
                joiner.add("- headers: " + headers);
            }

            if (!prettyPrint && nonNull(response.headers())) {
                headers = HttpUtils.inOneLine(response.headers().map());
                var accept = request.headers().firstValue("Accept");
                if (accept.isPresent() && accept.get().contains("application/json")) {
                    prettyPrint = true;
                }
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

    private void printRequest(HttpRequest request, String body) {
        String headers = null;
        StringJoiner joiner = new StringJoiner("\n", "Request:\n", "");
        joiner.add("- method: " + request.method());
        joiner.add("- url: " + request.uri());

        if (nonNull(request.headers())) {
            headers = HttpUtils.inOneLine(request.headers().map());
            joiner.add("- headers: " + headers);
            if (nonNull(body)) {
                body = formatBase64ImageForLogging(body);
                var contentType = request.headers().firstValue("Content-Type");
                if (contentType.isPresent() && contentType.get().contains("application/json")) {
                    body = Json.prettyPrint(body);
                }
                joiner.add("- body: " + body);
            }
        }

        logger.info(joiner.toString());
    }

    private String formatBase64ImageForLogging(String body) {

        if (body == null || body.isBlank())
            return body;

        Matcher matcher = BASE64_IMAGE_PATTERN.matcher(body);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb,
                matcher.group(1) + matcher.group(2) + "..." + matcher.group(4));
        }

        return sb.isEmpty() ? body : sb.toString();
    }

    /**
     * Specifies which parts of the HTTP transaction should be logged.
     */
    public enum LogMode {

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
        BOTH
    }

}
