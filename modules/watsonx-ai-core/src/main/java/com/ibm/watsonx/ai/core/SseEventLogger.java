/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.net.http.HttpHeaders;
import java.util.StringJoiner;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SseEventLogger implements Subscriber<String> {

  private static final Logger logger = LoggerFactory.getLogger(SseEventLogger.class);
  private final Flow.Subscriber<String> subscriber;
  private final int statusCode;
  private final String headers;
  private StringJoiner dataJoiner;

  /**
   * Constructs a new {@code SseEventLogger} with the given downstream subscriber, HTTP status code, and response headers.
   *
   * @param subscriber the downstream subscriber to forward events to
   * @param statusCode the HTTP response status code
   * @param headers the HTTP response headers
   */
  public SseEventLogger(Flow.Subscriber<String> subscriber, int statusCode, HttpHeaders headers) {
    this.subscriber = requireNonNull(subscriber);
    this.statusCode = statusCode;
    this.headers = nonNull(headers) ? HttpUtils.inOneLine(headers.map()) : null;
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
        logger.info(
          "Response:\n- status code: {}\n- headers: {}\n- body: {}",
          statusCode,
          headers,
          dataJoiner.toString());
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
