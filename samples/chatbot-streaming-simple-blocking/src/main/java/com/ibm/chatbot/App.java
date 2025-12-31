/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Scanner;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class App {

    static final Config config = ConfigProvider.getConfig();
    static final ChatService chatService = ChatService.builder()
        .modelId("ibm/granite-4-h-small")
        .baseUrl(config.getValue("WATSONX_URL", String.class))
        .apiKey(config.getValue("WATSONX_API_KEY", String.class))
        .projectId(config.getValue("WATSONX_PROJECT_ID", String.class))
        .tools(Tool.of("get_current_time", "Get the current time"))
        .parameters(ChatParameters.builder().maxCompletionTokens(0).build())
        .build();


    public static void main(String[] args) {

        var messages = new ArrayList<ChatMessage>();

        System.out.println("---------------------------------------------");
        System.out.println("Welcome to the IBM Watsonx Assistant Chatbot!");
        System.out.println("Type your message and press enter to send it");
        System.out.println("---------------------------------------------");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("User: ");
                messages.add(UserMessage.text(scanner.nextLine()));
                System.out.print("Assistant: ");
                var assistantMessage = chatService.chatStreaming(messages, System.out::print).join().toAssistantMessage();
                messages.add(assistantMessage);
                if (assistantMessage.hasToolCalls()) {
                    var toolMessage = assistantMessage.processTools((toolName, toolArgs) -> LocalTime.now());
                    messages.addAll(toolMessage);
                    assistantMessage = chatService.chatStreaming(messages, System.out::print).join().toAssistantMessage();
                    messages.add(assistantMessage);
                }
                System.out.println();
            }
        }
    }
}
