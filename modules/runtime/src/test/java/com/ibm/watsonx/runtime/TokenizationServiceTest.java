/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.core.Json;
import com.ibm.watsonx.core.auth.AuthenticationProvider;
import com.ibm.watsonx.runtime.tokenization.TokenizationParameters;
import com.ibm.watsonx.runtime.tokenization.TokenizationService;
import com.ibm.watsonx.runtime.utils.Utils;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class TokenizationServiceTest {

  @Mock
  HttpClient mockHttpClient;

  @Mock
  HttpRequest mockHttpRequest;

  @Mock
  HttpResponse<String> mockHttpResponse;

  @Mock
  AuthenticationProvider mockAuthenticationProvider;

  @Captor
  ArgumentCaptor<HttpRequest> httpRequestCaptor;

  @BeforeEach
  void setUp() {
    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
  }

  @Test
  void test_tokenization() throws Exception {

    final String REQUEST = """
      {
        "model_id": "google/flan-ul2",
        "input": "Write a tagline for an alumni association: Together we",
        "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f"
      }""";

    final String RESPONSE = """
      {
        "model_id": "google/flan-ul2",
        "result": {
          "token_count": 11
        }
      }""";

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);
    when(mockHttpClient.send(httpRequestCaptor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);;

    var tokenizationService = TokenizationService.builder()
      .url(CloudRegion.LONDON)
      .httpClient(mockHttpClient)
      .authenticationProvider(mockAuthenticationProvider)
      .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
      .modelId("google/flan-ul2")
      .build();

    var response = tokenizationService.tokenize("Write a tagline for an alumni association: Together we");

    JSONAssert.assertEquals(REQUEST, Utils.bodyPublisherToString(httpRequestCaptor), true);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
  }

  @Test
  void test_tokenization_with_parameters() throws Exception {

    final String REQUEST = """
      {
        "model_id": "google/flan-ul2",
        "input": "Write a tagline for an alumni association: Together we",
        "parameters": {
          "return_tokens": true
        },
        "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f"
      }""";

    final String RESPONSE = """
      {
        "model_id": "google/flan-ul2",
        "result": {
          "token_count": 11,
          "tokens": [
            "Write",
            "a",
            "tag",
            "line",
            "for",
            "an",
            "alumni",
            "associ",
            "ation:",
            "Together",
            "we"
          ]
        }
      }""";

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);
    when(mockHttpClient.send(httpRequestCaptor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);;

    var tokenizationService = TokenizationService.builder()
      .url(CloudRegion.LONDON)
      .httpClient(mockHttpClient)
      .authenticationProvider(mockAuthenticationProvider)
      .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
      .modelId("google/flan-ul2")
      .build();

    var parameters = TokenizationParameters.builder()
      .returnTokens(true)
      .build();

    assertEquals(true, parameters.getReturnTokens());

    var response = tokenizationService.tokenize(
      "Write a tagline for an alumni association: Together we",
      parameters
    );

    JSONAssert.assertEquals(REQUEST, Utils.bodyPublisherToString(httpRequestCaptor), true);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
  }

  @Test
  void test_tokenization_override_default_parameters() throws Exception {

    final String REQUEST = """
      {
        "model_id": "my-model-id",
        "input": "Write a tagline for an alumni association: Together we",
        "project_id": "my-project-id",
        "space_id": "my-space-id"
      }""";

    final String RESPONSE = """
      {
        "model_id": "my-model-id",
        "result": {
          "token_count": 11
        }
      }""";

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(RESPONSE);
    when(mockHttpClient.send(httpRequestCaptor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);;

    var tokenizationService = TokenizationService.builder()
      .url(CloudRegion.LONDON)
      .httpClient(mockHttpClient)
      .authenticationProvider(mockAuthenticationProvider)
      .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
      .modelId("google/flan-ul2")
      .build();

    var parameters = TokenizationParameters.builder()
      .modelId("my-model-id")
      .projectId("my-project-id")
      .spaceId("my-space-id")
      .build();

    var response = tokenizationService.tokenize(
      "Write a tagline for an alumni association: Together we",
      parameters
    );

    JSONAssert.assertEquals(REQUEST, Utils.bodyPublisherToString(httpRequestCaptor), true);
    JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
  }

  @Test
  void test_tokenization_exeception() throws Exception {

    when(mockHttpClient.send(any(), any())).thenThrow(new IOException("error"));

    var tokenizationService = TokenizationService.builder()
      .url(CloudRegion.LONDON)
      .httpClient(mockHttpClient)
      .authenticationProvider(mockAuthenticationProvider)
      .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
      .modelId("google/flan-ul2")
      .build();

    assertThrows(RuntimeException.class, () -> tokenizationService.tokenize("test"));
  }
}
