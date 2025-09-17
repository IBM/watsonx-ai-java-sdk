/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelResponse.Pagination;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

public class FoundationModelServiceTest extends AbstractWatsonxTest {

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
    void test_get_models_with_only_filter_parameter() throws Exception {

        String EXPECTED = new String(ClassLoader.getSystemResourceAsStream("foundation_model_response.json").readAllBytes());

        var queryParameters =
            """
                version=%s\
                &filters=modelid_test""".formatted(API_VERSION);

        wireMock.stubFor(get("%s/foundation_model_specs?%s".formatted(ML_API_PATH, queryParameters))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(jsonResponse(EXPECTED, 200))
        );

        var service = FoundationModelService.builder()
            .url("http://localhost:%d".formatted(wireMock.getPort()))
            .build();

        var result = service.getModels(Filter.of(modelId("test")));
        var foundationModel = result.resources().get(0);
        assertEquals("core42/jais-13b-chat", foundationModel.modelId());
        assertEquals(2048, foundationModel.maxSequenceLength());
        assertEquals(2048, foundationModel.maxOutputTokens());
    }

    @Test
    void test_get_models_with_parameters() throws Exception {

        String EXPECTED = new String(ClassLoader.getSystemResourceAsStream("foundation_model_response.json").readAllBytes());

        var queryParameters =
            """
                version=%s\
                &start=100\
                &limit=10\
                &filters=modelid_test""".formatted(API_VERSION);

        wireMock.stubFor(get("%s/foundation_model_specs?%s".formatted(ML_API_PATH, queryParameters))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .willReturn(jsonResponse(EXPECTED, 200))
        );

        var service = FoundationModelService.builder()
            .url("http://localhost:%d".formatted(wireMock.getPort()))
            .techPreview(true)
            .logRequests(true)
            .logResponses(true)
            .timeout(Duration.ofSeconds(1))
            .build();

        var parameters = FoundationModelParameters.builder()
            .start(100)
            .limit(10)
            .filter(Filter.of(modelId("test")))
            .techPreview(false)
            .transactionId("my-transaction-id")
            .build();

        var result = service.getModels(parameters);
        assertEquals(100, result.limit());
        assertEquals(new Pagination("https://eu-de.ml.cloud.ibm.com/ml/v1/foundation_model_specs?version=%s".formatted(API_VERSION)), result.first());
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
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .willReturn(jsonResponse(EXPECTED, 200))
        );

        var service = FoundationModelService.builder()
            .url("http://localhost:%d".formatted(wireMock.getPort()))
            .build();

        var parameters = FoundationModelParameters.builder()
            .start(100)
            .limit(12)
            .transactionId("my-transaction-id")
            .build();

        var result = service.getTasks(parameters);
        assertEquals(100, result.limit());
        assertEquals(new Pagination("https://eu-de.ml.cloud.ibm.com/ml/v1/foundation_model_tasks?version=%s".formatted(API_VERSION)), result.first());
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

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(EXPECTED);

        withWatsonxServiceMock(() -> {

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var service = FoundationModelService.builder()
                .url(CloudRegion.DALLAS)
                .version("2025-12-12")

                .build();

            service.getTasks();
            var uri = mockHttpRequest.getValue().uri();
            assertEquals(URI.create("https://us-south.ml.cloud.ibm.com/ml/v1/foundation_model_tasks?version=2025-12-12"), uri);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_exceptions() throws Exception {

        withWatsonxServiceMock(() -> {
            try {
                when(mockHttpClient.send(any(), any(BodyHandler.class)))
                    .thenThrow(IOException.class);


                var service = FoundationModelService.builder()
                    .url(CloudRegion.DALLAS)
                    .build();

                assertThrows(RuntimeException.class, () -> service.getModels());
                assertThrows(RuntimeException.class, () -> service.getTasks());
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
