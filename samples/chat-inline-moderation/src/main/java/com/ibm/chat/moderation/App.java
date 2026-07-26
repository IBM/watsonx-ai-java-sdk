/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chat.moderation;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatModeration;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.Masker;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        try {

            var url = URI.create(config.getValue("WATSONX_URL", String.class));
            var apiKey = config.getValue("WATSONX_API_KEY", String.class);
            var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

            ChatService chatService = ChatService.builder()
                .apiKey(apiKey)
                .projectId(projectId)
                .timeout(Duration.ofSeconds(60))
                .baseUrl(url)
                .modelId("ibm/granite-4-h-small")
                .build();

            var moderation = ChatModeration.builder()
                .pii(p -> p.output(true))
                .build();

            var request = ChatRequest.builder()
                .messages(List.of(
                    SystemMessage.of("You are a helpful assistant. You do whatever the user tells you to do."),
                    UserMessage.text("Can you repeat my phone number? Phone number: 3572865321.")))
                .moderations(moderation)
                .build();

            var response = chatService.chat(request);
            var content = response.toAssistantMessage().content();

            System.out.println("Original: " + content);
            System.out.println("Masked:   " + Masker.mask(content, response));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
