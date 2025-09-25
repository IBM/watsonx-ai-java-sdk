/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

public class AiService {

    private static final Config config = ConfigProvider.getConfig();
    private String modelId;
    private final ChatService chatService;
    private final FoundationModelService foundationModelService;
    private final ChatMemory memory;

    public AiService() {

        final var url = URI.create(config.getValue("WATSONX_URL", String.class));
        final var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        final var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);
        modelId = "ibm/granite-3-3-8b-instruct";

        chatService = ChatService.builder()
            .apiKey(apiKey)
            .projectId(projectId)
            .timeout(Duration.ofSeconds(60))
            .modelId(modelId)
            .baseUrl(url)
            .build();

        foundationModelService = FoundationModelService.builder()
            .apiKey(apiKey)
            .baseUrl(url)
            .timeout(Duration.ofSeconds(60))
            .build();

        memory = new ChatMemory();
        memory.addMessage(ControlMessage.of("thinking"));
    }

    public CompletableFuture<Void> chat(String message, Consumer<String> response, Consumer<String> thinking) {

        var parameters = ChatParameters.builder()
            .maxCompletionTokens(0)
            .build();

        memory.addMessage(UserMessage.text(message));

        var chatRequest = ChatRequest.builder()
            .parameters(parameters)
            .messages(memory.getMemory())
            .thinking(ExtractionTags.of("think", "response"))
            .build();

        return chatService.chatStreaming(chatRequest, new ChatHandler() {
            private boolean firstReasoningChunk = true;
            private boolean firstResponseChunk = true;

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                if (firstResponseChunk && !firstReasoningChunk) {
                    thinking.accept("\n</Thinking>\n");
                    firstResponseChunk = false;
                }
                response.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                memory.addMessage(completeResponse.toAssistantMessage());
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }

            @Override
            public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
                if (firstReasoningChunk) {
                    thinking.accept("\n<Thinking>\n");
                    firstReasoningChunk = false;
                }
                thinking.accept(partialThinking);
            }
        });
    }

    public FoundationModel getModel() {
        return foundationModelService.getModels(Filter.of(modelId(modelId))).resources().get(0);
    }
}
