/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.embedding;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.BodyHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingParameters.EncodingFormat;

@SuppressWarnings("unchecked")
public class ModelGatewayEmbeddingServiceTest extends AbstractWatsonxTest {

    private static final String MODEL_ID = "text-embedding-3-small";
    private static final List<Float> VECTOR = List.of(0.0023064255f, -0.009327292f, -0.0028842222f);
    // Base64 encoding of VECTOR as little-endian float32 values, the layout used by the "base64" encoding format.
    private static final String BASE64_VECTOR = "ZicXO4DRGLw4BT27";

    private static final String SIMPLE_RESPONSE = """
        {
            "object": "list",
            "model": "text-embedding-3-small",
            "data": [
                {
                    "object": "embedding",
                    "index": 0,
                    "embedding": [0.0023064255, -0.009327292, -0.0028842222]
                }
            ],
            "usage": {
                "prompt_tokens": 5,
                "total_tokens": 5
            }
        }""";

    @BeforeEach
    void beforeEach() {
        when(mockAuthenticator.token()).thenReturn("my-super-token");
    }

    private void stubHttpResponse(String body) {
        try {
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));
            when(mockHttpResponse.body()).thenReturn(body);
            when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
                .thenReturn(mockHttpResponse);
        } catch (Exception e) {
            fail(e);
        }
    }

    private ModelGatewayEmbeddingService buildService() {
        return ModelGatewayEmbeddingService.builder()
            .authenticator(mockAuthenticator)
            .modelId(MODEL_ID)
            .baseUrl("http://localhost")
            .build();
    }

    @Test
    void should_return_embeddings_for_single_input() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var response = buildService().embed("Hello, world!");
            assertNotNull(response);
            assertEquals("list", response.object());
            assertEquals(MODEL_ID, response.model());
            assertNotNull(response.data());
            assertEquals(1, response.data().size());
            assertEquals(0, response.data().get(0).index());
            assertEquals("embedding", response.data().get(0).object());
            assertEquals(VECTOR, response.data().get(0).embedding());
            assertNull(response.data().get(0).base64());
            assertNotNull(response.usage());
            assertEquals(5, response.usage().promptTokens());
            assertEquals(5, response.usage().totalTokens());
        });
    }

    @Test
    void should_send_correct_request_body_for_varargs_inputs() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            buildService().embed("Hello", "World");
            JSONAssert.assertEquals(
                """
                    { "model": "text-embedding-3-small", "input": ["Hello", "World"] }""",
                bodyPublisherToString(mockHttpRequest), false);
        });
    }

    @Test
    void should_send_correct_request_body_for_list_inputs() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            buildService().embed(List.of("A", "B", "C"));
            JSONAssert.assertEquals(
                """
                    { "model": "text-embedding-3-small", "input": ["A", "B", "C"] }""",
                bodyPublisherToString(mockHttpRequest), false);
        });
    }

    @Test
    void should_forward_parameters_when_provided() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var params = ModelGatewayEmbeddingParameters.builder()
                .dimensions(512)
                .encodingFormat(EncodingFormat.FLOAT)
                .user("user-99")
                .build();

            buildService().embed(List.of("Hello"), params);

            JSONAssert.assertEquals(
                """
                    {
                        "model": "text-embedding-3-small",
                        "input": ["Hello"],
                        "dimensions": 512,
                        "encoding_format": "float",
                        "user": "user-99"
                    }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_not_forward_null_parameters() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            buildService().embed(List.of("Hello"), null);
            JSONAssert.assertEquals(
                """
                    { "model": "text-embedding-3-small", "input": ["Hello"] }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_send_correct_request_body_with_all_fields() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var request = ModelGatewayEmbeddingRequest.builder()
                .input("Hello")
                .parameters(
                    ModelGatewayEmbeddingParameters.builder()
                        .dimensions(512)
                        .encodingFormat("float")
                        .user("user-123")
                        .build()
                ).build();

            buildService().embed(request);

            JSONAssert.assertEquals(
                """
                    {
                        "model": "text-embedding-3-small",
                        "input": ["Hello"],
                        "dimensions": 512,
                        "encoding_format": "float",
                        "user": "user-123"
                    }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_send_url_with_version() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var service = ModelGatewayEmbeddingService.builder()
                .authenticator(mockAuthenticator)
                .modelId(MODEL_ID)
                .baseUrl("http://my-gateway.com")
                .version("2025-01-01")
                .build();

            service.embed("hello");

            assertEquals(
                "http://my-gateway.com/ml/gateway/v1/embeddings?version=2025-01-01",
                mockHttpRequest.getValue().uri().toString());
        });
    }

    @Test
    void should_throw_when_modelId_is_missing() {
        withWatsonxServiceMock(() -> assertThrows(NullPointerException.class, () -> ModelGatewayEmbeddingService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl("http://localhost")
            .build()));
    }

    @Test
    void should_throw_when_varargs_input_is_null() {
        withWatsonxServiceMock(() -> {
            var service = buildService();
            assertThrows(NullPointerException.class, () -> service.embed((String[]) null));
        });
    }

    @Test
    void should_throw_when_list_input_is_null() {
        withWatsonxServiceMock(() -> {
            var service = buildService();
            assertThrows(NullPointerException.class, () -> service.embed((List<String>) null));
        });
    }

    @Test
    void should_throw_when_request_input_is_null() {
        withWatsonxServiceMock(() -> {
            var service = buildService();
            var request = ModelGatewayEmbeddingRequest.builder().build(); // no input set
            assertThrows(NullPointerException.class, () -> service.embed(request));
        });
    }

    @Test
    void should_throw_when_request_itself_is_null() {
        withWatsonxServiceMock(() -> {
            var service = buildService();
            assertThrows(NullPointerException.class, () -> service.embed((ModelGatewayEmbeddingRequest) null));
        });
    }

    @Test
    void should_throw_when_no_input_is_provided() {
        withWatsonxServiceMock(() -> {
            var service = buildService();
            var ex = assertThrows(IllegalArgumentException.class, () -> service.embed());
            assertEquals("At least one input must be provided", ex.getMessage());
        });
    }

    @Test
    void should_throw_when_input_list_is_empty() {
        withWatsonxServiceMock(() -> {
            var service = buildService();
            assertThrows(IllegalArgumentException.class, () -> service.embed(List.of()));
        });
    }

    @Test
    void should_serialize_encoding_format_enum_to_correct_string() {
        assertEquals("float", EncodingFormat.FLOAT.value());
        assertEquals("base64", EncodingFormat.BASE64.value());
    }

    @Test
    void should_use_encoding_format_enum_in_builder() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var request = ModelGatewayEmbeddingRequest.builder()
                .input("Hello")
                .parameters(
                    ModelGatewayEmbeddingParameters.builder()
                        .encodingFormat(EncodingFormat.BASE64)
                        .build()
                ).build();

            buildService().embed(request);

            JSONAssert.assertEquals(
                """
                    { "model": "text-embedding-3-small", "input": ["Hello"], "encoding_format": "base64" }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_handle_encoding_format_enum_as_null() {
        var parameters = ModelGatewayEmbeddingParameters.builder()
            .encodingFormat((EncodingFormat) null)
            .build();
        assertEquals(null, parameters.encodingFormat());
    }

    @Test
    void should_handle_input_list_as_null_in_builder() {
        var request = ModelGatewayEmbeddingRequest.builder()
            .input((List<String>) null)
            .build();
        assertEquals(null, request.input());
    }

    @Test
    void should_handle_input_set_then_overridden_with_list() {
        var request = ModelGatewayEmbeddingRequest.builder()
            .input("first")
            .input(List.of("second", "third"))
            .build();
        assertEquals(List.of("second", "third"), request.input());
    }

    @Test
    void should_handle_input_varargs_as_null_in_builder() {
        var request = ModelGatewayEmbeddingRequest.builder()
            .input((String[]) null)
            .build();
        assertEquals(null, request.input());
    }

    @Test
    void should_expose_toString_of_request_and_parameters() {
        var parameters = ModelGatewayEmbeddingParameters.builder()
            .dimensions(512)
            .encodingFormat(EncodingFormat.FLOAT)
            .user("user-1")
            .build();

        var request = ModelGatewayEmbeddingRequest.builder()
            .input("Hello")
            .parameters(parameters)
            .build();

        assertEquals(
            "ModelGatewayEmbeddingParameters [dimensions=512, encodingFormat=float, user=user-1]",
            parameters.toString());
        assertEquals(
            "ModelGatewayEmbeddingRequest [input=[Hello], parameters=%s]".formatted(parameters),
            request.toString());
    }

    @Test
    void should_expose_toString_of_response_types() {
        var embedding = new ModelGatewayEmbeddingResponse.Embedding("embedding", 0, List.of(0.1f, 0.2f), null);
        assertEquals("Embedding[object=embedding, index=0, embedding=[0.1, 0.2], base64=null]", embedding.toString());

        var response = new ModelGatewayEmbeddingResponse(
            "list", MODEL_ID, List.of(embedding), new ModelGatewayEmbeddingResponse.Usage(5, 5));
        assertTrue(response.toString().contains("Embedding[object=embedding, index=0, embedding=[0.1, 0.2], base64=null]"));
        assertTrue(response.toString().contains("Usage[promptTokens=5, totalTokens=5]"));
    }

    @Test
    void should_normalize_the_raw_embedding_value() {
        var floats = ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, List.of(0.1, 0.2));
        assertEquals(List.of(0.1f, 0.2f), floats.embedding());
        assertNull(floats.base64());

        // Integral JSON numbers are bound as Integer, not Double.
        var integers = ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, List.of(0, 1, -1));
        assertEquals(List.of(0.0f, 1.0f, -1.0f), integers.embedding());

        var empty = ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, List.of());
        assertEquals(List.of(), empty.embedding());

        var base64 = ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, BASE64_VECTOR);
        assertEquals(VECTOR, base64.embedding());
        assertEquals(BASE64_VECTOR, base64.base64());

        var missing = ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, null);
        assertNull(missing.embedding());
        assertNull(missing.base64());
    }

    @Test
    void should_decode_base64_to_the_same_vector_as_the_float_format() {
        var fromFloat = ModelGatewayEmbeddingResponse.Embedding.of(
            "embedding", 0, List.of(0.0023064255, -0.009327292, -0.0028842222));
        var fromBase64 = ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, BASE64_VECTOR);
        assertEquals(VECTOR, fromFloat.embedding());
        assertEquals(fromFloat.embedding(), fromBase64.embedding());
    }

    @Test
    void should_return_an_unmodifiable_vector() {
        var embedding = ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, List.of(0.1));
        var vector = embedding.embedding();
        assertThrows(UnsupportedOperationException.class, () -> vector.add(0.2f));

        var decoded = ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, BASE64_VECTOR).embedding();
        assertThrows(UnsupportedOperationException.class, () -> decoded.add(0.2f));

        var constructed = new ModelGatewayEmbeddingResponse.Embedding("embedding", 0, new ArrayList<>(VECTOR), null).embedding();
        assertThrows(UnsupportedOperationException.class, () -> constructed.add(0.2f));
    }

    @Test
    void should_not_expose_the_vector_passed_to_the_constructor() {
        var mutable = new ArrayList<>(VECTOR);
        var embedding = new ModelGatewayEmbeddingResponse.Embedding("embedding", 0, mutable, null);
        mutable.add(99.9f);
        assertEquals(VECTOR, embedding.embedding());
    }

    @Test
    void should_throw_when_the_raw_embedding_value_is_invalid() {
        var unsupported = assertThrows(IllegalArgumentException.class,
            () -> ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, 42));
        assertEquals("Unsupported embedding value of type java.lang.Integer", unsupported.getMessage());

        var truncated = assertThrows(IllegalArgumentException.class,
            () -> ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, "SjhCQVFFQkFRRUJBUUU9"));
        assertEquals("Invalid base64 embedding: 15 bytes is not a multiple of 4", truncated.getMessage());

        var notANumber = assertThrows(IllegalArgumentException.class,
            () -> ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, List.of("nope")));
        assertEquals("Invalid embedding vector: expected a number but found nope", notANumber.getMessage());

        assertThrows(IllegalArgumentException.class,
            () -> ModelGatewayEmbeddingResponse.Embedding.of("embedding", 0, "not base64!"));
    }

    @Test
    void should_consider_responses_parsed_from_the_same_json_equal() {
        var first = Json.fromJson(SIMPLE_RESPONSE, ModelGatewayEmbeddingResponse.class);
        var second = Json.fromJson(SIMPLE_RESPONSE, ModelGatewayEmbeddingResponse.class);
        assertEquals(first.data(), second.data());
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void should_serialize_the_embedding_in_the_shape_the_gateway_sent_it() {
        var floatFormat = Json.fromJson(SIMPLE_RESPONSE, ModelGatewayEmbeddingResponse.class);
        assertEquals(VECTOR, floatFormat.data().get(0).rawEmbedding());

        var base64Format = Json.fromJson(SIMPLE_RESPONSE.replace(
            "[0.0023064255, -0.009327292, -0.0028842222]", "\"%s\"".formatted(BASE64_VECTOR)),
            ModelGatewayEmbeddingResponse.class);
        assertEquals(BASE64_VECTOR, base64Format.data().get(0).rawEmbedding());

        // Reserializing must not turn one encoding format into the other, so both survive a round trip.
        assertEquals(floatFormat, Json.fromJson(Json.toJson(floatFormat), ModelGatewayEmbeddingResponse.class));
        assertEquals(base64Format, Json.fromJson(Json.toJson(base64Format), ModelGatewayEmbeddingResponse.class));
        assertFalse(Json.toJson(base64Format).contains("\"base64\""));
    }

    @Test
    void should_forward_dimensions_only() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var params = ModelGatewayEmbeddingParameters.builder()
                .dimensions(256)
                .build();

            buildService().embed(List.of("Hello"), params);

            JSONAssert.assertEquals(
                """
                    { "model": "text-embedding-3-small", "input": ["Hello"], "dimensions": 256 }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_forward_user_only() throws Exception {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var params = ModelGatewayEmbeddingParameters.builder()
                .user("user-42")
                .build();

            buildService().embed(List.of("Hello"), params);

            JSONAssert.assertEquals(
                """
                    { "model": "text-embedding-3-small", "input": ["Hello"], "user": "user-42" }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_round_trip_float_embeddings_over_http() {

        wireMock.stubFor(post("/ml/gateway/v1/embeddings?version=%s".formatted(API_VERSION))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(SIMPLE_RESPONSE)));

        var response = buildWireMockService().embed("Hello, world!");

        assertEquals("list", response.object());
        assertEquals(MODEL_ID, response.model());
        assertEquals(1, response.data().size());
        assertEquals("embedding", response.data().get(0).object());
        assertEquals(0, response.data().get(0).index());
        assertEquals(VECTOR, response.data().get(0).embedding());
        assertNull(response.data().get(0).base64());
        assertEquals(5, response.usage().promptTokens());
        assertEquals(5, response.usage().totalTokens());
    }

    @Test
    void should_round_trip_base64_embeddings_over_http() {

        wireMock.stubFor(post("/ml/gateway/v1/embeddings?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "object": "list",
                        "model": "text-embedding-3-small",
                        "data": [
                            {
                                "object": "embedding",
                                "index": 0,
                                "embedding": "%s"
                            }
                        ],
                        "usage": {
                            "prompt_tokens": 5,
                            "total_tokens": 5
                        }
                    }""".formatted(BASE64_VECTOR))));

        var request = ModelGatewayEmbeddingRequest.builder()
            .input("Hello, world!")
            .parameters(
                ModelGatewayEmbeddingParameters.builder()
                    .encodingFormat(EncodingFormat.BASE64)
                    .build()
            ).build();

        var response = buildWireMockService().embed(request);

        assertEquals(BASE64_VECTOR, response.data().get(0).base64());
        assertEquals(VECTOR, response.data().get(0).embedding());
    }

    @Test
    void should_throw_watsonx_exception_on_gateway_client_error() {

        wireMock.stubFor(post("/ml/gateway/v1/embeddings?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": {
                            "code": "model_not_found",
                            "message": "The model `text-embedding-3-small` does not exist.",
                            "request_id": "req-12345"
                        }
                    }""")));

        var service = buildWireMockService();
        var ex = assertThrows(WatsonxException.class, () -> service.embed("Hello"));

        assertEquals(404, ex.statusCode());
        assertNotNull(ex.details().orElse(null));
        assertEquals("req-12345", ex.details().orElse(null).trace());
        assertEquals(1, ex.details().orElse(null).errors().size());
        assertEquals("model_not_found", ex.details().orElse(null).errors().get(0).code());
        assertEquals(
            "The model `text-embedding-3-small` does not exist.",
            ex.details().orElse(null).errors().get(0).message());
    }

    @Test
    void should_throw_watsonx_exception_on_gateway_server_error() {

        final String ERROR_BODY = """
            {
                "error": {
                    "code": "internal_server_error",
                    "message": "Upstream provider failed.",
                    "request_id": "req-500"
                }
            }""";

        wireMock.stubFor(post("/ml/gateway/v1/embeddings?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody(ERROR_BODY)));

        var service = buildWireMockService();
        var ex = assertThrows(WatsonxException.class, () -> service.embed("Hello"));

        assertEquals(500, ex.statusCode());
        JSONAssert.assertEquals(ERROR_BODY, ex.getMessage(), true);
    }

    @Test
    void should_wrap_io_exception_in_runtime_exception() throws Exception {

        when(mockSecureHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));

        withWatsonxServiceMock(() -> {
            var service = buildService();
            var ex = assertThrows(RuntimeException.class, () -> service.embed("Hello"));
            assertInstanceOf(IOException.class, ex.getCause());
        });
    }

    private ModelGatewayEmbeddingService buildWireMockService() {
        return ModelGatewayEmbeddingService.builder()
            .authenticator(mockAuthenticator)
            .modelId(MODEL_ID)
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();
    }
}
