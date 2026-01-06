/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.ibm.watsonx.ai.utils.Utils.getFieldValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.net.http.HttpClient;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.provider.HttpClientProvider;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.rerank.RerankService;
import com.ibm.watsonx.ai.textgeneration.TextGenerationService;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationService;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;
import com.ibm.watsonx.ai.timeseries.TimeSeriesService;
import com.ibm.watsonx.ai.tokenization.TokenizationService;
import com.ibm.watsonx.ai.tool.ToolService;

public class CustomHttpClientTest {

    @Test
    void should_use_custom_http_client_for_chat_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        ChatService chatService = ChatService.builder()
            .baseUrl("https://localhost")
            .modelId("modelId")
            .apiKey("apiKey")
            .projectId("projectId")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(chatService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));

        Object asyncHttpClient = getFieldValue(restclient, "asyncHttpClient");
        assertEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(asyncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_chat_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                ChatService chatService = ChatService.builder()
                    .baseUrl("https://localhost")
                    .modelId("modelId")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(chatService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

                Object asyncHttpClient = getFieldValue(restclient, "asyncHttpClient");
                assertNotEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(asyncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_deployment_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        DeploymentService deploymentService = DeploymentService.builder()
            .baseUrl("https://localhost")
            .apiKey("apiKey")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(deploymentService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));

        Object asyncHttpClient = getFieldValue(restclient, "asyncHttpClient");
        assertEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(asyncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_deployment_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                DeploymentService deploymentService = DeploymentService.builder()
                    .baseUrl("https://localhost")
                    .apiKey("apiKey")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(deploymentService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

                Object asyncHttpClient = getFieldValue(restclient, "asyncHttpClient");
                assertNotEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(asyncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_detection_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        DetectionService detectionService = DetectionService.builder()
            .baseUrl("https://localhost")
            .apiKey("apiKey")
            .projectId("projectId")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(detectionService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_detection_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                DetectionService detectionService = DetectionService.builder()
                    .baseUrl("https://localhost")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(detectionService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_embedding_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        EmbeddingService embeddingService = EmbeddingService.builder()
            .baseUrl("https://localhost")
            .modelId("modelId")
            .apiKey("apiKey")
            .projectId("projectId")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(embeddingService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_embedding_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                EmbeddingService embeddingService = EmbeddingService.builder()
                    .baseUrl("https://localhost")
                    .modelId("modelId")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(embeddingService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_foundation_model_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        FoundationModelService foundationModelService = FoundationModelService.builder()
            .baseUrl("https://localhost")
            .apiKey("apiKey")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(foundationModelService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_foundation_model_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                FoundationModelService foundationModelService = FoundationModelService.builder()
                    .baseUrl("https://localhost")
                    .apiKey("apiKey")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(foundationModelService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_rerank_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        RerankService rerankService = RerankService.builder()
            .baseUrl("https://localhost")
            .modelId("modelId")
            .apiKey("apiKey")
            .projectId("projectId")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(rerankService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_rerank_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                RerankService rerankService = RerankService.builder()
                    .baseUrl("https://localhost")
                    .modelId("modelId")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(rerankService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_text_generation_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        TextGenerationService textGenerationService = TextGenerationService.builder()
            .baseUrl("https://localhost")
            .modelId("modelId")
            .apiKey("apiKey")
            .projectId("projectId")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(textGenerationService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));

        Object asyncHttpClient = getFieldValue(restclient, "asyncHttpClient");
        assertEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(asyncHttpClient, "delegate"));

    }

    @Test
    void should_use_default_http_client_for_text_generation_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                TextGenerationService textGenerationService = TextGenerationService.builder()
                    .baseUrl("https://localhost")
                    .modelId("modelId")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(textGenerationService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

                Object asyncHttpClient = getFieldValue(restclient, "asyncHttpClient");
                assertNotEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(asyncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_text_classification_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        TextClassificationService textClassificationService = TextClassificationService.builder()
            .baseUrl("https://localhost")
            .apiKey("apiKey")
            .projectId("projectId")
            .cosUrl("http://localhost")
            .documentReference("connection_id", "bucket")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(textClassificationService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));

        Object syncCosHttpClient = getFieldValue(restclient, "syncCosHttpClient");
        assertEquals(customClient, getFieldValue(syncCosHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncCosHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncCosHttpClient, "delegate"));

        Object asyncCosHttpClient = getFieldValue(restclient, "asyncCosHttpClient");
        assertEquals(customClient, getFieldValue(asyncCosHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(asyncCosHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(asyncCosHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_text_classification_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                TextClassificationService textClassificationService = TextClassificationService.builder()
                    .baseUrl("https://localhost")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .cosUrl("http://localhost")
                    .verifySsl(verifySsl)
                    .documentReference("connection_id", "bucket")
                    .build();

                Object restclient = getFieldValue(textClassificationService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

                Object syncCosHttpClient = getFieldValue(restclient, "syncCosHttpClient");
                assertNotEquals(customClient, getFieldValue(syncCosHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncCosHttpClient, "delegate"));

                Object asyncCosHttpClient = getFieldValue(restclient, "asyncCosHttpClient");
                assertNotEquals(customClient, getFieldValue(asyncCosHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(asyncCosHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_text_extraction_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        TextExtractionService textExtractionService = TextExtractionService.builder()
            .baseUrl("https://localhost")
            .apiKey("apiKey")
            .projectId("projectId")
            .cosUrl("http://localhost")
            .documentReference("connection_id", "bucket")
            .resultReference("connection_id", "bucket")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(textExtractionService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));

        Object syncCosHttpClient = getFieldValue(restclient, "syncCosHttpClient");
        assertEquals(customClient, getFieldValue(syncCosHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncCosHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncCosHttpClient, "delegate"));

        Object asyncCosHttpClient = getFieldValue(restclient, "asyncCosHttpClient");
        assertEquals(customClient, getFieldValue(asyncCosHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(asyncCosHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(asyncCosHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_text_extraction_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                TextExtractionService textExtractionService = TextExtractionService.builder()
                    .baseUrl("https://localhost")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .cosUrl("http://localhost")
                    .verifySsl(verifySsl)
                    .documentReference("connection_id", "bucket")
                    .resultReference("connection_id", "bucket")
                    .build();

                Object restclient = getFieldValue(textExtractionService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

                Object syncCosHttpClient = getFieldValue(restclient, "syncCosHttpClient");
                assertNotEquals(customClient, getFieldValue(syncCosHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncCosHttpClient, "delegate"));

                Object asyncCosHttpClient = getFieldValue(restclient, "asyncCosHttpClient");
                assertNotEquals(customClient, getFieldValue(asyncCosHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(asyncCosHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_time_series_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        TimeSeriesService timeSeriesService = TimeSeriesService.builder()
            .baseUrl("https://localhost")
            .modelId("modelId")
            .apiKey("apiKey")
            .projectId("projectId")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(timeSeriesService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_time_series_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                TimeSeriesService timeSeriesService = TimeSeriesService.builder()
                    .baseUrl("https://localhost")
                    .modelId("modelId")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(timeSeriesService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_tokenization_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        TokenizationService tokenizationService = TokenizationService.builder()
            .baseUrl("https://localhost")
            .modelId("modelId")
            .apiKey("apiKey")
            .projectId("projectId")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(tokenizationService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));

        Object asyncHttpClient = getFieldValue(restclient, "asyncHttpClient");
        assertEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(asyncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(asyncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_tokenization_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                TokenizationService tokenizationService = TokenizationService.builder()
                    .baseUrl("https://localhost")
                    .modelId("modelId")
                    .apiKey("apiKey")
                    .projectId("projectId")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(tokenizationService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

                Object asyncHttpClient = getFieldValue(restclient, "asyncHttpClient");
                assertNotEquals(customClient, getFieldValue(asyncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(asyncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_custom_http_client_for_tool_service() throws Exception {

        HttpClient customClient = HttpClient.newHttpClient();
        ToolService toolService = ToolService.builder()
            .baseUrl("https://localhost")
            .apiKey("apiKey")
            .httpClient(customClient)
            .build();

        Object restclient = getFieldValue(toolService, "client");
        assertEquals(customClient, getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(restclient, "httpClient"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(restclient, "httpClient"));

        Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
        assertEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(true), getFieldValue(syncHttpClient, "delegate"));
        assertNotEquals(HttpClientProvider.httpClient(false), getFieldValue(syncHttpClient, "delegate"));
    }

    @Test
    void should_use_default_http_client_for_tool_service() throws Exception {

        Stream.of(true, false).forEach(verifySsl -> {

            try {

                HttpClient customClient = HttpClient.newHttpClient();
                ToolService toolService = ToolService.builder()
                    .baseUrl("https://localhost")
                    .apiKey("apiKey")
                    .verifySsl(verifySsl)
                    .build();

                Object restclient = getFieldValue(toolService, "client");
                assertNotEquals(customClient, getFieldValue(restclient, "httpClient"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(restclient, "httpClient"));

                Object syncHttpClient = getFieldValue(restclient, "syncHttpClient");
                assertNotEquals(customClient, getFieldValue(syncHttpClient, "delegate"));
                assertEquals(HttpClientProvider.httpClient(verifySsl), getFieldValue(syncHttpClient, "delegate"));

            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
