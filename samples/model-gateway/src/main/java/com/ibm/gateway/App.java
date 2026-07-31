/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.gateway;

import java.net.URI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.gateway.ModelGatewayChatProvider;
import com.ibm.watsonx.ai.gateway.ModelGatewayChatRequest;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.Router;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.Cache;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.ServiceTier;
import com.ibm.watsonx.ai.gateway.ModelGatewayService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var modelId = config.getValue("WATSONX_MODEL_ID", String.class);

        // Build default ModelGatewayParameters with router/cache and service tier
        var defaultParameters = ModelGatewayParameters.builder()
            .temperature(0.7)
            .serviceTier(ServiceTier.AUTO)
            .router(new Router(new Cache(false, null, null)))
            .build();

        // Assign to ModelGatewayChatProvider to illustrate adapter-style consumption
        ModelGatewayChatProvider gatewayProvider = ModelGatewayService.builder()
            .apiKey(apiKey)
            .baseUrl(url)
            .modelId(modelId)
            .parameters(defaultParameters)
            .build();

        var message = "What is the capital of Italy?";
        var chatRequest = ModelGatewayChatRequest.builder()
            .messages(UserMessage.text(message))
            .parameters(
                ModelGatewayParameters.builder()
                    .temperature(0.3)
                    .build())
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
