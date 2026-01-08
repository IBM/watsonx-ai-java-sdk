/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.util.LinkedList;
import java.util.List;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.ToolMessage;

public class ChatMemory {

    private static final int MAX_SIZE = 10;
    private List<ChatMessage> memory;

    public ChatMemory() {
        memory = new LinkedList<>();
    }

    public void addMessages(List<? extends ChatMessage> messages) {
        messages.forEach(this::addMessage);
    }

    public synchronized void addMessage(ChatMessage message) {
        if (memory.size() >= MAX_SIZE) {
            int index = (memory.get(0) instanceof SystemMessage) ? 1 : 0;
            var removedMessage = memory.remove(index);
            // If we removed an AssistantMessage with tool calls, also remove subsequent ToolMessages
            if (removedMessage instanceof AssistantMessage am && am.hasToolCalls()) {
                while (index < memory.size() && memory.get(index) instanceof ToolMessage) {
                    memory.remove(index);
                }
            }
        }
        memory.add(message);
    }

    public List<ChatMessage> getMemory() {
        return memory;
    }
}
