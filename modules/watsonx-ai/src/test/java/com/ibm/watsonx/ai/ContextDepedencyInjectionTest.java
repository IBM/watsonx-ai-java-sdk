/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.detection.DetectionService;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.rerank.RerankService;
import com.ibm.watsonx.ai.textgeneration.TextGenerationService;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationService;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;
import com.ibm.watsonx.ai.timeseries.TimeSeriesService;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
import com.ibm.watsonx.ai.tool.builtin.PythonInterpreterTool;
import com.ibm.watsonx.ai.tool.builtin.TavilySearchTool;
import com.ibm.watsonx.ai.tool.builtin.WeatherTool;
import com.ibm.watsonx.ai.tool.builtin.WebCrawlerTool;
import com.ibm.watsonx.ai.tool.builtin.WikipediaTool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@EnableWeld
public class ContextDepedencyInjectionTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator
        .from(Producer.class, ChatService.class, DeploymentService.class,
            DetectionService.class, EmbeddingService.class, FoundationModelService.class,
            RerankService.class, TextGenerationService.class, TextClassificationService.class,
            TextExtractionService.class, TimeSeriesService.class, ToolService.class)
        .build();

    @Inject
    ChatService chatService;

    @Inject
    DeploymentService deploymentService;

    @Inject
    DetectionService detectionService;

    @Inject
    EmbeddingService embeddingService;

    @Inject
    FoundationModelService foundationModelService;

    @Inject
    RerankService rerankService;

    @Inject
    TextGenerationService textGenerationService;

    @Inject
    TextClassificationService textClassificationService;

    @Inject
    TextExtractionService textExtractionService;

    @Inject
    TimeSeriesService timeSeriesService;

    @Inject
    ToolService toolService;

    @Inject
    GoogleSearchTool googleSearchTool;

    @Inject
    PythonInterpreterTool pythonInterpreterTool;;

    @Inject
    TavilySearchTool tavilySearchTool;

    @Inject
    WeatherTool weatherTool;

    @Inject
    WebCrawlerTool webCrawlerTool;

    @Inject
    WikipediaTool wikipediaTool;

    @Test
    void should_inject_chat_service() {
        assertNotNull(chatService);
    }

    @Test
    void should_inject_deployment_service() {
        assertNotNull(deploymentService);
    }

    @Test
    void should_inject_detection_service() {
        assertNotNull(detectionService);
    }

    @Test
    void should_inject_embedding_service() {
        assertNotNull(embeddingService);
    }

    @Test
    void should_inject_foundation_model_service() {
        assertNotNull(foundationModelService);
    }

    @Test
    void should_inject_rerank_service() {
        assertNotNull(rerankService);
    }

    @Test
    void should_inject_text_generation_service() {
        assertNotNull(textGenerationService);
    }

    @Test
    void should_inject_text_classification_service() {
        assertNotNull(textClassificationService);
    }

    @Test
    void should_inject_text_extraction_service() {
        assertNotNull(textExtractionService);
    }

    @Test
    void should_inject_time_series_service() {
        assertNotNull(timeSeriesService);
    }

    @Test
    void should_inject_tool_service() {
        assertNotNull(toolService);
    }

    @Test
    void should_inject_google_search_tool() {
        assertNotNull(googleSearchTool);
    }

    @ApplicationScoped
    public static class Producer {

        @Produces
        public ChatService produceChatService() {
            return ChatService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .projectId("project-id")
                .modelId("model-id")
                .build();
        }

        @Produces
        public DeploymentService produceDeploymentService() {
            return DeploymentService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .build();
        }

        @Produces
        public DetectionService produceDetectionService() {
            return DetectionService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .projectId("project-id")
                .build();
        }

        @Produces
        public EmbeddingService produceEmbeddingService() {
            return EmbeddingService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .projectId("project-id")
                .modelId("model-id")
                .build();
        }

        @Produces
        public FoundationModelService produceFoundationModelService() {
            return FoundationModelService.builder()
                .baseUrl("https://example.com")
                .build();
        }

        @Produces
        public RerankService produceRerankService() {
            return RerankService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .projectId("project-id")
                .modelId("model-id")
                .build();
        }

        @Produces
        public TextGenerationService produceTextGenerationService() {
            return TextGenerationService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .projectId("project-id")
                .modelId("model-id")
                .build();
        }

        @Produces
        public TextClassificationService produceTextClassificationService() {
            return TextClassificationService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .projectId("project-id")
                .cosUrl("https://example.com")
                .documentReference("connection-id", "bucket")
                .build();
        }

        @Produces
        public TextExtractionService produceTextExtractionService() {
            return TextExtractionService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .projectId("project-id")
                .cosUrl("https://example.com")
                .documentReference("connection-id", "bucket")
                .resultReference("connection-id", "bucket")
                .build();
        }

        @Produces
        public TimeSeriesService produceTimeSeriesService() {
            return TimeSeriesService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .projectId("project-id")
                .modelId("model-id")
                .build();
        }

        @Produces
        public ToolService produceToolService() {
            return ToolService.builder()
                .baseUrl("https://example.com")
                .apiKey("api-key")
                .build();
        }

        @Produces
        public GoogleSearchTool produceGoogleSearchTool() {
            return new GoogleSearchTool(produceToolService());
        }

        @Produces
        public PythonInterpreterTool producePythonInterpreterTool() {
            return new PythonInterpreterTool(produceToolService(), "deployment-id");
        }

        @Produces
        public TavilySearchTool produceTavilySearchTool() {
            return new TavilySearchTool(produceToolService(), "api-key");
        }

        @Produces
        public WeatherTool produceWeatherTool() {
            return new WeatherTool(produceToolService());
        }

        @Produces
        public WebCrawlerTool produceWebCrawlerTool() {
            return new WebCrawlerTool(produceToolService());
        }

        @Produces
        public WikipediaTool produceWikipediaTool() {
            return new WikipediaTool(produceToolService());
        }
    }
}
