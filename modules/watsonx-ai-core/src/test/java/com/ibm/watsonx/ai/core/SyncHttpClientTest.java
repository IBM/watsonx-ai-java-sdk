/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
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
import com.ibm.watsonx.ai.core.http.SyncHttpClient;
import com.ibm.watsonx.ai.core.http.SyncHttpInterceptor;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class SyncHttpClientTest {

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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);

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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);

        var response = client.send(httpRequest, handler);

        assertEquals(httpResponse, response);

        InOrder inOrder = inOrder(interceptor1, interceptor2);
        inOrder.verify(interceptor1).intercept(any(), eq(handler), anyInt(), any());
        inOrder.verify(interceptor2).intercept(any(), eq(handler), anyInt(), any());
    }

    @Test
    void should_send_request_when_no_interceptors_are_configured() throws Exception {

        BodyHandler<String> handler = mock(BodyHandler.class);
        SyncHttpClient client = SyncHttpClient.builder()
            .httpClient(httpClient)
            .build();

        when(httpResponse.statusCode())
            .thenReturn(200);

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);

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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
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

        when(httpClient.send(any(), eq(handler))).thenReturn(httpResponse);
        assertThrows(WatsonxException.class, () -> client.send(httpRequest, handler));
    }
}
