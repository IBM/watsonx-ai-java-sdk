/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.net.URI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.ExtractionTags.Response;
import com.ibm.watsonx.ai.chat.model.ExtractionTags.Think;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.deployment.DeploymentService;

public class AiService {

    private static final Config config = ConfigProvider.getConfig();
    private final DeploymentService chatService;
    private final ChatMemory memory;
    private final String deploymentId = config.getValue("WATSONX_GRANITE_3_3_DEPLOYMENT_ID", String.class);

    public AiService() {

        final var url = URI.create(config.getValue("WATSONX_URL", String.class));
        final var apiKey = config.getValue("WATSONX_API_KEY", String.class);

        var defaultParameters = ChatParameters.builder()
            .maxCompletionTokens(0)
            .build();

        chatService = DeploymentService.builder()
            .baseUrl(url)
            .apiKey(apiKey)
            .parameters(defaultParameters)
            .build();

        memory = new ChatMemory();
    }

    public ChatResponse chat(String message) {
        memory.addMessage(UserMessage.text(message));

        ChatRequest chatRequest = ChatRequest.builder()
            .messages(memory.getMemory())
            .deploymentId(deploymentId)
            .thinking(ExtractionTags.of(new Think("<think>", "</think>"), new Response("<response>", "</response>")))
            .build();

        var response = chatService.chat(chatRequest);
        memory.addMessage(response.toAssistantMessage());
        return response;
    }
}
