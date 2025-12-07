/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.client.impl.CustomCP4DRestClient;
import com.ibm.watsonx.ai.client.impl.CustomChatRestClient;
import com.ibm.watsonx.ai.client.impl.CustomDeploymentRestClient;
import com.ibm.watsonx.ai.client.impl.CustomDetectionRestClient;
import com.ibm.watsonx.ai.client.impl.CustomEmbeddingRestClient;
import com.ibm.watsonx.ai.client.impl.CustomFoundationModelRestClient;
import com.ibm.watsonx.ai.client.impl.CustomIBMCloudRestClient;
import com.ibm.watsonx.ai.client.impl.CustomRerankRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTextClassificationRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTextExtractionRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTextGenerationRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTimeSeriesRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTokenizationRestClient;
import com.ibm.watsonx.ai.client.impl.CustomToolRestClient;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
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
import com.ibm.watsonx.ai.utils.ServiceLoaderUtils;

public class CustomRestClientTest {

    @BeforeEach
    void setup() throws Exception {
        ServiceLoaderUtils.setupServiceLoader();
    }

    @AfterEach
    void cleanup() throws Exception {
        ServiceLoaderUtils.cleanupServiceLoader();
    }

    @Test
    // resources/META-INF/services/com.ibm.watsonx.ai.chat.ChatRestClient$ChatRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_chat_service() throws Exception {

        ChatService chatService = ChatService.builder()
            .apiKey("test")
            .modelId("model-id")
            .baseUrl("http://localhost")
            .projectId("project-id")
            .build();

        Class<ChatService> clazz = ChatService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(chatService);
        assertTrue(client instanceof CustomChatRestClient);
    }

    @Test
    // resources/META-INF/services/com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudRestClient$IBMCloudRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_ibm_cloud_provider() throws Exception {

        Authenticator authenticator = IBMCloudAuthenticator.builder()
            .apiKey("test")
            .build();

        Class<IBMCloudAuthenticator> clazz = IBMCloudAuthenticator.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(authenticator);
        assertTrue(client instanceof CustomIBMCloudRestClient);
    }

    @Test
    // resources/META-INF/services/com.ibm.watsonx.ai.core.auth.cp4d.CP4DRestClient$CP4DRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_cp4d_provider() throws Exception {

        Authenticator authenticator = CP4DAuthenticator.builder()
            .baseUrl("https://localhost")
            .username("username")
            .apiKey("api-key")
            .build();

        Class<CP4DAuthenticator> clazz = CP4DAuthenticator.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(authenticator);
        assertTrue(client instanceof CustomCP4DRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.deployment.DeploymentRestClient$DeploymentRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_deployment_service() throws Exception {

        DeploymentService deploymentService = DeploymentService.builder()
            .apiKey("test")
            .baseUrl("http://localhost")
            .build();

        Class<DeploymentService> clazz = DeploymentService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(deploymentService);
        assertTrue(client instanceof CustomDeploymentRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.embedding.EmbeddingRestClient$EmbeddingRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_embedding_service() throws Exception {

        EmbeddingService embeddingService = EmbeddingService.builder()
            .apiKey("test")
            .modelId("model-id")
            .baseUrl("http://localhost")
            .projectId("project-id")
            .build();

        Class<EmbeddingService> clazz = EmbeddingService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(embeddingService);
        assertTrue(client instanceof CustomEmbeddingRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.foundationmodel.FoundationModelRestClient$FoundationModelRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_foundation_model_service() throws Exception {

        FoundationModelService foundationModelService = FoundationModelService.builder()
            .baseUrl("http://localhost")
            .build();

        Class<FoundationModelService> clazz = FoundationModelService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(foundationModelService);
        assertTrue(client instanceof CustomFoundationModelRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.rerank.RerankRestClient$RerankRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_rerank_service() throws Exception {

        RerankService rerankService = RerankService.builder()
            .apiKey("test")
            .modelId("model-id")
            .baseUrl("http://localhost")
            .projectId("project-id")
            .build();

        Class<RerankService> clazz = RerankService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(rerankService);
        assertTrue(client instanceof CustomRerankRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionRestClient$TextExtractionRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_text_extraction_service() throws Exception {

        TextExtractionService textExtractionService = TextExtractionService.builder()
            .apiKey("test")
            .cosUrl("http://localhost")
            .baseUrl("http://localhost")
            .documentReference("test", "test")
            .resultReference("test", "test")
            .projectId("project-id")
            .build();

        Class<TextExtractionService> clazz = TextExtractionService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(textExtractionService);
        assertTrue(client instanceof CustomTextExtractionRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationRestClient$TextClassificationRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_text_classification_service() throws Exception {

        TextClassificationService textClassificationService = TextClassificationService.builder()
            .apiKey("test")
            .cosUrl("http://localhost")
            .baseUrl("http://localhost")
            .documentReference("test", "test")
            .projectId("project-id")
            .build();

        Class<TextClassificationService> clazz = TextClassificationService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(textClassificationService);
        assertTrue(client instanceof CustomTextClassificationRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.textgeneration.TextGenerationRestClient$TextGenerationRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_text_generation_service() throws Exception {

        TextGenerationService textGenerationService = TextGenerationService.builder()
            .apiKey("test")
            .modelId("test")
            .baseUrl("http://localhost")
            .projectId("project-id")
            .build();

        Class<TextGenerationService> clazz = TextGenerationService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(textGenerationService);
        assertTrue(client instanceof CustomTextGenerationRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.timeseries.TimeSeriesRestClient$TimeSeriesRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_time_series_service() throws Exception {

        TimeSeriesService timeSeriesService = TimeSeriesService.builder()
            .apiKey("test")
            .modelId("test")
            .baseUrl("http://localhost")
            .projectId("project-id")
            .build();

        Class<TimeSeriesService> clazz = TimeSeriesService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(timeSeriesService);
        assertTrue(client instanceof CustomTimeSeriesRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.tokenization.TokenizationRestClient$TokenizationRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_tokenization_service() throws Exception {

        TokenizationService tokenizationService = TokenizationService.builder()
            .apiKey("test")
            .modelId("test")
            .baseUrl("http://localhost")
            .projectId("project-id")
            .build();

        Class<TokenizationService> clazz = TokenizationService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(tokenizationService);
        assertTrue(client instanceof CustomTokenizationRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.tool.ToolRestClient$ToolRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_tool_service() throws Exception {

        ToolService toolService = ToolService.builder()
            .apiKey("test")
            .baseUrl("http://localhost")
            .build();

        Class<ToolService> clazz = ToolService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(toolService);
        assertTrue(client instanceof CustomToolRestClient);
    }

    @Test
    // com.ibm.watsonx.ai.detection.DetectionRestClient$DetectionRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_detection_service() throws Exception {

        DetectionService detectionService = DetectionService.builder()
            .apiKey("test")
            .baseUrl("http://localhost")
            .projectId("project-id")
            .build();

        Class<DetectionService> clazz = DetectionService.class;
        var clientField = clazz.getDeclaredField("client");
        clientField.setAccessible(true);
        var client = clientField.get(detectionService);
        assertTrue(client instanceof CustomDetectionRestClient);
    }
}
