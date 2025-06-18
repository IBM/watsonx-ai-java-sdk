/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.core.http.interceptors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.core.HttpUtils;
import com.ibm.watsonx.core.Json;
import com.ibm.watsonx.core.exeception.WatsonxException;
import com.ibm.watsonx.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.core.http.SyncHttpInterceptor;

/**
 * Interceptor that logs HTTP requests and responses.
 */
public class LoggerInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(LoggerInterceptor.class);
  private static final Pattern BEARER_PATTERN =
    Pattern.compile("(Bearer\\s*)(\\w{4})(\\w+)(\\w{4})");
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
    };
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
    Executor executor, int index, AsyncChain chain) {
    return CompletableFuture
      .runAsync(() -> logRequest(request), executor)
      .thenCompose(v -> chain.proceed(request, bodyHandler, executor))
      .whenCompleteAsync((respose, exception) -> {
        if (isNull(exception))
          logResponse(request, respose);
      }, executor);
  }

  @Override
  public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
    throws WatsonxException, IOException, InterruptedException {
    logRequest(request);
    var response = chain.proceed(request, bodyHandler);
    logResponse(request, response);
    return response;
  }

  private void logRequest(HttpRequest request) {
    if (!logRequest)
      return;

    if (request.bodyPublisher().isPresent()) {
      request.bodyPublisher().get().subscribe(new Subscriber<>() {
        @Override
        public void onSubscribe(Subscription subscription) {
          subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer item) {
          printRequest(request, new String(item.array(), StandardCharsets.UTF_8));
        }

        @Override
        public void onError(Throwable throwable) {}

        @Override
        public void onComplete() {}
      });
    } else
      printRequest(request, null);
  }

  private <T> void logResponse(HttpRequest request, HttpResponse<T> response) {
    if (!logResponse)
      return;

    try {

      String headers = null;
      boolean prettyPrint = false;
      String body = HttpUtils.extractBodyAsString(response).orElse(null);

      if (nonNull(response.headers()))
        headers = inOneLine(response.headers().map());

      if (!prettyPrint && nonNull(request.headers())) {
        headers = inOneLine(request.headers().map());
        var accept = request.headers().firstValue("Accept");
        if (accept.isPresent() && accept.get().contains("application/json")) {
          prettyPrint = true;
        }
      }

      if (prettyPrint)
        body = Json.prettyPrint(body);

      logger.info(
        "Response:\n- status code: {}\n- headers: {}\n- body: {}",
        response.statusCode(),
        headers,
        body);

    } catch (Exception e) {
      logger.warn("Failed to log response", e);
    }
  }

  private void printRequest(HttpRequest request, String body) {
    String headers = null;

    if (nonNull(request.headers())) {
      body = formatBase64ImageForLogging(body);
      headers = inOneLine(request.headers().map());
      var contentType = request.headers().firstValue("Content-Type");
      if (contentType.isPresent() && contentType.get().contains("application/json")) {
        body = Json.prettyPrint(body);
      }
    }

    logger.info(
      "Request:\n- method: {}\n- url: {}\n- headers: {}\n- body: {}",
      request.method(),
      request.uri(),
      headers,
      body);
  }

  private String inOneLine(Map<String, List<String>> headers) {
    return headers.entrySet().stream().map(header -> {
      String headerKey = header.getKey();
      String headerValues = header.getValue().stream().collect(Collectors.joining(" "));
      if ("Authorization".equals(headerKey)) {
        headerValues = maskAuthorizationHeaderValue(headerValues);
      }
      return String.format("[%s: %s]", headerKey, headerValues);
    }).collect(joining(", "));
  }

  private String maskAuthorizationHeaderValue(String authorizationHeaderValue) {

    Matcher matcher = BEARER_PATTERN.matcher(authorizationHeaderValue);

    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2) + "..." + matcher.group(4));
    }

    return sb.toString();
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
