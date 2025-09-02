/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import java.net.URI;
import java.time.Duration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ControlMessage;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

public class AiService {

    private static final Config config = ConfigProvider.getConfig();
    private final ChatService chatService;
    private final FoundationModelService foundationModelService;
    private final ChatMemory memory;
    private String modelId;

    public AiService() {

        final var url = URI.create(config.getValue("WATSONX_URL", String.class));
        final var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        final var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);
        modelId = "ibm/granite-3-3-8b-instruct";

        AuthenticationProvider authProvider = IAMAuthenticator.builder()
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(60))
            .build();

        chatService = ChatService.builder()
            .authenticationProvider(authProvider)
            .projectId(projectId)
            .timeout(Duration.ofSeconds(60))
            .modelId(modelId)
            .url(url)
            .build();

        foundationModelService = FoundationModelService.builder()
            .authenticationProvider(authProvider)
            .url(url)
            .timeout(Duration.ofSeconds(60))
            .build();

        memory = new ChatMemory();
        memory.addMessage(ControlMessage.of("thinking"));
    }

    public ChatResponse chat(String message) {
        memory.addMessage(UserMessage.text(message));

        var parameters = ChatParameters.builder()
            .maxCompletionTokens(0)
            .build();

        ChatRequest chatRequest = ChatRequest.builder()
            .messages(memory.getMemory())
            .parameters(parameters)
            .thinking(ExtractionTags.of("think", "response"))
            .build();

        var response = chatService.chat(chatRequest);
        memory.addMessage(response.toAssistantMessage());
        return response;
    }

    public FoundationModel getModel() {
        return foundationModelService.getModels(Filter.of(modelId(modelId))).resources().get(0);
    }
}
