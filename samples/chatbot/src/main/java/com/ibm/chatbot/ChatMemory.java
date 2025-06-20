/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.util.LinkedList;
import java.util.List;
import com.ibm.watsonx.runtime.chat.model.ChatMessage;
import com.ibm.watsonx.runtime.chat.model.SystemMessage;

public class ChatMemory {

  private static final int MAX_SIZE = 10;
  private List<ChatMessage> memory;

  public ChatMemory() {
    memory = new LinkedList<>();
  }

  public void addMessage(ChatMessage message) {
    if (memory.size() >= MAX_SIZE) {
      var index = 0;
      if (memory.get(0) instanceof SystemMessage)
        index = 1;
      memory.remove(index);
    }

    memory.add(message);
  }

  public List<ChatMessage> getMemory() {
    return memory;
  }
}
