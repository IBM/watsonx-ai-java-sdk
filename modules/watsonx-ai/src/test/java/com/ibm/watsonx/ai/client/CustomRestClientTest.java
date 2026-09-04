/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.batch.BatchService;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.client.impl.CustomBatchRestClient;
import com.ibm.watsonx.ai.client.impl.CustomCP4DIAMRestClient;
import com.ibm.watsonx.ai.client.impl.CustomCP4DLegacyRestClient;
import com.ibm.watsonx.ai.client.impl.CustomCP4DZenRestClient;
import com.ibm.watsonx.ai.client.impl.CustomChatRestClient;
import com.ibm.watsonx.ai.client.impl.CustomCreateSchemaRestClient;
import com.ibm.watsonx.ai.client.impl.CustomDeploymentRestClient;
import com.ibm.watsonx.ai.client.impl.CustomDetectionRestClient;
import com.ibm.watsonx.ai.client.impl.CustomEmbeddingRestClient;
import com.ibm.watsonx.ai.client.impl.CustomFileRestClient;
import com.ibm.watsonx.ai.client.impl.CustomFoundationModelRestClient;
import com.ibm.watsonx.ai.client.impl.CustomIBMCloudRestClient;
import com.ibm.watsonx.ai.client.impl.CustomImproveSchemaRestClient;
import com.ibm.watsonx.ai.client.impl.CustomMergeSchemaRestClient;
import com.ibm.watsonx.ai.client.impl.CustomModelGatewayCatalogRestClient;
import com.ibm.watsonx.ai.client.impl.CustomModelGatewayChatRestClient;
import com.ibm.watsonx.ai.client.impl.CustomModelGatewayEmbeddingRestClient;
import com.ibm.watsonx.ai.client.impl.CustomModelGatewayImageRestClient;
import com.ibm.watsonx.ai.client.impl.CustomRerankRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTextClassificationRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTextExtractionRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTextGenerationRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTimeSeriesRestClient;
import com.ibm.watsonx.ai.client.impl.CustomTokenizationRestClient;
import com.ibm.watsonx.ai.client.impl.CustomToolRestClient;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.cp4d.AuthMode;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import com.ibm.watsonx.ai.file.FileService;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.gateway.catalog.ModelGatewayCatalogService;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatService;
import com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingService;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageService;
import com.ibm.watsonx.ai.rerank.RerankService;
import com.ibm.watsonx.ai.textgeneration.TextGenerationService;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaService;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationService;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;
import com.ibm.watsonx.ai.timeseries.TimeSeriesService;
import com.ibm.watsonx.ai.tokenization.TokenizationService;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.utils.ServiceLoaderUtils;

public class CustomRestClientTest extends AbstractWatsonxTest {

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

        withWatsonxServiceMock(() -> {
            ChatService chatService = ChatService.builder()
                .apiKey("test")
                .modelId("model-id")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<ChatService> clazz = ChatService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(chatService);
                assertTrue(client instanceof CustomChatRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // resources/META-INF/services/com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudRestClient$IBMCloudRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_ibm_cloud_provider() throws Exception {

        withWatsonxServiceMock(() -> {
            Authenticator authenticator = IBMCloudAuthenticator.builder()
                .apiKey("test")
                .build();

            try {
                Class<IBMCloudAuthenticator> clazz = IBMCloudAuthenticator.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(authenticator);
                assertTrue(client instanceof CustomIBMCloudRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // resources/META-INF/services/com.ibm.watsonx.ai.core.auth.cp4d.CP4DRestClient$CP4DLegacyRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_cp4d_legacy_provider() throws Exception {

        withWatsonxServiceMock(() -> {
            Authenticator authenticator = CP4DAuthenticator.builder()
                .baseUrl("https://localhost")
                .username("username")
                .password("password")
                .build();

            try {
                Class<CP4DAuthenticator> clazz = CP4DAuthenticator.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(authenticator);
                assertTrue(client instanceof CustomCP4DLegacyRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // resources/META-INF/services/com.ibm.watsonx.ai.core.auth.cp4d.CP4DRestClient$CP4DIAMRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_cp4d_iam_provider() throws Exception {

        withWatsonxServiceMock(() -> {
            Authenticator authenticator = CP4DAuthenticator.builder()
                .baseUrl("https://localhost")
                .username("username")
                .password("password")
                .authMode(AuthMode.IAM)
                .build();

            try {
                Class<CP4DAuthenticator> clazz = CP4DAuthenticator.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(authenticator);
                assertTrue(client instanceof CustomCP4DIAMRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // resources/META-INF/services/com.ibm.watsonx.ai.core.auth.cp4d.CP4DRestClient$CP4DZenRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_cp4d_zen_provider() throws Exception {

        withWatsonxServiceMock(() -> {
            Authenticator authenticator = CP4DAuthenticator.builder()
                .baseUrl("https://localhost")
                .username("username")
                .password("password")
                .authMode(AuthMode.ZEN_API_KEY)
                .build();

            try {
                Class<CP4DAuthenticator> clazz = CP4DAuthenticator.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(authenticator);
                assertTrue(client instanceof CustomCP4DZenRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.deployment.DeploymentRestClient$DeploymentRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_deployment_service() throws Exception {

        withWatsonxServiceMock(() -> {
            DeploymentService deploymentService = DeploymentService.builder()
                .apiKey("test")
                .baseUrl("http://localhost")
                .build();

            try {
                Class<DeploymentService> clazz = DeploymentService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(deploymentService);
                assertTrue(client instanceof CustomDeploymentRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.gateway.ModelGatewayChatRestClient$ModelGatewayChatRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_model_gateway_service() throws Exception {

        withWatsonxServiceMock(() -> {
            ModelGatewayChatService modelGatewayChatService = ModelGatewayChatService.builder()
                .apiKey("test")
                .modelId("model-id")
                .baseUrl("http://localhost")
                .build();

            try {
                Class<ModelGatewayChatService> clazz = ModelGatewayChatService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(modelGatewayChatService);
                assertTrue(client instanceof CustomModelGatewayChatRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.gateway.catalog.ModelGatewayCatalogRestClient$ModelGatewayCatalogRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_model_gateway_catalog_service() throws Exception {

        withWatsonxServiceMock(() -> {
            ModelGatewayCatalogService catalogService = ModelGatewayCatalogService.builder()
                .apiKey("test")
                .baseUrl("http://localhost")
                .build();

            try {
                Class<ModelGatewayCatalogService> clazz = ModelGatewayCatalogService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(catalogService);
                assertTrue(client instanceof CustomModelGatewayCatalogRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.embedding.EmbeddingRestClient$EmbeddingRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_embedding_service() throws Exception {

        withWatsonxServiceMock(() -> {
            EmbeddingService embeddingService = EmbeddingService.builder()
                .apiKey("test")
                .modelId("model-id")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<EmbeddingService> clazz = EmbeddingService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(embeddingService);
                assertTrue(client instanceof CustomEmbeddingRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.foundationmodel.FoundationModelRestClient$FoundationModelRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_foundation_model_service() throws Exception {

        withWatsonxServiceMock(() -> {
            FoundationModelService foundationModelService = FoundationModelService.builder()
                .baseUrl("http://localhost")
                .build();

            try {
                Class<FoundationModelService> clazz = FoundationModelService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(foundationModelService);
                assertTrue(client instanceof CustomFoundationModelRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.rerank.RerankRestClient$RerankRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_rerank_service() throws Exception {

        withWatsonxServiceMock(() -> {
            RerankService rerankService = RerankService.builder()
                .apiKey("test")
                .modelId("model-id")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<RerankService> clazz = RerankService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(rerankService);
                assertTrue(client instanceof CustomRerankRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaRestClient$CreateSchemaRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_create_schema_service() throws Exception {

        withWatsonxServiceMock(() -> {
            CreateSchemaService createSchemaService = CreateSchemaService.builder()
                .apiKey("test")
                .cosUrl("http://localhost")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .documentReference("test", "test")
                .build();

            try {
                Class<CreateSchemaService> clazz = CreateSchemaService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(createSchemaService);
                assertTrue(client instanceof CustomCreateSchemaRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaRestClient$ImproveSchemaRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_improve_schema_service() throws Exception {

        withWatsonxServiceMock(() -> {
            ImproveSchemaService improveSchemaService = ImproveSchemaService.builder()
                .apiKey("test")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<ImproveSchemaService> clazz = ImproveSchemaService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(improveSchemaService);
                assertTrue(client instanceof CustomImproveSchemaRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaRestClient$MergeSchemaRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_merge_schema_service() throws Exception {

        withWatsonxServiceMock(() -> {
            MergeSchemaService mergeSchemaService = MergeSchemaService.builder()
                .apiKey("test")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<MergeSchemaService> clazz = MergeSchemaService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(mergeSchemaService);
                assertTrue(client instanceof CustomMergeSchemaRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionRestClient$TextExtractionRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_text_extraction_service() throws Exception {

        withWatsonxServiceMock(() -> {
            TextExtractionService textExtractionService = TextExtractionService.builder()
                .apiKey("test")
                .cosUrl("http://localhost")
                .baseUrl("http://localhost")
                .documentReference("test", "test")
                .resultReference("test", "test")
                .projectId("project-id")
                .build();

            try {
                Class<TextExtractionService> clazz = TextExtractionService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(textExtractionService);
                assertTrue(client instanceof CustomTextExtractionRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationRestClient$TextClassificationRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_text_classification_service() throws Exception {

        withWatsonxServiceMock(() -> {
            TextClassificationService textClassificationService = TextClassificationService.builder()
                .apiKey("test")
                .cosUrl("http://localhost")
                .baseUrl("http://localhost")
                .documentReference("test", "test")
                .projectId("project-id")
                .build();

            try {
                Class<TextClassificationService> clazz = TextClassificationService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(textClassificationService);
                assertTrue(client instanceof CustomTextClassificationRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.textgeneration.TextGenerationRestClient$TextGenerationRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_text_generation_service() throws Exception {

        withWatsonxServiceMock(() -> {
            TextGenerationService textGenerationService = TextGenerationService.builder()
                .apiKey("test")
                .modelId("test")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<TextGenerationService> clazz = TextGenerationService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(textGenerationService);
                assertTrue(client instanceof CustomTextGenerationRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.timeseries.TimeSeriesRestClient$TimeSeriesRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_time_series_service() throws Exception {

        withWatsonxServiceMock(() -> {
            TimeSeriesService timeSeriesService = TimeSeriesService.builder()
                .apiKey("test")
                .modelId("test")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<TimeSeriesService> clazz = TimeSeriesService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(timeSeriesService);
                assertTrue(client instanceof CustomTimeSeriesRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.tokenization.TokenizationRestClient$TokenizationRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_tokenization_service() throws Exception {

        withWatsonxServiceMock(() -> {
            TokenizationService tokenizationService = TokenizationService.builder()
                .apiKey("test")
                .modelId("test")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<TokenizationService> clazz = TokenizationService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(tokenizationService);
                assertTrue(client instanceof CustomTokenizationRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.tool.ToolRestClient$ToolRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_tool_service() throws Exception {

        withWatsonxServiceMock(() -> {
            ToolService toolService = ToolService.builder()
                .apiKey("test")
                .baseUrl("http://localhost")
                .build();

            try {
                Class<ToolService> clazz = ToolService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(toolService);
                assertTrue(client instanceof CustomToolRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.detection.DetectionRestClient$DetectionRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_detection_service() throws Exception {

        withWatsonxServiceMock(() -> {
            DetectionService detectionService = DetectionService.builder()
                .apiKey("test")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<DetectionService> clazz = DetectionService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(detectionService);
                assertTrue(client instanceof CustomDetectionRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.file.FileRestClient$FileRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_file_service() throws Exception {

        withWatsonxServiceMock(() -> {
            FileService fileService = FileService.builder()
                .apiKey("test")
                .baseUrl("http://localhost")
                .projectId("project-id")
                .build();

            try {
                Class<FileService> clazz = FileService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(fileService);
                assertTrue(client instanceof CustomFileRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.gateway.embedding.ModelGatewayEmbeddingRestClient$ModelGatewayEmbeddingRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_model_gateway_embedding_service() throws Exception {

        withWatsonxServiceMock(() -> {
            ModelGatewayEmbeddingService embeddingService = ModelGatewayEmbeddingService.builder()
                .apiKey("test")
                .modelId("model-id")
                .baseUrl("http://localhost")
                .build();

            try {
                Class<ModelGatewayEmbeddingService> clazz = ModelGatewayEmbeddingService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(embeddingService);
                assertTrue(client instanceof CustomModelGatewayEmbeddingRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.batch.BatchRestClient$BatchRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_batch_service() throws Exception {

        withWatsonxServiceMock(() -> {
            try {
                FileService fileService = FileService.builder()
                    .apiKey("test")
                    .baseUrl("http://localhost")
                    .projectId("project-id")
                    .build();

                BatchService batchService = BatchService.builder()
                    .apiKey("test")
                    .baseUrl("http://localhost")
                    .projectId("project-id")
                    .endpoint("/v1/chat/completions")
                    .fileService(fileService)
                    .build();

                Class<BatchService> clazz = BatchService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(batchService);
                assertTrue(client instanceof CustomBatchRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    // com.ibm.watsonx.ai.gateway.image.ModelGatewayImageRestClient$ModelGatewayImageRestClientBuilderFactory
    public void should_use_custom_rest_client_when_building_model_gateway_image_service() throws Exception {

        withWatsonxServiceMock(() -> {
            ModelGatewayImageService imageService = ModelGatewayImageService.builder()
                .apiKey("test")
                .modelId("model-id")
                .baseUrl("http://localhost")
                .build();

            try {
                Class<ModelGatewayImageService> clazz = ModelGatewayImageService.class;
                var clientField = clazz.getDeclaredField("client");
                clientField.setAccessible(true);
                var client = clientField.get(imageService);
                assertTrue(client instanceof CustomModelGatewayImageRestClient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
