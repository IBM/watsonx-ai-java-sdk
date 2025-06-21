/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError.Error;
import com.ibm.watsonx.ai.embedding.EmbeddingParameters;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import com.ibm.watsonx.ai.utils.Utils;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmbeddingServiceTest {

  private final String MODEL_ID = "slate";
  private final String PROJECT_ID = "project_id";

  @Mock
  HttpClient mockHttpClient;

  @Mock
  HttpRequest mockHttpRequest;

  @Mock
  HttpResponse<String> mockHttpResponse;

  @Mock
  AuthenticationProvider authenticationProvider;

  @Captor
  ArgumentCaptor<HttpRequest> httpRequestCaptor;

  @BeforeEach
  void setUp() {
    when(authenticationProvider.getToken()).thenReturn("my-super-token");
  }

  @Test
  void test_embedding() throws Exception {

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
    when(mockHttpClient.send(httpRequestCaptor.capture(), any(BodyHandler.class)))
      .thenReturn(mockHttpResponse);

    var embeddingService = EmbeddingService.builder()
      .authenticationProvider(authenticationProvider)
      .httpClient(mockHttpClient)
      .modelId(MODEL_ID)
      .logRequests(true)
      .projectId(PROJECT_ID)
      .url(CloudRegion.DALLAS)
      .build();

    var response = embeddingService.embedding(
      "Youth craves thrills while adulthood cherishes wisdom.",
      "Youth seeks ambition while adulthood finds contentment.",
      "Dreams chased in youth while goals pursued in adulthood."
    );

    JSONAssert.assertEquals(REQUEST, Utils.bodyPublisherToString(httpRequestCaptor), true);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
  }

  @Test
  void test_embedding_parameters() throws Exception {

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
    when(mockHttpClient.send(httpRequestCaptor.capture(), any(BodyHandler.class)))
      .thenReturn(mockHttpResponse);

    var embeddingService = EmbeddingService.builder()
      .authenticationProvider(authenticationProvider)
      .httpClient(mockHttpClient)
      .modelId(MODEL_ID)
      .logRequests(true)
      .projectId(PROJECT_ID)
      .url(CloudRegion.DALLAS)
      .build();

    var parameters = EmbeddingParameters.builder()
      .modelId("override-model")
      .projectId("override-project-id")
      .spaceId("override-space-id")
      .truncateInputTokens(512)
      .inputText(true)
      .build();

    assertEquals(512, parameters.getTruncateInputTokens());
    assertEquals(true, parameters.getInputText());

    var inputs = List.of(
      "Youth craves thrills while adulthood cherishes wisdom.",
      "Youth seeks ambition while adulthood finds contentment.",
      "Dreams chased in youth while goals pursued in adulthood."
    );

    var response = embeddingService.embedding(inputs, parameters);
    JSONAssert.assertEquals(REQUEST, Utils.bodyPublisherToString(httpRequestCaptor), true);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
  }

  @Test
  void test_embedding_exception() throws Exception {

    var error = new WatsonxError(
      400, "error", List.of(new Error("X", "X", "X")));

    var json = Json.toJson(error);

    when(mockHttpResponse.statusCode()).thenReturn(400);
    when(mockHttpResponse.body()).thenReturn(json);
    when(mockHttpClient.send(httpRequestCaptor.capture(), any(BodyHandler.class)))
      .thenReturn(mockHttpResponse);

    var embeddingService = EmbeddingService.builder()
      .authenticationProvider(authenticationProvider)
      .httpClient(mockHttpClient)
      .modelId(MODEL_ID)
      .logRequests(true)
      .projectId(PROJECT_ID)
      .url(CloudRegion.DALLAS)
      .build();

    var ex = assertThrows(RuntimeException.class, () -> embeddingService.embedding("Hello"));
    JSONAssert.assertEquals(json, ex.getMessage(), true);
  }
}
