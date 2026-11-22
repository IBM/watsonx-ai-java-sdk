/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.http.logging.HttpRequestLog;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLog;
import com.sun.net.httpserver.HttpServer;

@DisabledInNativeImage
class LoggerTransportTest {

    private static final String AUTHORIZATION = "synthetic-authorization-12345";
    private static final String COOKIE = "synthetic-cookie-12345";
    private static final String IAM_TOKEN = "synthetic-iam-token-12345";
    private static final String API_KEY = "synthetic-api-key-12345";
    private static final String SECOND_API_KEY = "synthetic-second-api-key-12345";
    private static final String QUERY_KEY = "synthetic-query-key-12345";
    private static final String PASSWORD = "synthetic-body-password-12345";
    private static final String NESTED_KEY = "synthetic-nested-key-12345";
    private static final String RESPONSE_TOKEN = "synthetic-response-token-12345";
    private static final String RESPONSE_COOKIE = "synthetic-response-cookie-12345";
    private static final List<String> SECRETS = List.of(AUTHORIZATION, COOKIE, IAM_TOKEN, API_KEY, SECOND_API_KEY, QUERY_KEY, PASSWORD,
        NESTED_KEY, RESPONSE_TOKEN, RESPONSE_COOKIE);
    private static final String REQUEST_BODY =
        "{\"password\":\"" + PASSWORD + "\",\"nested\":[{\"apikey\":\"" + NESTED_KEY + "\"}],\"visible\":\"kept\"}";
    private static final String RESPONSE_BODY = "{\"access_token\":\"" + RESPONSE_TOKEN + "\",\"visible\":\"kept\"}";

    @ParameterizedTest
    @CsvSource({ "false, false", "true, false", "false, true", "true, true" })
    void should_sanitize_logs_without_changing_http_traffic(boolean async, boolean streamingBody) throws Exception {
        var received = new CompletableFuture<ReceivedRequest>();
        var server = server(200, "application/json", RESPONSE_BODY, received);
        try {
            var requestEntry = new CompletableFuture<HttpRequestLog>();
            var responseEntry = new CompletableFuture<HttpResponseLog>();
            var logger = new LoggerInterceptor(LogMode.BOTH, requestEntry::complete, Level.INFO, responseEntry::complete, Level.INFO);
            var subscriptions = new AtomicInteger();
            var body = streamingBody ? BodyPublishers.ofInputStream(() -> {
                if (subscriptions.incrementAndGet() != 1)
                    throw new IllegalStateException("The request body was consumed more than once");
                return new ByteArrayInputStream(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));
            }) : BodyPublishers.ofString(REQUEST_BODY);
            var request = request(server).POST(body).build();
            var httpClient = HttpClient.newHttpClient();
            HttpResponse<?> response;
            String responseBody;
            if (streamingBody) {
                var streamedResponse = async
                    ? AsyncHttpClient.builder().httpClient(httpClient).interceptor(logger).build()
                        .send(request, BodyHandlers.ofInputStream()).get(10, TimeUnit.SECONDS)
                    : SyncHttpClient.builder().httpClient(httpClient).interceptor(logger).build().send(request, BodyHandlers.ofInputStream());
                response = streamedResponse;
                try (InputStream input = streamedResponse.body()) {
                    responseBody = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
                assertEquals(1, subscriptions.get());
            } else {
                var textResponse = async
                    ? AsyncHttpClient.builder().httpClient(httpClient).interceptor(logger).build()
                        .send(request, BodyHandlers.ofString()).get(10, TimeUnit.SECONDS)
                    : SyncHttpClient.builder().httpClient(httpClient).interceptor(logger).build().send(request, BodyHandlers.ofString());
                response = textResponse;
                responseBody = textResponse.body();
            }

            assertEquals(RESPONSE_BODY, responseBody);
            assertEquals(RESPONSE_COOKIE, response.headers().firstValue("Set-Cookie").orElseThrow());
            assertEquals("Bearer " + RESPONSE_TOKEN, response.headers().firstValue("Authorization").orElseThrow());
            assertEquals(request.uri(), response.uri());
            assertOriginalRequest(received.get(10, TimeUnit.SECONDS), request);
            assertEquals("Bearer " + AUTHORIZATION, request.headers().firstValue("Authorization").orElseThrow());
            assertEquals(List.of(API_KEY, SECOND_API_KEY), request.headers().allValues("X-Api-Key"));
            assertEquals(body, request.bodyPublisher().orElseThrow());

            var requestLog = requestEntry.get(10, TimeUnit.SECONDS);
            var responseLog = responseEntry.get(10, TimeUnit.SECONDS);
            assertSafeRequest(requestLog, request);
            if (streamingBody) {
                assertEquals("[non-repeatable body skipped]", requestLog.body());
                assertNull(responseLog.body());
            } else {
                assertTrue(requestLog.body().contains("kept"));
                assertTrue(responseLog.body().contains("kept"));
            }
            assertEquals(200, responseLog.statusCode());
            assertEquals("***", responseLog.headers().firstValue("Set-Cookie").orElseThrow());
            assertEquals("***", responseLog.headers().firstValue("Authorization").orElseThrow());
            assertNull(responseLog.error());
            assertSafeResponse(responseLog);
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_sanitize_error_logs_and_preserve_the_error_for_the_caller(boolean async) throws Exception {
        var errorBody = "password=" + PASSWORD + "&apikey=" + API_KEY;
        var received = new CompletableFuture<ReceivedRequest>();
        var server = server(400, "text/plain", errorBody, received);
        try {
            var requestEntry = new CompletableFuture<HttpRequestLog>();
            var responseEntry = new CompletableFuture<HttpResponseLog>();
            var logger = new LoggerInterceptor(LogMode.BOTH, requestEntry::complete, Level.INFO, responseEntry::complete, Level.INFO);
            var request = request(server).POST(BodyPublishers.ofString(REQUEST_BODY)).build();
            var httpClient = HttpClient.newHttpClient();
            WatsonxException failure;
            if (async) {
                var client = AsyncHttpClient.builder().httpClient(httpClient).interceptor(logger).build();
                var future = client.send(request, BodyHandlers.ofString());
                var execution = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
                failure = assertInstanceOf(WatsonxException.class, execution.getCause());
            } else {
                var client = SyncHttpClient.builder().httpClient(httpClient).interceptor(logger).build();
                failure = assertThrows(WatsonxException.class, () -> client.send(request, BodyHandlers.ofString()));
            }

            assertEquals(400, failure.statusCode());
            assertEquals(errorBody, failure.getMessage());
            assertTrue(failure.details().orElseThrow().errors().get(0).message().contains(PASSWORD));
            assertOriginalRequest(received.get(10, TimeUnit.SECONDS), request);
            assertSafeRequest(requestEntry.get(10, TimeUnit.SECONDS), request);
            var responseLog = responseEntry.get(10, TimeUnit.SECONDS);
            assertNotNull(responseLog.error());
            assertNotSame(failure, responseLog.error());
            assertNull(responseLog.error().getCause());
            assertEquals(0, responseLog.error().getSuppressed().length);
            assertTrue(responseLog.error().getStackTrace().length > 0);
            assertEquals(400, responseLog.statusCode());
            assertFalse(responseLog.error() instanceof WatsonxException);
            assertSafeResponse(responseLog);
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @CsvSource({ "false, false, false", "false, false, true", "false, true, false", "false, true, true",
            "true, false, false", "true, false, true", "true, true, false", "true, true, true" })
    void should_preserve_traffic_and_logs_for_long_quoted_text(boolean async, boolean custom, boolean requestSide) throws Exception {
        String longText = "Testo: \"" + "a".repeat(20_000) + "\"";
        String requestBody = requestSide ? longText : "visible request";
        String responseBody = requestSide ? "visible response" : longText;
        var received = new CompletableFuture<ReceivedRequest>();
        var server = server(200, "text/plain", responseBody, received);
        var requestEntry = new CompletableFuture<HttpRequestLog>();
        var responseEntry = new CompletableFuture<HttpResponseLog>();
        List<String> messages = new CopyOnWriteArrayList<>();
        var appender = new AbstractAppender("r04-long-body", null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        var log4jLogger = (Logger) LogManager.getLogger(LoggerInterceptor.class);
        log4jLogger.addAppender(appender);
        try {
            var logger = custom
                ? new LoggerInterceptor(LogMode.BOTH, requestEntry::complete, Level.INFO, responseEntry::complete, Level.INFO)
                : new LoggerInterceptor(LogMode.BOTH);
            var request = request(server).setHeader("Content-Type", "text/plain").POST(BodyPublishers.ofString(requestBody)).build();
            var httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = async
                ? AsyncHttpClient.builder().httpClient(httpClient).interceptor(logger).build()
                    .send(request, BodyHandlers.ofString()).get(10, TimeUnit.SECONDS)
                : SyncHttpClient.builder().httpClient(httpClient).interceptor(logger).build().send(request, BodyHandlers.ofString());
            assertEquals(requestBody, received.get(10, TimeUnit.SECONDS).body());
            assertEquals(responseBody, response.body());
            if (custom) {
                assertEquals(requestBody, requestEntry.get(10, TimeUnit.SECONDS).body());
                assertEquals(responseBody, responseEntry.get(10, TimeUnit.SECONDS).body());
            } else {
                assertTrue(String.join("\n", messages).contains(longText));
            }
        } finally {
            log4jLogger.removeAppender(appender);
            appender.stop();
            server.stop(0);
        }
    }

    private HttpServer server(int status, String contentType, String responseBody, CompletableFuture<ReceivedRequest> received) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                var headers = HttpHeaders.of(exchange.getRequestHeaders(), (name, value) -> true);
                var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                received.complete(new ReceivedRequest(body, exchange.getRequestURI().getRawQuery(), headers));
                exchange.getResponseHeaders().add("Content-Type", contentType);
                exchange.getResponseHeaders().add("Set-Cookie", RESPONSE_COOKIE);
                exchange.getResponseHeaders().add("Authorization", "Bearer " + RESPONSE_TOKEN);
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Exception e) {
                received.completeExceptionally(e);
                throw e;
            }
        });
        server.start();
        return server;
    }

    private HttpRequest.Builder request(HttpServer server) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/?apikey=" + QUERY_KEY + "&visible=kept"))
            .timeout(Duration.ofSeconds(10))
            .header("aUtHoRiZaTiOn", "Bearer " + AUTHORIZATION)
            .header("Cookie", COOKIE)
            .header("iam-token", IAM_TOKEN)
            .header("X-Api-Key", API_KEY)
            .header("X-Api-Key", SECOND_API_KEY)
            .header("Content-Type", "application/json");
    }

    private void assertOriginalRequest(ReceivedRequest received, HttpRequest request) {
        assertEquals(REQUEST_BODY, received.body());
        assertEquals(request.uri().getRawQuery(), received.query());
        assertEquals("Bearer " + AUTHORIZATION, received.headers().firstValue("Authorization").orElseThrow());
        assertEquals(COOKIE, received.headers().firstValue("Cookie").orElseThrow());
        assertEquals(IAM_TOKEN, received.headers().firstValue("iam-token").orElseThrow());
        assertEquals(List.of(API_KEY, SECOND_API_KEY), received.headers().allValues("X-Api-Key"));
    }

    private void assertSafeRequest(HttpRequestLog entry, HttpRequest original) {
        var snapshot = entry.request();
        assertNotSame(original, snapshot);
        assertEquals(original.method(), snapshot.method());
        assertEquals(original.timeout(), snapshot.timeout());
        assertEquals(original.version(), snapshot.version());
        assertEquals(original.expectContinue(), snapshot.expectContinue());
        assertTrue(snapshot.bodyPublisher().isEmpty());
        assertEquals("***", snapshot.headers().firstValue("Authorization").orElseThrow());
        assertEquals("***", snapshot.headers().firstValue("Cookie").orElseThrow());
        assertEquals("***", snapshot.headers().firstValue("iam-token").orElseThrow());
        assertEquals(List.of("***", "***"), snapshot.headers().allValues("X-Api-Key"));
        assertSafe(snapshot + " " + snapshot.uri() + " " + snapshot.headers().map() + " " + snapshot.method() + " " + snapshot.timeout()
            + " " + snapshot.version() + " " + snapshot.expectContinue() + " " + snapshot.bodyPublisher() + " " + entry.body());
    }

    private void assertSafeResponse(HttpResponseLog entry) {
        var diagnostic = new StringBuilder().append(entry.requestId()).append(' ').append(entry.statusCode()).append(' ')
            .append(entry.headers()).append(' ').append(entry.uri()).append(' ').append(entry.body()).append(' ').append(entry.error());
        if (nonNull(entry.headers()))
            diagnostic.append(entry.headers().map());
        if (nonNull(entry.error())) {
            var output = new StringWriter();
            entry.error().printStackTrace(new PrintWriter(output));
            diagnostic.append(output);
        }
        assertSafe(diagnostic.toString());
    }

    private void assertSafe(String diagnostic) {
        for (var secret : SECRETS)
            assertFalse(diagnostic.contains(secret), () -> "A secret appeared in a sanitized logging event: " + secret);
    }

    private record ReceivedRequest(String body, String query, HttpHeaders headers) {}
}
