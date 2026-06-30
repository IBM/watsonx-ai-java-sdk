/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.util.Scanner;

public class App {

    private static final AiService aiService = new AiService();

    public static void main(String[] args) throws Exception {

        System.out.println("Welcome to the IBM Watsonx Assistant Chatbot!");
        System.out.println("Type your message and press enter to send it:\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                var userInput = scanner.nextLine();
                var assistantMessage = aiService.chat(userInput).toAssistantMessage();
                System.out.println("""
                    Assistant:
                    ------------------
                    <think>
                    %s
                    </think>
                    ------------------
                    %s""".formatted(assistantMessage.thinking(), assistantMessage.content()));
                System.out.println();
            }
        }
    }
}
