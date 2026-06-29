/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.ibm.watsonx.ai.core.http.AsyncHttpClient;
import com.ibm.watsonx.ai.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.LoggerInterceptor.LogMode;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

@ExtendWith(MockitoExtension.class)
public class LoggerInterceptorTest {

    @Mock
    HttpClient httpClient;

    @Nested
    @SuppressWarnings("unchecked")
    class DefaultConstructor {

        @Test
        void should_log_headers_properly_with_masking() throws Exception {

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("test-headers", "1")
                .header("test-headers", "2")
                .header("last-header", "last-header")
                .header("Authorization", "Bearer this_is_a_test_bearer_token_mask_it")
                .GET()
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);
            BodyHandler<String> bodyHandler = BodyHandlers.ofString();

            when(response.body()).thenReturn("response");
            when(response.statusCode()).thenReturn(200);
            when(response.headers()).thenReturn(HttpHeaders.of(
                Map.of(
                    "headers", List.of("1", "2"),
                    "empty-header", List.of(),
                    "single-header", List.of("single")
                ),
                (k, v) -> true
            ));
            when(chain.proceed(request, bodyHandler)).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, bodyHandler, 0, chain));
            verify(chain, times(1)).proceed(request, bodyHandler);
        }

        @Test
        void should_log_base64_body_correctly() throws Exception {

            String body = "data:image/png;base64,AAAAAAAAAAAAAAAABBBBBBBBBBBBBBBCCCCCCCCCCCCCCCDDDDDDDDDDDDDD";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .POST(BodyPublishers.ofString(body))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain));
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_log_json_body_when_content_type_is_json() throws Exception {

            String json = "{\"name\":\"Alan\",\"last_name\":\"Wake\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(json))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(response.statusCode()).thenReturn(404);
            when(response.body()).thenReturn(json);
            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain));
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_log_non_json_body_even_with_json_content_type_header() throws Exception {

            String message = "This is clearly not a json";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(message))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(response.statusCode()).thenReturn(404);
            when(response.body()).thenReturn(message);
            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain));
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_log_text_body_with_text_content_type() throws Exception {

            String body = "Hello, world!";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(response.body()).thenReturn("response");
            when(response.statusCode()).thenReturn(200);
            when(response.headers()).thenReturn(null);
            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain));
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_handle_null_response_body_when_logging() throws Exception {

            String body = "Hello, world!";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(response.body()).thenReturn(null);
            when(response.statusCode()).thenReturn(204);
            when(response.headers()).thenReturn(null);
            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain));
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_log_byte_array_response_body() throws Exception {

            String body = "Hello, world!";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<byte[]> response = mock(HttpResponse.class);

            when(response.body()).thenReturn("This is a byte response".getBytes());
            when(response.statusCode()).thenReturn(200);
            when(chain.proceed(request, BodyHandlers.ofByteArray())).thenReturn(response);

            var result = assertDoesNotThrow(
                () -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofByteArray(), 0, chain));
            assertEquals("This is a byte response", new String(result.body()));
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_log_input_stream_response_body() throws Exception {

            InputStream requestBody = new ByteArrayInputStream("Hello, world!".getBytes());
            InputStream responseBody = new ByteArrayInputStream("Hello!".getBytes());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> requestBody))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<InputStream> response = mock(HttpResponse.class);

            when(response.body()).thenReturn(responseBody);
            when(response.statusCode()).thenReturn(200);
            when(chain.proceed(request, BodyHandlers.ofInputStream())).thenReturn(response);

            var result = assertDoesNotThrow(
                () -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofInputStream(), 0, chain));
            assertEquals(responseBody, result.body());
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_log_file_response_body() throws Exception {

            Path path = Path.of(getClass().getClassLoader().getResource("test.txt").toURI());
            BodyHandler<Path> bodyHandler = HttpResponse.BodyHandlers.ofFile(path);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .POST(HttpRequest.BodyPublishers.ofFile(path))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<Path> response = mock(HttpResponse.class);

            when(response.body()).thenReturn(path);
            when(response.statusCode()).thenReturn(200);
            when(chain.proceed(request, bodyHandler)).thenReturn(response);

            var result = assertDoesNotThrow(
                () -> interceptor.intercept(request, bodyHandler, 0, chain));
            assertEquals("This is a test file.", Files.readString(result.body()));
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_log_stream_of_lines_response() throws Exception {

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("my-header", "header")
                .POST(BodyPublishers.noBody())
                .build();

            Stream<String> lines = Stream.of("line one", "line two", "line three");

            HttpResponse<Stream<String>> response = mock(HttpResponse.class);
            when(response.body()).thenReturn(lines);
            when(response.statusCode()).thenReturn(200);
            when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (k, v) -> true));

            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            when(chain.proceed(request, BodyHandlers.ofLines())).thenReturn(response);

            LoggerInterceptor interceptor = new LoggerInterceptor();

            var result = assertDoesNotThrow(
                () -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofLines(), 0, chain));

            assertEquals(result.body(), lines);
            verify(chain).proceed(any(), any());
        }

        @Test
        void should_log_async_requests_and_responses() throws Exception {

            String body = "Hello, world!";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor();
            LoggerInterceptor.AsyncChain chain = mock(LoggerInterceptor.AsyncChain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(response.body()).thenReturn("response");
            when(response.statusCode()).thenReturn(200);
            when(response.headers()).thenReturn(null);
            when(chain.proceed(eq(request), eq(BodyHandlers.ofString()))).thenReturn(completedFuture(response));

            assertDoesNotThrow(() -> interceptor
                .intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain).get());
            verify(chain).proceed(any(), any());
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        void should_use_correct_executors() throws Exception {

            var threadNames = new ArrayList<>();

            var mockHttpResponse = mock(HttpResponse.class);
            when(httpClient.sendAsync(any(), any(BodyHandler.class))).thenReturn(completedFuture(mockHttpResponse));
            when(httpClient.executor()).thenReturn(Optional.of(ExecutorProvider.ioExecutor()));

            LoggerInterceptor interceptor = new LoggerInterceptor();

            var client = AsyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(interceptor)
                .interceptor(new AsyncHttpInterceptor() {
                    @Override
                    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
                        int index, AsyncChain chain) {
                        return chain.proceed(request, bodyHandler)
                            .thenApplyAsync(r -> {
                                threadNames.add(Thread.currentThread().getName());
                                return r;
                            }, ExecutorProvider.cpuExecutor())
                            .thenApplyAsync(r -> {
                                threadNames.add(Thread.currentThread().getName());
                                return r;
                            }, ExecutorProvider.ioExecutor());
                    }
                }).build();

            client.send(HttpRequest.newBuilder()
                .uri(URI.create("https://test.com"))
                .GET().build(), BodyHandlers.ofString())
                .get(3, TimeUnit.SECONDS);

            assertEquals(2, threadNames.size());
            assertEquals("ForkJoinPool.commonPool-worker-1", threadNames.get(0));
            assertTrue(String.valueOf(threadNames.get(1)).startsWith("http-io-"));
        }
    }

    @Nested
    @SuppressWarnings("unchecked")
    class Request {

        @Test
        void should_log_only_request_when_log_mode_is_request() throws Exception {

            String body = "Hello, world!";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor(LogMode.REQUEST);
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain));
            verify(chain).proceed(any(), any());
        }
    }

    @Nested
    @SuppressWarnings("unchecked")
    public class Response {

        @Test
        void should_log_only_response_when_log_mode_is_response() throws Exception {

            String body = "Hello, world!";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor(LogMode.RESPONSE);
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(response.body()).thenReturn("response");
            when(response.statusCode()).thenReturn(200);
            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);
            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain));
            verify(chain).proceed(any(), any());
        }
    }

    @Nested
    @SuppressWarnings("unchecked")
    class Both {

        @Test
        void should_log_both_request_and_response_when_log_mode_is_both() throws Exception {

            String body = "Hello, world!";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            LoggerInterceptor interceptor = new LoggerInterceptor(LogMode.BOTH);
            LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            when(response.body()).thenReturn("response");
            when(response.statusCode()).thenReturn(200);
            when(response.headers()).thenReturn(null);
            when(chain.proceed(request, BodyHandlers.ofString())).thenReturn(response);

            assertDoesNotThrow(() -> interceptor.intercept(request, HttpResponse.BodyHandlers.ofString(), 0, chain));
            verify(chain).proceed(any(), any());
        }
    }

    @Nested
    @SuppressWarnings("unchecked")
    class Masking {

        @Test
        void should_mask_apikey_in_form_urlencoded_request_body() throws Exception {

            String body = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=MY_SUPER_SECRET_KEY";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(body))
                .build();

            List<String> logs = captureLogs(() -> intercept(new LoggerInterceptor(LogMode.REQUEST), request));

            String requestLog = findLog(logs, "Request:");
            assertFalse(requestLog.contains("MY_SUPER_SECRET_KEY"), "API key must not be logged in clear text");
            assertTrue(requestLog.contains("apikey=***"), () -> "API key must be masked, but was: " + requestLog);
        }

        @Test
        void should_mask_password_in_form_urlencoded_request_body() throws Exception {

            String body = "grant_type=password&username=admin&password=MY_SUPER_SECRET_PWD&scope=openid";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(body))
                .build();

            List<String> logs = captureLogs(() -> intercept(new LoggerInterceptor(LogMode.REQUEST), request));

            String requestLog = findLog(logs, "Request:");
            assertFalse(requestLog.contains("MY_SUPER_SECRET_PWD"), "Password must not be logged in clear text");
            assertTrue(requestLog.contains("password=***"), () -> "Password must be masked, but was: " + requestLog);
        }

        @Test
        void should_mask_apikey_in_json_request_body() throws Exception {

            String body = "{\"apikey\":\"MY_SUPER_SECRET_KEY\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();

            List<String> logs = captureLogs(() -> intercept(new LoggerInterceptor(LogMode.REQUEST), request));

            String requestLog = findLog(logs, "Request:");
            assertFalse(requestLog.contains("MY_SUPER_SECRET_KEY"), "API key must not be logged in clear text");
            assertTrue(requestLog.contains("\"***\""), () -> "API key must be masked, but was: " + requestLog);
        }

        @Test
        void should_mask_access_token_in_json_response_body() throws Exception {

            // This is the shape of the token endpoint response.
            String responseBody = "{\"access_token\":\"MY_SUPER_SECRET_TOKEN\",\"expiration\":123}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .GET()
                .build();

            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.body()).thenReturn(responseBody);
            when(response.statusCode()).thenReturn(200);
            when(response.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));

            List<String> logs = captureLogs(() -> intercept(new LoggerInterceptor(LogMode.RESPONSE), request, response));

            String responseLog = findLog(logs, "Response:");
            assertFalse(responseLog.contains("MY_SUPER_SECRET_TOKEN"), "Access token must not be logged in clear text");
            assertTrue(responseLog.contains("\"***\""), () -> "Access token must be masked, but was: " + responseLog);
        }

        @Test
        void should_not_log_anything_when_log_mode_is_disabled() throws Exception {

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString("apikey=MY_SUPER_SECRET_KEY"))
                .build();

            List<String> logs = captureLogs(() -> intercept(new LoggerInterceptor(LogMode.DISABLED), request));

            assertTrue(logs.isEmpty(), () -> "Nothing should be logged when disabled, but was: " + logs);
        }

        @Test
        void should_not_read_body_when_info_level_is_disabled() throws Exception {

            // Logging is enabled on the interceptor, but the log level is raised above INFO: the
            // interceptor must short-circuit and avoid reading/masking the body altogether.
            AtomicBoolean bodyRead = new AtomicBoolean(false);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(trackingPublisher("apikey=MY_SUPER_SECRET_KEY", bodyRead))
                .build();

            String loggerName = LoggerInterceptor.class.getName();
            Configurator.setLevel(loggerName, Level.WARN);
            try {
                List<String> logs = captureLogs(() -> intercept(new LoggerInterceptor(LogMode.REQUEST), request));
                assertTrue(logs.isEmpty(), () -> "Nothing should be logged when INFO is disabled, but was: " + logs);
                assertFalse(bodyRead.get(), "Request body must not be read when INFO level is disabled");
            } finally {
                Configurator.setLevel(loggerName, Level.INFO);
            }
        }

        private HttpResponse<String> intercept(LoggerInterceptor interceptor, HttpRequest request) {
            HttpResponse<String> response = mock(HttpResponse.class);
            return intercept(interceptor, request, response);
        }

        private HttpResponse<String> intercept(LoggerInterceptor interceptor, HttpRequest request, HttpResponse<String> response) {
            try {
                LoggerInterceptor.Chain chain = mock(LoggerInterceptor.Chain.class);
                BodyHandler<String> bodyHandler = BodyHandlers.ofString();
                when(chain.proceed(request, bodyHandler)).thenReturn(response);
                return interceptor.intercept(request, bodyHandler, 0, chain);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //
    // Attaches an in-memory Log4j2 appender to the LoggerInterceptor logger, runs the given action,
    // and returns every message logged during its execution.
    //
    private static List<String> captureLogs(Runnable action) {
        var logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(LoggerInterceptor.class);
        List<String> messages = new CopyOnWriteArrayList<>();
        var appender = new AbstractAppender("test-capture", null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
        return messages;
    }

    private static String findLog(List<String> logs, String prefix) {
        return logs.stream()
            .filter(msg -> msg.startsWith(prefix))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No log found starting with '" + prefix + "' in: " + logs));
    }

    //
    // Wraps a string body publisher and flips the given flag the first time it is subscribed,
    // so a test can assert whether the interceptor actually read the request body.
    //
    private static BodyPublisher trackingPublisher(String body, AtomicBoolean subscribed) {
        BodyPublisher delegate = BodyPublishers.ofString(body);
        return new BodyPublisher() {
            @Override
            public long contentLength() {
                return delegate.contentLength();
            }

            @Override
            public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                subscribed.set(true);
                delegate.subscribe(subscriber);
            }
        };
    }
}
