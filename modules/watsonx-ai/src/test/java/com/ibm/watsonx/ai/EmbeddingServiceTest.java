/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.BodyHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError.Error;
import com.ibm.watsonx.ai.embedding.EmbeddingParameters;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import com.ibm.watsonx.ai.utils.HttpUtils;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmbeddingServiceTest extends AbstractWatsonxTest {

    private final String MODEL_ID = "slate";
    private final String PROJECT_ID = "project_id";

    @BeforeEach
    void setUp() {
        when(mockAuthenticator.token()).thenReturn("my-super-token");
    }

    @Test
    void should_return_embeddings_for_multiple_inputs() throws Exception {

        final String REQUEST = """
            {
                "inputs": [
                    "Youth craves thrills while adulthood cherishes wisdom.",
                    "Youth seeks ambition while adulthood finds contentment.",
                    "Dreams chased in youth while goals pursued in adulthood."
                ],
                "model_id": "%s",
                "project_id": "%s"
            }""".formatted(MODEL_ID, PROJECT_ID);

        final String RESPONSE = """
            {
              "model_id": "%s",
              "results": [
                {
                  "embedding": [
                    -0.006929283,
                    -0.005336422,
                    -0.024047505
                  ]
                }
              ],
              "created_at": "2024-02-21T17:32:28Z",
              "input_token_count": 10
            }""".formatted(MODEL_ID);

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var embeddingService = EmbeddingService.builder()
                .authenticator(mockAuthenticator)
                .modelId(MODEL_ID)
                .logRequests(true)
                .projectId(PROJECT_ID)
                .baseUrl(CloudRegion.DALLAS)
                .build();

            var response = embeddingService.embedding(
                "Youth craves thrills while adulthood cherishes wisdom.",
                "Youth seeks ambition while adulthood finds contentment.",
                "Dreams chased in youth while goals pursued in adulthood."
            );

            JSONAssert.assertEquals(REQUEST, HttpUtils.bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_return_embeddings_with_parameters() throws Exception {

        final String REQUEST = """
            {
                "inputs": [
                    "Youth craves thrills while adulthood cherishes wisdom.",
                    "Youth seeks ambition while adulthood finds contentment.",
                    "Dreams chased in youth while goals pursued in adulthood."
                ],
                "model_id": "override-model",
                "project_id": "override-project-id",
                "space_id": "override-space-id",
                "parameters" : {
                    "truncate_input_tokens" : 512,
                    "return_options": {
                        "input_text": true
                    }
                }
            }""";

        final String RESPONSE = """
            {
              "model_id": "override-model",
              "results": [
                {
                  "embedding": [
                    -0.006929283,
                    -0.005336422,
                    -0.024047505
                  ],
                  "input": "Youth craves thrills while adulthood cherishes wisdom."
                },
                {
                  "embedding": [
                    -0.006929283,
                    -0.005336422,
                    -0.024047505
                  ],
                  "input": "Youth seeks ambition while adulthood finds contentment."
                },
                {
                  "embedding": [
                    -0.006929283,
                    -0.005336422,
                    -0.024047505
                  ],
                  "input": "Dreams chased in youth while goals pursued in adulthood."
                }
              ],
              "created_at": "2024-02-21T17:32:28Z",
              "input_token_count": 10
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var embeddingService = EmbeddingService.builder()
                .authenticator(mockAuthenticator)
                .modelId(MODEL_ID)
                .logRequests(true)
                .projectId(PROJECT_ID)
                .baseUrl(CloudRegion.DALLAS)
                .build();

            var parameters = EmbeddingParameters.builder()
                .modelId("override-model")
                .projectId("override-project-id")
                .spaceId("override-space-id")
                .truncateInputTokens(512)
                .inputText(true)
                .transactionId("my-transaction-id")
                .build();

            assertEquals(512, parameters.truncateInputTokens());
            assertEquals(true, parameters.inputText());

            var inputs = List.of(
                "Youth craves thrills while adulthood cherishes wisdom.",
                "Youth seeks ambition while adulthood finds contentment.",
                "Dreams chased in youth while goals pursued in adulthood."
            );

            var response = embeddingService.embedding(inputs, parameters);
            JSONAssert.assertEquals(REQUEST, HttpUtils.bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }

    @Test
    void should_throw_runtime_exception_on_embedding_error() throws Exception {

        var error = new WatsonxError(
            400, "error", List.of(new Error("X", "X", "X")));

        var json = Json.toJson(error);

        when(mockHttpResponse.statusCode()).thenReturn(400);
        when(mockHttpResponse.body()).thenReturn(json);
        when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));
        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var embeddingService = EmbeddingService.builder()
                .authenticator(mockAuthenticator)
                .modelId(MODEL_ID)
                .logRequests(true)
                .projectId(PROJECT_ID)
                .baseUrl(CloudRegion.DALLAS)
                .build();

            var ex = assertThrows(RuntimeException.class, () -> embeddingService.embedding("Hello"));
            JSONAssert.assertEquals(json, ex.getMessage(), true);
        });
    }

    @Test
    void should_throw_runtime_exception_on_http_client_error() throws Exception {

        when(mockSecureHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));

        withWatsonxServiceMock(() -> {
            var embeddingService = EmbeddingService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .projectId("project-id")
                .modelId("model-id")
                .build();

            assertThrows(RuntimeException.class, () -> embeddingService.embedding("test"), "IOException");
        });
    }

    @Test
    void should_send_embedded_crypted_request() throws Exception {

        final String REQUEST = """
            {
                "inputs": ["Youth craves thrills while adulthood cherishes wisdom."],
                "model_id": "slate",
                "project_id": "project_id",
                "crypto": {
                    "key_ref": "key-ref"
                }
            }""";

        final String RESPONSE = """
            {
              "model_id": "slate",
              "results": [
                {
                  "embedding": [
                    -0.006929283,
                    -0.005336422,
                    -0.024047505
                  ]
                }
              ],
              "created_at": "2024-02-21T17:32:28Z",
              "input_token_count": 10
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var embeddingService = EmbeddingService.builder()
                .authenticator(mockAuthenticator)
                .modelId(MODEL_ID)
                .projectId(PROJECT_ID)
                .logRequests(true)
                .baseUrl(CloudRegion.DALLAS)
                .build();

            var inputs = List.of("Youth craves thrills while adulthood cherishes wisdom.");
            var parameters = EmbeddingParameters.builder().crypto("key-ref").build();

            embeddingService.embedding(inputs, parameters);
            JSONAssert.assertEquals(REQUEST, HttpUtils.bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_handle_parallel_requests_for_large_input_batches() throws Exception {

        final String RESPONSE_BATCH_1 = """
            {
              "model_id": "%s",
              "results": [
                {
                  "embedding": [-0.001, -0.002, -0.003]
                }
              ],
              "created_at": "2024-02-21T17:32:28Z",
              "input_token_count": 1000
            }""".formatted(MODEL_ID);

        final String RESPONSE_BATCH_2 = """
            {
              "model_id": "%s",
              "results": [
                {
                  "embedding": [-0.004, -0.005, -0.006]
                }
              ],
              "created_at": "2024-02-21T17:32:29Z",
              "input_token_count": 1000
            }""".formatted(MODEL_ID);

        final String RESPONSE_BATCH_3 = """
            {
              "model_id": "%s",
              "results": [
                {
                  "embedding": [-0.007, -0.008, -0.009]
                }
              ],
              "created_at": "2024-02-21T17:32:30Z",
              "input_token_count": 500
            }""".formatted(MODEL_ID);

        when(mockAuthenticator.token()).thenReturn("token");

        String BASE_URL = "http://localhost:%s".formatted(wireMock.getPort());
        String path = "/ml/v1/text/embeddings?version=%s".formatted(API_VERSION);

        wireMock.stubFor(post(path)
            .withRequestBody(containing("\"Input text 0\""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(RESPONSE_BATCH_1)));

        wireMock.stubFor(post(path)
            .withRequestBody(containing("\"Input text 1000\""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(RESPONSE_BATCH_2)));

        wireMock.stubFor(post(path)
            .withRequestBody(containing("\"Input text 2000\""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(RESPONSE_BATCH_3)));

        var embeddingService = EmbeddingService.builder()
            .authenticator(mockAuthenticator)
            .modelId(MODEL_ID)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        List<String> inputs = new ArrayList<>();
        for (int i = 0; i < 2500; i++)
            inputs.add("Input text " + i);

        var response = embeddingService.embedding(inputs);

        assertEquals(MODEL_ID, response.modelId());
        assertEquals(3, response.results().size());
        assertEquals(2500, response.inputTokenCount());
        assertNotNull(response.createdAt());

        assertEquals(3, response.results().get(0).embedding().size());
        assertEquals(-0.001f, response.results().get(0).embedding().get(0));
        assertEquals(-0.002f, response.results().get(0).embedding().get(1));
        assertEquals(-0.003f, response.results().get(0).embedding().get(2));

        assertEquals(3, response.results().get(1).embedding().size());
        assertEquals(-0.004f, response.results().get(1).embedding().get(0));
        assertEquals(-0.005f, response.results().get(1).embedding().get(1));
        assertEquals(-0.006f, response.results().get(1).embedding().get(2));

        assertEquals(3, response.results().get(2).embedding().size());
        assertEquals(-0.007f, response.results().get(2).embedding().get(0));
        assertEquals(-0.008f, response.results().get(2).embedding().get(1));
        assertEquals(-0.009f, response.results().get(2).embedding().get(2));
    }
}
