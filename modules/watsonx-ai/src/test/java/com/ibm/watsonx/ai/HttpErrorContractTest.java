/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.ibm.watsonx.ai.core.exception.InvalidInputArgumentException;
import com.ibm.watsonx.ai.core.exception.JsonException;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;
import com.sun.net.httpserver.HttpServer;

@DisabledInNativeImage
class HttpErrorContractTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final String STANDARD_ERROR = """
        {"errors":[{"code":"invalid_input_argument","message":"Invalid input"}],"status_code":400}""";
    private static final String XML_ERROR = """
        <Error><Code>invalid_input_argument</Code><Message>Invalid input</Message><RequestId>trace-id</RequestId></Error>""";
    private static final String LARGE_ARRAY_ERROR = "[" + "12345,".repeat(175_000) + "0]";

    private final AtomicInteger attempts = new AtomicInteger();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null)
            server.stop(0);
    }

    @ParameterizedTest
    @MethodSource("unexpectedPayloads")
    void should_preserve_http_status_and_original_body(boolean async, String contentType, String body) throws Exception {
        var request = server(new Reply(403, contentType, body));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        assertEquals(403, failure.statusCode());
        assertEquals(body.isEmpty() ? "Status code: 403" : body, failure.getMessage());
        assertEquals(1, attempts.get());
    }

    private static Stream<Arguments> unexpectedPayloads() {
        var payloads = List.of(
            new Reply(403, "application/json", "{}"),
            new Reply(403, "application/json", "null"),
            new Reply(403, "application/json", "[1,2]"),
            new Reply(403, "application/json", "42"),
            new Reply(403, "application/json", "\"error\""),
            new Reply(403, "application/json", "{\"error\":"),
            new Reply(403, "application/json", "<html>Proxy unavailable</html>"),
            new Reply(403, "application/json", "{\"errors\":null}"),
            new Reply(403, "application/json", "{\"errors\":[]}"),
            new Reply(403, "application/json", "{\"errors\":[{}]}"),
            new Reply(403, "application/json", "{\"errors\":[null]}"),
            new Reply(403, "application/json", "{\"errors\":[\"unexpected\"]}"),
            new Reply(403, "application/json", "{\"errors\":{\"code\":\"invalid_input_argument\"}}"),
            new Reply(403, "application/json", "{\"errors\":[{\"code\":null,\"message\":\"Missing code\"}]}"),
            new Reply(403, "application/json", "{\"errors\":[{\"code\":[],\"message\":7}]}"),
            new Reply(403, "application/json", "{\"trace\":[],\"error\":null}"),
            new Reply(403, "application/json", "{\"error\":{\"code\":[],\"message\":{}}}"),
            new Reply(403, "application/json", "{\"errorCode\":{},\"errorMessage\":[]}"),
            new Reply(403, "application/json", "{\"code\":[],\"description\":17}"),
            new Reply(403, "application/problem+json", "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403}"),
            new Reply(403, "text/plain", "Proxy denied the request"),
            new Reply(403, "text/html", "<html>Forbidden</html>"),
            new Reply(403, "application/xml", "<Error><Code>MissingPermission</Code></Error>"),
            new Reply(403, "application/xml", "<Error>"),
            new Reply(403, "application/octet-stream", STANDARD_ERROR),
            new Reply(403, null, STANDARD_ERROR),
            new Reply(403, null, ""),
            new Reply(403, "application/json", ""),
            new Reply(403, "text/plain", " \n\t "));
        return Stream.of(false, true).flatMap(async -> payloads.stream().map(reply -> Arguments.of(async, reply.contentType(), reply.body())));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_decode_error_body_using_declared_charset(boolean async) throws Exception {
        var body = "Accès refusé: café";
        var request = server(StandardCharsets.ISO_8859_1, new Reply(403, "text/plain; charset=ISO-8859-1", body));

        var failure = failure(async, request, BodyHandlers.discarding(), false);

        assertEquals(403, failure.statusCode());
        assertEquals(body, failure.getMessage());
    }

    @ParameterizedTest
    @MethodSource("recognizedMediaTypes")
    void should_map_known_error_codes_for_recognized_media_types(boolean async, String contentType, String body) throws Exception {
        var request = server(new Reply(400, contentType, body));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        assertInstanceOf(InvalidInputArgumentException.class, failure);
        assertEquals(400, failure.statusCode());
        assertEquals(400, failure.details().orElseThrow().statusCode());
        assertEquals(body, failure.getMessage());
        assertNull(failure.getCause());
    }

    private static Stream<Arguments> recognizedMediaTypes() {
        return Stream.of(false, true).flatMap(async -> Stream.concat(
            Stream.of("application/json", "Application/JSON", "application/json; charset=UTF-8", "application/problem+json",
                "Application/Vnd.Ibm.Error+Json; charset=utf-8")
                .map(contentType -> Arguments.of(async, contentType, STANDARD_ERROR)),
            Stream.of("application/xml", "text/xml", "Application/XML; charset=UTF-8", "application/problem+xml")
                .map(contentType -> Arguments.of(async, contentType, XML_ERROR))));
    }

    @ParameterizedTest
    @MethodSource("knownErrorFormats")
    void should_preserve_known_json_error_formats(boolean async, String body, String code, String message) throws Exception {
        var request = server(new Reply(400, "application/json", body));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        var details = failure.details().orElseThrow();
        assertEquals(400, failure.statusCode());
        assertEquals(400, details.statusCode());
        assertEquals(code, details.errors().get(0).code());
        assertEquals(message, details.errors().get(0).message());
        assertEquals(body, failure.getMessage());
    }

    private static Stream<Arguments> knownErrorFormats() {
        return Stream.of(false, true).flatMap(async -> Stream.of(
            Arguments.of(async, "{\"errorCode\":\"BXNIM0415E\",\"errorMessage\":\"Invalid API key\"}", "BXNIM0415E", "Invalid API key"),
            Arguments.of(async, "{\"code\":400,\"message\":\"Invalid tool\",\"description\":\"Unknown tool\"}", "Unclassified", "Invalid tool"),
            Arguments.of(async,
                "{\"error\":{\"code\":\"invalid_input_argument\",\"message\":\"Invalid gateway input\",\"request_id\":\"request-id\"}}",
                "invalid_input_argument", "Invalid gateway input"),
            Arguments.of(async,
                "{\"trace\":\"trace-id\",\"status\":200,\"error\":{\"code\":\"invalid_input_argument\",\"message\":\"Invalid file\"}}",
                "invalid_input_argument", "Invalid file"),
            Arguments.of(async, "{\"error\":\"Schema does not exist\"}", "schema_event_does_not_exist", "Schema does not exist")));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_not_infer_error_format_without_recognized_media_type(boolean async) throws Exception {
        var request = server(new Reply(400, null, STANDARD_ERROR));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        assertEquals(WatsonxException.class, failure.getClass());
        assertEquals(STANDARD_ERROR, failure.getMessage());
        assertNull(failure.getCause());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_use_http_status_when_body_disagrees(boolean async) throws Exception {
        var body = STANDARD_ERROR.replace("\"status_code\":400", "\"status_code\":200");
        var request = server(new Reply(400, "application/json", body));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        assertInstanceOf(InvalidInputArgumentException.class, failure);
        assertEquals(400, failure.statusCode());
        assertEquals(400, failure.details().orElseThrow().statusCode());
        assertEquals(body, failure.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_keep_first_error_precedence(boolean async) throws Exception {
        var body = """
            {"errors":[{"code":"future_error","message":"First error"},
            {"code":"invalid_input_argument","message":"Second error"}]}""";
        var request = server(new Reply(400, "application/json", body));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        assertEquals(WatsonxException.class, failure.getClass());
        assertEquals("future_error", failure.details().orElseThrow().errors().get(0).code());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_preserve_parsing_failure_as_cause(boolean async) throws Exception {
        var body = "{\"error\":";
        var request = server(new Reply(400, "application/json", body));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        assertEquals(WatsonxException.class, failure.getClass());
        assertEquals(400, failure.statusCode());
        assertEquals(body, failure.getMessage());
        assertNotNull(failure.getCause());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_reject_non_object_error_before_reading_array_contents(boolean async) throws Exception {
        var body = "[\n{\"ignored\": tru";
        var request = server(new Reply(400, "application/json", body));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        assertEquals(400, failure.statusCode());
        assertEquals(body, failure.getMessage());
        assertTrue(failure.details().isEmpty());
        var cause = assertInstanceOf(JsonException.class, failure.getCause());
        assertTrue(cause.getMessage().contains("line 1, column "), cause.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_preserve_http_failure_for_a_large_json_root_array(boolean async) throws Exception {
        var request = server(new Reply(403, "application/json", LARGE_ARRAY_ERROR));

        var failure = failure(async, request, BodyHandlers.ofString(), false);

        assertEquals(WatsonxException.class, failure.getClass());
        assertEquals(403, failure.statusCode());
        assertEquals(LARGE_ARRAY_ERROR, failure.getMessage());
        assertInstanceOf(JsonException.class, failure.getCause());
        assertTrue(failure.details().isEmpty());
        assertEquals(1, attempts.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_retry_large_json_root_arrays_and_preserve_the_final_http_failure(boolean async) throws Exception {
        var lastBody = LARGE_ARRAY_ERROR + "\n";
        var request = server(new Reply(503, "application/json", LARGE_ARRAY_ERROR), new Reply(503, "application/json", lastBody));

        var failure = failure(async, request, BodyHandlers.ofString(), true);

        assertEquals(WatsonxException.class, failure.getClass());
        assertEquals(503, failure.statusCode());
        assertEquals(lastBody, failure.getMessage());
        assertInstanceOf(JsonException.class, failure.getCause());
        assertTrue(failure.details().isEmpty());
        assertEquals(2, attempts.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_retry_malformed_503_and_return_success(boolean async) throws Exception {
        var request = server(new Reply(503, "application/json", "{\"error\":"), new Reply(200, "text/plain", "recovered"));

        var response = send(async, request, BodyHandlers.ofString(), true);

        assertEquals(200, response.statusCode());
        assertEquals("recovered", response.body());
        assertEquals(2, attempts.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_keep_final_http_error_after_retry_exhaustion(boolean async) throws Exception {
        var lastBody = "{\"error\":\"last attempt\"";
        var request = server(new Reply(503, "application/json", "{\"error\":"), new Reply(503, "application/json", lastBody));

        var failure = failure(async, request, BodyHandlers.ofString(), true);

        assertEquals(503, failure.statusCode());
        assertEquals(lastBody, failure.getMessage());
        assertNotNull(failure.getCause());
        assertEquals(2, attempts.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_collect_error_body_independently_of_success_handler(boolean async) throws Exception {
        var body = "<html>Proxy error</html>";
        var request = server(new Reply(400, "application/json", body));
        List<BodyHandler<?>> handlers = List.of(BodyHandlers.discarding(), BodyHandlers.ofByteArray(), BodyHandlers.ofInputStream(),
            BodyHandlers.ofLines(), responseInfo -> fail("Success handler must not be invoked for an HTTP error"));

        for (var handler : handlers) {
            var failure = failure(async, request, handler, false);
            assertEquals(400, failure.statusCode());
            assertEquals(body, failure.getMessage());
        }
        assertEquals(handlers.size(), attempts.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_keep_success_body_handler_behavior(boolean async) throws Exception {
        var body = " \nOriginal UTF-8 body: è\n ";
        var request = server(new Reply(200, "text/plain", body));
        var handlerInvocations = new AtomicInteger();
        BodyHandler<Integer> handler = responseInfo -> {
            handlerInvocations.incrementAndGet();
            assertEquals(200, responseInfo.statusCode());
            return BodySubscribers.mapping(BodySubscribers.ofString(StandardCharsets.UTF_8), String::length);
        };

        var response = send(async, request, handler, false);

        assertEquals(body.length(), response.body());
        assertEquals(1, handlerInvocations.get());
        assertEquals(1, attempts.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_keep_success_body_stream_readable(boolean async) throws Exception {
        var body = " \nStreamed body\n ";
        var request = server(new Reply(200, "text/plain", body));

        var response = send(async, request, BodyHandlers.ofInputStream(), false);

        try (InputStream stream = response.body()) {
            assertEquals(body, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private <T> WatsonxException failure(boolean async, HttpRequest request, BodyHandler<T> handler, boolean retry) {
        if (async) {
            var failure = assertThrows(ExecutionException.class, () -> send(true, request, handler, retry));
            return assertInstanceOf(WatsonxException.class, failure.getCause());
        }
        return assertThrows(WatsonxException.class, () -> send(false, request, handler, retry));
    }

    private <T> HttpResponse<T> send(boolean async, HttpRequest request, BodyHandler<T> handler, boolean retry) throws Exception {
        var retryInterceptor = RetryInterceptor.builder().maxRetries(retry ? 1 : 0)
            .retryOn(WatsonxException.class, failure -> ((WatsonxException) failure).statusCode() == 503).build();
        if (async)
            return AsyncHttpClient.builder().httpClient(HTTP_CLIENT).interceptor(RetryInterceptor.ON_TOKEN_EXPIRED)
                .interceptor(retryInterceptor).build().send(request, handler).get(10, TimeUnit.SECONDS);
        return SyncHttpClient.builder().httpClient(HTTP_CLIENT).interceptor(RetryInterceptor.ON_TOKEN_EXPIRED)
            .interceptor(retryInterceptor).build().send(request, handler);
    }

    private HttpRequest server(Reply... replies) throws IOException {
        return server(StandardCharsets.UTF_8, replies);
    }

    private HttpRequest server(Charset charset, Reply... replies) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/error", exchange -> {
            try {
                var reply = replies[Math.min(attempts.getAndIncrement(), replies.length - 1)];
                if (reply.contentType() != null)
                    exchange.getResponseHeaders().set("Content-Type", reply.contentType());
                var bytes = reply.body().getBytes(charset);
                exchange.sendResponseHeaders(reply.status(), bytes.length == 0 ? -1 : bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally {
                exchange.close();
            }
        });
        server.start();
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/error"))
            .timeout(Duration.ofSeconds(5)).GET().build();
    }

    private record Reply(int status, String contentType, String body) {}
}
