/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.cp4d;

import java.net.URI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        final var baseUrl = URI.create(config.getValue("CP4D_URL", String.class));
        final var username = config.getValue("CP4D_USERNAME", String.class);
        final var apiKey = config.getValue("CP4D_API_KEY", String.class);
        final var projectId = config.getValue("CP4D_PROJECT_ID", String.class);

        ChatService chatService = ChatService.builder()
            .baseUrl(baseUrl)
            .modelId("ibm/granite-3-2-8b-instruct")
            .projectId(projectId)
            .authenticator(
                CP4DAuthenticator.builder()
                    .baseUrl(baseUrl)
                    .username(username)
                    .apiKey(apiKey)
                    .build()
            ).build();

        var assistantMessage = chatService.chat("How are you?").toAssistantMessage();

        System.out.println("USER: ".concat("How are you?"));
        System.out.println("ASSISTANT: ".concat(assistantMessage.content()));
    }
}
