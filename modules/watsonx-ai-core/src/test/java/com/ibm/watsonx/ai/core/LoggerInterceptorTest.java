/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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
            assertEquals("http-io-thread", threadNames.get(1));
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
}
