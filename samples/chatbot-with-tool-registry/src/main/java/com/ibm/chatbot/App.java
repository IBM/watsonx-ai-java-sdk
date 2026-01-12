/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.net.URI;
import java.util.ArrayList;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.ToolRegistry;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.tool.ToolService;
import com.ibm.watsonx.ai.tool.builtin.GoogleSearchTool;
import com.ibm.watsonx.ai.tool.builtin.WebCrawlerTool;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        var URL = URI.create(config.getValue("WATSONX_URL", String.class));
        var WX_URL = URI.create(config.getValue("WATSONX_WX_URL", String.class));
        var API_KEY = config.getValue("WATSONX_API_KEY", String.class);
        var PROJECT_ID = config.getValue("WATSONX_PROJECT_ID", String.class);

        ToolService toolService = ToolService.builder()
            .apiKey(API_KEY)
            .baseUrl(WX_URL)
            .build();

        ToolRegistry toolRegistry = ToolRegistry.builder()
            .register(new GoogleSearchTool(toolService), new WebCrawlerTool(toolService))
            .beforeExecution((toolName, toolArgs) -> System.out.println("%s -> %s".formatted(toolName, toolArgs)))
            .build();

        ChatService chatService = ChatService.builder()
            .baseUrl(URL)
            .apiKey(API_KEY)
            .projectId(PROJECT_ID)
            .modelId("ibm/granite-4-h-small")
            .parameters(ChatParameters.builder().temperature(0.0).build())
            .tools(toolRegistry.tools())
            .build();

        var messages = new ArrayList<ChatMessage>();
        var question = "Is there a watsonx.ai Java SDK? If yes, give me the last commit";
        messages.add(SystemMessage.of("You are a helpful assistant. Answer user questions concisely."));
        messages.add(UserMessage.text(question));

        System.out.println("User: %s".formatted(question));
        var assistantMessage = chatService.chat(messages).toAssistantMessage();
        messages.add(assistantMessage);

        System.out.println("---------------TOOL_EXECUTION---------------");
        while (assistantMessage.hasToolCalls()) {
            var toolMessages = assistantMessage.processTools(toolRegistry::execute);
            messages.addAll(toolMessages);
            assistantMessage = chatService.chat(messages).toAssistantMessage();
            messages.add(assistantMessage);
        }
        System.out.println("--------------------------------------------");
        System.out.println("Assistant: %s".formatted(assistantMessage.content()));
    }
}
