/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.exception.AuthenticationTokenExpiredException;
import com.ibm.watsonx.ai.core.exception.AuthorizationRejectedException;
import com.ibm.watsonx.ai.core.exception.InvalidInputArgumentException;
import com.ibm.watsonx.ai.core.exception.InvalidRequestEntityException;
import com.ibm.watsonx.ai.core.exception.JsonTypeErrorException;
import com.ibm.watsonx.ai.core.exception.JsonValidationErrorException;
import com.ibm.watsonx.ai.core.exception.ModelNoSupportForFunctionException;
import com.ibm.watsonx.ai.core.exception.ModelNotSupportedException;
import com.ibm.watsonx.ai.core.exception.TokenQuotaReachedException;
import com.ibm.watsonx.ai.core.exception.UserAuthorizationFailedException;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.http.interceptors.AuthenticationInterceptor;
import com.ibm.watsonx.ai.core.http.interceptors.RetryInterceptor;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("unchecked")
@DisabledInNativeImage
public class SyncHttpClientTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Requests {

        @Mock
        HttpClient httpClient;

        @Mock
        HttpRequest httpRequest;

        @Mock
        HttpResponse<String> httpResponse;

        @Mock
        SyncHttpInterceptor interceptor1;

        @Mock
        SyncHttpInterceptor interceptor2;

        Answer<HttpResponse<String>> CHAIN_MOCK = invocation -> {
            HttpRequest req = invocation.getArgument(0);
            BodyHandler<String> bh = invocation.getArgument(1);
            SyncHttpInterceptor.Chain chain = invocation.getArgument(3);
            return chain.proceed(req, bh);
        };

        @BeforeEach
        void setUp() {
            when(httpRequest.uri()).thenReturn(URI.create("https://test.com"));
            when(httpRequest.method()).thenReturn("GET");
            when(httpRequest.headers()).thenReturn(HttpHeaders.of(Map.of(), (k, v) -> true));
        }

        @Test
        void should_send_request_when_interceptors_are_configured() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptor(interceptor1)
                .interceptor(interceptor2)
                .build();

            when(interceptor1.intercept(any(), eq(handler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(interceptor2.intercept(any(), eq(handler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpResponse.statusCode())
                .thenReturn(200);

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);

            var response = client.send(httpRequest, handler);

            assertEquals(httpResponse, response);

            InOrder inOrder = inOrder(interceptor1, interceptor2);
            inOrder.verify(interceptor1).intercept(any(), eq(handler), anyInt(), any());
            inOrder.verify(interceptor2).intercept(any(), eq(handler), anyInt(), any());
        }

        @Test
        void should_send_request_when_interceptors_are_provided_as_list() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptors(List.of(interceptor1, interceptor2))
                .build();

            when(interceptor1.intercept(any(), eq(handler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(interceptor2.intercept(any(), eq(handler), anyInt(), any()))
                .thenAnswer(CHAIN_MOCK);

            when(httpResponse.statusCode())
                .thenReturn(200);

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);

            var response = client.send(httpRequest, handler);

            assertEquals(httpResponse, response);

            InOrder inOrder = inOrder(interceptor1, interceptor2);
            inOrder.verify(interceptor1).intercept(any(), eq(handler), anyInt(), any());
            inOrder.verify(interceptor2).intercept(any(), eq(handler), anyInt(), any());
        }

        @Test
        void should_not_be_affected_by_external_list_mutation_after_build() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);

            List<SyncHttpInterceptor> list = new ArrayList<>();
            list.add(interceptor1);

            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .interceptors(list)
                .build();

            // Mutating the original list after build must not change the client's interceptors.
            list.clear();

            when(interceptor1.intercept(any(), eq(handler), anyInt(), any())).thenAnswer(CHAIN_MOCK);
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);

            assertDoesNotThrow(() -> client.send(httpRequest, handler));
            verify(interceptor1).intercept(any(), eq(handler), anyInt(), any());
        }

        @Test
        void should_send_request_when_no_interceptors_are_configured() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(200);

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);

            var response = client.send(httpRequest, handler);
            assertEquals(httpResponse, response);
        }

        @Test
        void should_throw_watsonx_exception_when_response_status_is_401() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(401);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "authentication_token_not_valid",
                                    "message": "Failed to authenticate the request due to invalid token: Failed to parse and verify token",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "23e11747002c4d2919987401b745f6a7",
                            "status_code": 401
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(WatsonxException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_throw_watsonx_exception_when_401_response_has_no_body() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(401);

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(WatsonxException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_authentication_token_expired_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(401);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "authentication_token_expired",
                                    "message": "Failed to authenticate the request due to an expired token",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "d7b410d8847781df5c520b2269367bc7",
                            "status_code": 401
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(AuthenticationTokenExpiredException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_authorization_rejected_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(403);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "authorization_rejected",
                                    "message": "Authorization rejected",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
                            "status_code": 403
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(AuthorizationRejectedException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_invalid_input_argument_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(400);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "invalid_input_argument",
                                    "message": "Invalid input argument provided",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7",
                            "status_code": 400
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(InvalidInputArgumentException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_invalid_request_entity_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(422);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "invalid_request_entity",
                                    "message": "The request entity is invalid",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8",
                            "status_code": 422
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(InvalidRequestEntityException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_json_type_error_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(400);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "json_type_error",
                                    "message": "JSON type error occurred",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9",
                            "status_code": 400
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(JsonTypeErrorException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_json_validation_error_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(400);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "json_validation_error",
                                    "message": "JSON validation failed",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0",
                            "status_code": 400
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(JsonValidationErrorException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_model_not_supported_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(400);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "model_not_supported",
                                    "message": "The requested model is not supported",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1",
                            "status_code": 400
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(ModelNotSupportedException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_model_no_support_for_function_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(400);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "model_no_support_for_function",
                                    "message": "The model does not support the requested function",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2",
                            "status_code": 400
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(ModelNoSupportForFunctionException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_token_quota_reached_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(429);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "token_quota_reached",
                                    "message": "Token quota has been reached",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3",
                            "status_code": 429
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(TokenQuotaReachedException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_map_user_authorization_failed_exception() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(403);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "user_authorization_failed",
                                    "message": "User authorization failed",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4",
                            "status_code": 403
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(UserAuthorizationFailedException.class, () -> client.send(httpRequest, handler));
        }

        @Test
        void should_rethrow_generic_exception_without_mapping() throws Exception {

            BodyHandler<String> handler = mock(BodyHandler.class);
            SyncHttpClient client = SyncHttpClient.builder()
                .httpClient(httpClient)
                .build();

            when(httpResponse.statusCode())
                .thenReturn(500);

            when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            when(httpResponse.body())
                .thenReturn(
                    """
                        {
                            "errors": [
                                {
                                    "code": "exception_that_doesn_t_exist",
                                    "message": "User authorization failed",
                                    "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#text-chat"
                                }
                            ],
                            "trace": "i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4",
                            "status_code": 500
                        }""");

            when(httpClient.send(any(), any(BodyHandler.class))).thenReturn(httpResponse);
            assertThrows(WatsonxException.class, () -> client.send(httpRequest, handler));
        }
    }

    @Nested
    class Deadlines {

        private final HttpClient httpClient = mock(HttpClient.class);
        private final AtomicLong clock = new AtomicLong();
        private final List<Long> delays = new ArrayList<>();

        @Test
        void should_limit_retry_attempts_and_backoff_to_the_shared_budget() throws Exception {
            var attempts = new ArrayList<Duration>();
            when(httpClient.send(any(), any())).thenAnswer(invocation -> {
                HttpRequest request = invocation.getArgument(0);
                attempts.add(request.timeout().orElseThrow());
                throw new WatsonxException(503);
            });
            var retry = RetryInterceptor.builder().maxRetries(10).retryInterval(Duration.ofMillis(60)).retryOn(WatsonxException.class).build();
            var client = client(List.of(retry));

            assertThrows(HttpTimeoutException.class,
                () -> client.send(request(Duration.ofMillis(80)), BodyHandlers.ofString(), Duration.ofMillis(100)));

            assertEquals(List.of(Duration.ofMillis(80), Duration.ofMillis(40)), attempts);
            assertEquals(List.of(Duration.ofMillis(60).toNanos(), Duration.ofMillis(40).toNanos()), delays);
            assertEquals(Duration.ofMillis(100).toNanos(), clock.get());
        }

        @Test
        void should_share_the_deadline_between_nested_token_and_status_retries() throws Exception {
            var attempts = new ArrayList<Duration>();
            when(httpClient.send(any(), any())).thenAnswer(invocation -> {
                HttpRequest request = invocation.getArgument(0);
                attempts.add(request.timeout().orElseThrow());
                if (attempts.size() == 2)
                    throw new AuthenticationTokenExpiredException("expired", 401, null);
                throw new WatsonxException(503);
            });
            var tokens = RetryInterceptor.builder().maxRetries(3).retryOn(AuthenticationTokenExpiredException.class).build();
            var statuses = RetryInterceptor.builder().maxRetries(3).retryInterval(Duration.ofMillis(60))
                .retryOn(WatsonxException.class, error -> ((WatsonxException) error).statusCode() == 503).build();
            var client = client(List.of(tokens, statuses));

            assertThrows(HttpTimeoutException.class,
                () -> client.send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofMillis(100)));

            assertEquals(List.of(Duration.ofMillis(100), Duration.ofMillis(40), Duration.ofMillis(40)), attempts);
            assertEquals(Duration.ofMillis(100).toNanos(), clock.get());
        }

        @Test
        void should_not_send_after_an_interceptor_exhausts_the_budget() throws Exception {
            var interceptor = new SyncHttpInterceptor() {
                @Override
                public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain)
                    throws IOException, InterruptedException {
                    clock.addAndGet(Duration.ofMillis(100).toNanos());
                    return chain.proceed(request, bodyHandler);
                }
            };
            var client = client(List.of(interceptor));

            assertThrows(HttpTimeoutException.class,
                () -> client.send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofMillis(100)));

            verifyNoInteractions(httpClient);
        }

        @Test
        void should_preserve_interruptions_during_retry_backoff() throws Exception {
            var interrupted = new InterruptedException("interrupted retry");
            when(httpClient.send(any(), any())).thenThrow(new WatsonxException(503));
            var retry = RetryInterceptor.builder().retryInterval(Duration.ofMillis(10)).retryOn(WatsonxException.class).build();
            var client = new SyncHttpClient(httpClient, List.of(retry), clock::get, nanos -> {
                throw interrupted;
            });

            assertSame(interrupted, assertThrows(InterruptedException.class,
                () -> client.send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofMillis(100))));

            verify(httpClient, times(1)).send(any(), any());
        }

        @Test
        void should_not_retry_interrupted_backoff_even_when_retry_rules_match() throws Exception {
            for (var retryType : List.of(Exception.class, InterruptedException.class)) {
                var transport = mock(HttpClient.class);
                var interrupted = new InterruptedException("interrupted retry");
                var retryDecisions = new AtomicInteger();
                var pauses = new AtomicInteger();
                when(transport.send(any(), any())).thenThrow(new IOException("failed request"))
                    .thenAnswer(invocation -> successfulResponse());
                var retry = RetryInterceptor.builder().maxRetries(3).retryInterval(Duration.ofMillis(10))
                    .retryOn(IOException.class).retryOn(retryType, error -> {
                        retryDecisions.incrementAndGet();
                        return true;
                    }).build();
                var client = new SyncHttpClient(transport, List.of(retry), clock::get, nanos -> {
                    if (pauses.incrementAndGet() == 1)
                        throw interrupted;
                });

                assertSame(interrupted, assertThrows(InterruptedException.class,
                    () -> client.send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofSeconds(1))));

                assertEquals(0, retryDecisions.get());
                assertEquals(1, pauses.get());
                verify(transport, times(1)).send(any(), any());
            }
        }

        @Test
        void should_stop_even_when_custom_retry_rules_match_timeout_exceptions() throws Exception {
            var retryDecisions = new AtomicInteger();
            when(httpClient.send(any(), any())).thenAnswer(invocation -> {
                clock.addAndGet(Duration.ofMillis(100).toNanos());
                throw new IOException("failed attempt");
            });
            var retry = RetryInterceptor.builder().maxRetries(10).retryOn(IOException.class, error -> {
                retryDecisions.incrementAndGet();
                return true;
            }).build();

            assertThrows(HttpTimeoutException.class,
                () -> client(List.of(retry)).send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofMillis(100)));

            assertEquals(1, retryDecisions.get());
            verify(httpClient, times(1)).send(any(), any());
        }

        @Test
        void should_return_late_success_so_the_caller_can_clean_up_response_resources() throws Exception {
            var response = successfulResponse();
            when(httpClient.send(any(), any())).thenAnswer(invocation -> {
                clock.addAndGet(Duration.ofMillis(101).toNanos());
                return response;
            });

            assertSame(response,
                client(List.of()).send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofMillis(100)));
        }

        @Test
        void should_keep_the_existing_send_operation_without_a_shared_deadline() throws Exception {
            var response = successfulResponse();
            var request = request(Duration.ofSeconds(1));
            when(httpClient.send(any(), any())).thenAnswer(invocation -> {
                HttpRequest attempt = invocation.getArgument(0);
                assertEquals(request.timeout(), attempt.timeout());
                clock.addAndGet(Duration.ofSeconds(2).toNanos());
                return response;
            });

            assertSame(response, client(List.of()).send(request, BodyHandlers.ofString()));
        }

        @Test
        void should_preserve_the_io_failure_as_the_cause_when_the_budget_expires() throws Exception {
            var failure = new IOException("failed request");
            when(httpClient.send(any(), any())).thenAnswer(invocation -> {
                clock.addAndGet(Duration.ofMillis(100).toNanos());
                throw failure;
            });

            var timeout = assertThrows(HttpTimeoutException.class,
                () -> client(List.of()).send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofMillis(100)));

            assertSame(failure, timeout.getCause());
        }

        @Test
        void should_support_nanosecond_budgets_and_monotonic_clock_wraparound() throws Exception {
            clock.set(Long.MAX_VALUE - 5);
            when(httpClient.send(any(), any())).thenAnswer(invocation -> {
                HttpRequest attempt = invocation.getArgument(0);
                assertEquals(Duration.ofNanos(10), attempt.timeout().orElseThrow());
                clock.addAndGet(11);
                throw new WatsonxException(503);
            });

            assertThrows(HttpTimeoutException.class,
                () -> client(List.of()).send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofNanos(10)));
        }

        @Test
        void should_reject_non_positive_budgets_before_sending() {
            var client = client(List.of());

            assertThrows(IllegalArgumentException.class,
                () -> client.send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ZERO));
            assertThrows(IllegalArgumentException.class,
                () -> client.send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofNanos(-1)));

            verifyNoInteractions(httpClient);
        }

        @Test
        void should_bound_retry_backoff_after_a_real_503_response() throws Exception {
            var requests = new AtomicInteger();
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                requests.incrementAndGet();
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
            });
            server.start();
            try {
                var delegate = HttpClient.newHttpClient();
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                    .timeout(Duration.ofSeconds(30)).GET().build();
                var retry = RetryInterceptor.builder().maxRetries(10).retryInterval(Duration.ofSeconds(60)).retryOn(WatsonxException.class).build();
                var client = new SyncHttpClient(delegate, List.of(retry), clock::get, nanos -> {
                    delays.add(nanos);
                    clock.addAndGet(nanos);
                });

                assertThrows(HttpTimeoutException.class, () -> client.send(request, BodyHandlers.ofString(), Duration.ofSeconds(30)));

                assertEquals(1, requests.get());
                assertEquals(List.of(Duration.ofSeconds(30).toNanos()), delays);
                assertEquals(Duration.ofSeconds(30).toNanos(), clock.get());
            } finally {
                server.stop(0);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_close_error_input_streams_after_reading_the_body() throws Exception {
            var stream = mock(InputStream.class);
            when(stream.readAllBytes()).thenReturn("unavailable".getBytes(StandardCharsets.UTF_8));
            HttpResponse<InputStream> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(503);
            when(response.body()).thenReturn(stream);
            when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
            when(httpClient.send(any(), any())).thenAnswer(invocation -> response);

            var failure = assertThrows(WatsonxException.class,
                () -> client(List.of()).send(request(Duration.ofSeconds(1)), BodyHandlers.ofInputStream(), Duration.ofMillis(100)));

            assertEquals(503, failure.statusCode());
            assertEquals("unavailable", failure.getMessage());
            verify(stream).close();
        }

        @Test
        void should_preserve_status_and_close_materialized_error_stream_when_reading_fails() throws Exception {
            var stream = mock(InputStream.class);
            var readFailure = new IOException("error response read failed");
            when(stream.readAllBytes()).thenThrow(readFailure);
            HttpResponse<InputStream> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(503);
            when(response.body()).thenReturn(stream);
            when(httpClient.send(any(), any(BodyHandler.class))).thenAnswer(invocation -> response);

            var failure = assertThrows(WatsonxException.class,
                () -> client(List.of()).send(request(Duration.ofSeconds(1)), BodyHandlers.ofInputStream(), Duration.ofMillis(100)));

            assertEquals(503, failure.statusCode());
            assertEquals("Status code: 503", failure.getMessage());
            var cause = failure.getCause();
            while (cause != null && cause.getCause() != null)
                cause = cause.getCause();
            assertSame(readFailure, cause);
            verify(stream).close();
        }

        @Test
        void should_preserve_materialized_error_body_containing_only_whitespace() throws Exception {
            String body = " \t\n ";
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(503);
            when(response.body()).thenReturn(body);
            when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
            when(httpClient.send(any(), any(BodyHandler.class))).thenAnswer(invocation -> response);

            var failure = assertThrows(WatsonxException.class,
                () -> client(List.of()).send(request(Duration.ofSeconds(1)), BodyHandlers.ofString(), Duration.ofMillis(100)));

            assertEquals(503, failure.statusCode());
            assertEquals(body, failure.getMessage());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_close_error_line_streams_after_reading_the_body() throws Exception {
            var closed = new AtomicBoolean();
            var stream = Stream.of("unavailable", "try later").onClose(() -> closed.set(true));
            HttpResponse<Stream<String>> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(503);
            when(response.body()).thenReturn(stream);
            when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
            when(httpClient.send(any(), any())).thenAnswer(invocation -> response);

            var failure = assertThrows(WatsonxException.class,
                () -> client(List.of()).send(request(Duration.ofSeconds(1)), BodyHandlers.ofLines(), Duration.ofMillis(100)));

            assertEquals(503, failure.statusCode());
            assertEquals("unavailable\ntry later", failure.getMessage());
            assertTrue(closed.get());
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_allow_the_caller_to_read_successful_streams_after_the_budget_expires() throws Exception {
            var stream = mock(InputStream.class);
            when(stream.readAllBytes()).thenReturn("body".getBytes(StandardCharsets.UTF_8));
            HttpResponse<InputStream> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(response.body()).thenReturn(stream);
            when(httpClient.send(any(), any())).thenAnswer(invocation -> response);

            var result = client(List.of()).send(request(Duration.ofSeconds(1)), BodyHandlers.ofInputStream(), Duration.ofMillis(100));
            clock.addAndGet(Duration.ofMillis(101).toNanos());
            verify(stream, never()).close();
            try (var body = result.body()) {
                assertEquals("body", new String(body.readAllBytes(), StandardCharsets.UTF_8));
            }

            verify(stream).close();
        }

        private SyncHttpClient client(List<SyncHttpInterceptor> interceptors) {
            return new SyncHttpClient(httpClient, interceptors, clock::get, nanos -> {
                delays.add(nanos);
                clock.addAndGet(nanos);
            });
        }

        private HttpRequest request(Duration timeout) {
            return HttpRequest.newBuilder(URI.create("https://example.com")).timeout(timeout).GET().build();
        }

        @SuppressWarnings("unchecked")
        private HttpResponse<String> successfulResponse() {
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            return response;
        }
    }

    @Nested
    class Authentication {

        @Test
        void should_not_send_when_token_acquisition_consumes_the_shared_budget() {
            var transport = mock(HttpClient.class);
            var authenticator = mock(Authenticator.class);
            var clock = new AtomicLong();
            when(authenticator.token()).thenAnswer(invocation -> {
                clock.addAndGet(Duration.ofSeconds(1).toNanos());
                return "token";
            });
            when(authenticator.scheme()).thenReturn("Bearer");
            var client = new SyncHttpClient(transport, List.of(new AuthenticationInterceptor(authenticator)), clock::get, nanos -> {});

            assertThrows(HttpTimeoutException.class,
                () -> client.send(request(), BodyHandlers.ofString(), Duration.ofSeconds(1)));

            verify(authenticator).token();
            verifyNoInteractions(transport);
        }

        @Test
        void should_use_the_current_thread_local_token_for_each_request() throws Exception {
            assertContextTokens(new ThreadLocal<>());
        }

        @Test
        void should_use_the_current_inherited_thread_local_token_for_each_request() throws Exception {
            assertContextTokens(new InheritableThreadLocal<>());
        }

        @Test
        void should_subtract_token_acquisition_time_from_the_http_attempt_budget() throws Exception {
            var transport = mock(HttpClient.class);
            var authenticator = mock(Authenticator.class);
            var clock = new AtomicLong();
            when(authenticator.token()).thenAnswer(invocation -> {
                clock.addAndGet(Duration.ofMillis(600).toNanos());
                return "token";
            });
            when(authenticator.scheme()).thenReturn("Bearer");
            var response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(transport.send(any(), any())).thenAnswer(invocation -> {
                HttpRequest authenticated = invocation.getArgument(0);
                assertEquals(Duration.ofMillis(400), authenticated.timeout().orElseThrow());
                return response;
            });
            var client = new SyncHttpClient(transport, List.of(new AuthenticationInterceptor(authenticator)), clock::get, nanos -> {});

            assertSame(response, client.send(request(), BodyHandlers.ofString(), Duration.ofSeconds(1)));
        }

        @Test
        void should_preserve_authentication_failure_without_wrapping_it() {
            var transport = mock(HttpClient.class);
            var authenticator = mock(Authenticator.class);
            var failure = new IllegalStateException("Token acquisition failed");
            when(authenticator.token()).thenThrow(failure);

            assertSame(failure, assertThrows(IllegalStateException.class,
                () -> client(transport, authenticator).send(request(), BodyHandlers.ofString(), Duration.ofSeconds(5))));

            verifyNoInteractions(transport);
        }

        @Test
        void should_keep_unbudgeted_token_acquisition_on_the_calling_thread() throws Exception {
            assertTokenAcquisitionOnCallingThread(false);
        }

        @Test
        void should_keep_budgeted_token_acquisition_on_the_calling_thread() throws Exception {
            assertTokenAcquisitionOnCallingThread(true);
        }

        private void assertTokenAcquisitionOnCallingThread(boolean budgeted) throws Exception {
            var transport = mock(HttpClient.class);
            var authenticator = mock(Authenticator.class);
            var caller = Thread.currentThread();
            when(authenticator.token()).thenAnswer(invocation -> {
                assertSame(caller, Thread.currentThread());
                return "token";
            });
            when(authenticator.scheme()).thenReturn("Bearer");
            var response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(transport.send(any(), any())).thenAnswer(invocation -> {
                HttpRequest authenticated = invocation.getArgument(0);
                assertEquals("Bearer token", authenticated.headers().firstValue("Authorization").orElseThrow());
                return response;
            });

            assertSame(response, send(client(transport, authenticator), budgeted));
        }

        private void assertContextTokens(ThreadLocal<String> tokens) throws Exception {
            var caller = Thread.currentThread();
            for (var budgeted : List.of(false, true)) {
                var transport = mock(HttpClient.class);
                var authenticator = mock(Authenticator.class);
                var headers = new ArrayList<String>();
                when(authenticator.token()).thenAnswer(invocation -> {
                    assertSame(caller, Thread.currentThread());
                    return tokens.get();
                });
                when(authenticator.scheme()).thenReturn("Bearer");
                var response = mock(HttpResponse.class);
                when(response.statusCode()).thenReturn(200);
                when(transport.send(any(), any())).thenAnswer(invocation -> {
                    HttpRequest authenticated = invocation.getArgument(0);
                    headers.add(authenticated.headers().firstValue("Authorization").orElseThrow());
                    return response;
                });
                var client = client(transport, authenticator);

                try {
                    tokens.set("first-user-token");
                    assertSame(response, send(client, budgeted));
                    tokens.set("second-user-token");
                    assertSame(response, send(client, budgeted));

                    assertEquals(List.of("Bearer first-user-token", "Bearer second-user-token"), headers);
                } finally {
                    tokens.remove();
                }
            }
        }

        private HttpResponse<String> send(SyncHttpClient client, boolean budgeted) throws Exception {
            return budgeted ? client.send(request(), BodyHandlers.ofString(), Duration.ofSeconds(5))
                : client.send(request(), BodyHandlers.ofString());
        }

        private SyncHttpClient client(HttpClient transport, Authenticator authenticator) {
            return new SyncHttpClient(transport, List.of(new AuthenticationInterceptor(authenticator)), () -> 0L, nanos -> {});
        }

        private HttpRequest request() {
            return HttpRequest.newBuilder(URI.create("https://service.example.com"))
                .timeout(Duration.ofSeconds(30)).GET().build();
        }
    }
}
