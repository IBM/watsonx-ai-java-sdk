/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
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
        modelId = config.getOptionalValue("WATSONX_MODEL_ID", String.class).orElse("mistralai/mistral-medium-2505");

        var defaultParameters = ChatParameters.builder()
            .maxCompletionTokens(0)
            .build();

        chatService = ChatService.builder()
            .baseUrl(url)
            .apiKey(apiKey)
            .modelId(modelId)
            .projectId(projectId)
            .defaultParameters(defaultParameters)
            .build();

        foundationModelService = FoundationModelService.builder()
            .baseUrl(url)
            .apiKey(apiKey)
            .build();

        memory = new ChatMemory();
        memory.addMessage(SystemMessage.of("You are an helpful assistant"));
    }

    public CompletableFuture<ChatResponse> chat(String message, Consumer<String> handler) {

        memory.addMessage(UserMessage.text(message));
        return chatService.chatStreaming(memory.getMemory(), new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                handler.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                memory.addMessage(completeResponse.toAssistantMessage());
            }

            @Override
            public void onError(Throwable error) {
                System.err.println(error);
            }
        });
    }

    public FoundationModel getModel() {
        return foundationModelService.getModels(Filter.of(modelId(modelId))).resources().get(0);
    }
}
