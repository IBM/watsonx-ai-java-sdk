/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.ibm.watsonx.ai.WatsonxService.API_VERSION;
import static com.ibm.watsonx.ai.WatsonxService.ML_API_PATH;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelResponse.Pagination;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

public class FoundationModelServiceTest {

  @RegisterExtension
  WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
    .build();

  @Test
  void test_get_models_by_id() throws Exception {

    var RESPONSE =
      """
        {
              "total_count": {total_count},
              "limit": 100,
              "first": {
                  "href": "https://eu-de.ml.cloud.ibm.com/ml/v1/foundation_model_specs?version=2025-04-23&filters=modelid_meta-llama%2Fllama-3-3-70b-instruct"
              },
              "resources": [
                  {}
              ]
        }""";

    var queryParameters =
      """
        version=%s\
        &filters=modelid_test""".formatted(API_VERSION);

    wireMock.stubFor(get("%s/foundation_model_specs?%s".formatted(ML_API_PATH, queryParameters))
      .inScenario("getModels")
      .whenScenarioStateIs(Scenario.STARTED)
      .willSetStateTo("secondCall")
      .withHeader("Accept", equalTo("application/json"))
      .willReturn(jsonResponse(RESPONSE.replace("{total_count}", "1"), 200))
    );

    wireMock.stubFor(get("%s/foundation_model_specs?%s".formatted(ML_API_PATH, queryParameters))
      .inScenario("getModels")
      .whenScenarioStateIs("secondCall")
      .withHeader("Accept", equalTo("application/json"))
      .willReturn(jsonResponse(RESPONSE.replace("{total_count}", "0"), 200))
    );

    var service = FoundationModelService.builder()
      .url("http://localhost:%d".formatted(wireMock.getPort()))
      .build();

    assertNotNull(service.getModelDetails("test"));
    assertFalse(service.getModelDetails("test").isPresent());
  }

  @Test
  void test_get_models_without_parameters() throws Exception {

    String EXPECTED = new String(ClassLoader.getSystemResourceAsStream("foundation_model_response.json").readAllBytes());

    var queryParameters = "version=%s".formatted(API_VERSION);
    wireMock.stubFor(get("%s/foundation_model_specs?%s".formatted(ML_API_PATH, queryParameters))
      .withHeader("Accept", equalTo("application/json"))
      .willReturn(jsonResponse(EXPECTED, 200))
    );

    var service = FoundationModelService.builder()
      .url("http://localhost:%d".formatted(wireMock.getPort()))
      .build();

    JSONAssert.assertEquals(EXPECTED, toJson(service.getModels()), true);
  }

  @Test
  void test_get_models_with_parameters() throws Exception {

    String EXPECTED = new String(ClassLoader.getSystemResourceAsStream("foundation_model_response.json").readAllBytes());

    var queryParameters =
      """
        version=%s\
        &start=100\
        &limit=10\
        &tech_preview=true\
        &filters=modelid_test""".formatted(API_VERSION);

    wireMock.stubFor(get("%s/foundation_model_specs?%s".formatted(ML_API_PATH, queryParameters))
      .withHeader("Accept", equalTo("application/json"))
      .willReturn(jsonResponse(EXPECTED, 200))
    );

    var service = FoundationModelService.builder()
      .url("http://localhost:%d".formatted(wireMock.getPort()))
      .techPreview(true)
      .logRequests(true)
      .logResponses(true)
      .timeout(Duration.ofSeconds(1))
      .build();

    var result = service.getModels(100, 10, Filter.of(modelId("test")));
    assertEquals(100, result.limit());
    assertEquals(new Pagination("https://eu-de.ml.cloud.ibm.com/ml/v1/foundation_model_specs?version=2025-04-23"), result.first());
    assertEquals(35, result.totalCount());
    assertEquals(35, result.resources().size());

    var foundationModel = result.resources().get(0);
    assertEquals("core42/jais-13b-chat", foundationModel.modelId());
    assertEquals("jais-13b-chat", foundationModel.label());
    assertEquals("Core42", foundationModel.provider());
    assertEquals("Hugging Face", foundationModel.source());
    assertEquals("class_2", foundationModel.inputTier());
    assertEquals("class_2", foundationModel.outputTier());
    assertEquals("13b", foundationModel.numberParams());
    assertEquals(1, foundationModel.minShotSize());
    assertTrue(foundationModel.shortDescription().startsWith("Jais-13b-chat is Jais-13b"));
    assertTrue(foundationModel.longDescription().contains("SwiGLU"));
    assertEquals("https://www.apache.org/licenses/LICENSE-2.0", foundationModel.termsUrl());
    assertEquals(1, foundationModel.functions().size());
    assertEquals("text_generation", foundationModel.functions().get(0).id());
    assertEquals(7, foundationModel.taskIds().size());
    assertTrue(foundationModel.taskIds().contains("summarization"));
    assertEquals(7, foundationModel.tasks().size());
    assertEquals(2048, foundationModel.modelLimits().maxSequenceLength());
    assertEquals(2048, foundationModel.modelLimits().maxOutputTokens());
    assertEquals("5m0s", foundationModel.limits().get("lite").callTime());
    assertEquals("10m0s", foundationModel.limits().get("v2-professional").callTime());
    assertEquals(2048, foundationModel.limits().get("v2-professional").maxOutputTokens());
    assertEquals(1, foundationModel.lifecycle().size());
    assertEquals("available", foundationModel.lifecycle().get(0).id());
    assertEquals("2024-04-11", foundationModel.lifecycle().get(0).startDate());
  }

  @Test
  void test_get_tasks_without_parameters() throws Exception {

    String EXPECTED = new String(ClassLoader.getSystemResourceAsStream("foundation_model_tasks_response.json").readAllBytes());

    var queryParameters = "version=%s".formatted(API_VERSION);
    wireMock.stubFor(get("%s/foundation_model_tasks?%s".formatted(ML_API_PATH, queryParameters))
      .willReturn(jsonResponse(EXPECTED, 200))
    );

    var service = FoundationModelService.builder()
      .url("http://localhost:%d".formatted(wireMock.getPort()))
      .build();

    JSONAssert.assertEquals(EXPECTED, toJson(service.getTasks()), true);
  }

  @Test
  void test_get_tasks_with_parameters() throws Exception {

    String EXPECTED = new String(ClassLoader.getSystemResourceAsStream("foundation_model_tasks_response.json").readAllBytes());

    var queryParameters =
      """
        version=%s\
        &start=100\
        &limit=12""".formatted(API_VERSION);

    wireMock.stubFor(get("%s/foundation_model_tasks?%s".formatted(ML_API_PATH, queryParameters))
      .willReturn(jsonResponse(EXPECTED, 200))
    );

    var service = FoundationModelService.builder()
      .url("http://localhost:%d".formatted(wireMock.getPort()))
      .build();

    var result = service.getTasks(100, 12);
    assertEquals(100, result.limit());
    assertEquals(new Pagination("https://eu-de.ml.cloud.ibm.com/ml/v1/foundation_model_tasks?version=2025-04-23"), result.first());
    assertEquals(12, result.totalCount());
    assertEquals(12, result.resources().size());

    var task = result.resources().get(0);
    assertEquals("question_answering", task.taskId());
    assertEquals("Question answering", task.label());
    assertEquals(1, task.rank());
    assertEquals(
      "Based on a set of documents or dynamic content, create a chatbot or a question-answering feature grounded on specific content. E.g. building a Q&A resource from a broad knowledge base, providing customer service assistance.",
      task.description());
  }

  @Test
  @SuppressWarnings("unchecked")
  void test_override_parameters() throws Exception {

    String EXPECTED = new String(ClassLoader.getSystemResourceAsStream("foundation_model_tasks_response.json").readAllBytes());

    var mockHttpClient = mock(HttpClient.class);
    var mockHttpResponse = mock(HttpResponse.class);
    var captor = ArgumentCaptor.forClass(HttpRequest.class);

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(EXPECTED);
    when(mockHttpClient.send(captor.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

    var service = FoundationModelService.builder()
      .url(CloudRegion.DALLAS)
      .version("2025-12-12")
      .httpClient(mockHttpClient)
      .build();

    service.getTasks();
    var httpRequest = captor.getValue();
    assertEquals(URI.create("https://us-south.ml.cloud.ibm.com/ml/v1/foundation_model_tasks?version=2025-12-12"), httpRequest.uri());
  }

  @Test
  @SuppressWarnings("unchecked")
  void test_exceptions() throws Exception {

    var mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.send(any(), any(BodyHandler.class)))
      .thenThrow(IOException.class);

    var service = FoundationModelService.builder()
      .url(CloudRegion.DALLAS)
      .httpClient(mockHttpClient)
      .build();

    assertThrows(RuntimeException.class, () -> service.getModels());
    assertThrows(RuntimeException.class, () -> service.getTasks());
  }
}
