/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.util.Scanner;

public class App {

    private static final ChatBot chatBot = new ChatBot();

    public static void main(String[] args) throws Exception {

        System.out.println("---------------------------------------------");
        System.out.println("Welcome to the IBM Watsonx Assistant Chatbot!");
        System.out.println("---------------------------------------------");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                String userInput = scanner.nextLine();
                System.out.print("Assistant: ");
                chatBot.chat(userInput).get();
                System.out.println();
            }
        }
    }
}
