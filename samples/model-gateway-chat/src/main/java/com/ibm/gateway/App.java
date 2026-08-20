/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.gateway;

import java.net.URI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatRequest;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var modelId = config.getValue("WATSONX_MODEL_ID", String.class);

        var gatewayProvider = ModelGatewayChatService.builder()
            .apiKey(apiKey)
            .baseUrl(url)
            .modelId(modelId)
            .build();

        var message = "What is the capital of Italy?";
        var chatRequest = ModelGatewayChatRequest.builder()
            .messages(UserMessage.text(message))
            .build();

        var response = gatewayProvider.chat(chatRequest);

        System.out.println("USER: " + message);
        System.out.println("ASSISTANT: " + response.toAssistantMessage().content());
        System.out.println();
        System.out.println("--- Gateway metadata ---");
        System.out.println("Model: " + response.model());
        System.out.println("Service tier: " + response.serviceTier());
        System.out.println("System fingerprint: " + response.systemFingerprint());
        System.out.println("Cached: " + response.cached());
        System.out.println("Usage - prompt tokens: " + (response.usage() != null ? response.usage().promptTokens() : "n/a"));
        System.out.println("Usage - completion tokens: " + (response.usage() != null ? response.usage().completionTokens() : "n/a"));
    }
}
