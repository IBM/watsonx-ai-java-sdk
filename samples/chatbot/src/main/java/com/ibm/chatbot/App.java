/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.util.Scanner;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;

public class App {

  private static final AiService aiService = new AiService();

  public static void main(String[] args) throws Exception {

    FoundationModel foundationModel = aiService.getModel();
    System.out.println("Welcome to the IBM Watsonx Assistant Chatbot!");
    System.out.println("""
      ---------------------------------------------
      Model: %s
      Max Sequence Length: %s
      Max Output Tokens: %s
      Supported Languages: %s
      ---------------------------------------------""".formatted(
      foundationModel.modelId(), foundationModel.maxSequenceLength(),
      foundationModel.maxOutputTokens(), foundationModel.supportedLanguages())
    );

    System.out.println("Type your message and press enter to send it:\n");

    try (Scanner scanner = new Scanner(System.in)) {
      while (true) {
        System.out.print("You: ");
        String userInput = scanner.nextLine();
        String response = aiService.chat(userInput);
        System.out.println("Assistant: " + response);
        System.out.println();
      }
    }
  }
}
