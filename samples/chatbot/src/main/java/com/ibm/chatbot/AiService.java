/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.net.URI;
import java.time.Duration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.runtime.chat.ChatService;
import com.ibm.watsonx.runtime.chat.model.AssistantMessage;
import com.ibm.watsonx.runtime.chat.model.ChatParameters;
import com.ibm.watsonx.runtime.chat.model.SystemMessage;
import com.ibm.watsonx.runtime.chat.model.UserMessage;

public class AiService {

  private static final Config config = ConfigProvider.getConfig();
  private final ChatService chatService;
  private final ChatMemory memory;

  public AiService() {

    final var url = URI.create(config.getValue("WATSONX_URL", String.class));
    final var apiKey = config.getValue("WATSONX_API_KEY", String.class);
    final var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

    AuthenticationProvider authProvider = IAMAuthenticator.builder()
      .apiKey(apiKey)
      .timeout(Duration.ofSeconds(60))
      .build();

    chatService = ChatService.builder()
      .authenticationProvider(authProvider)
      .projectId(projectId)
      .modelId("meta-llama/llama-4-maverick-17b-128e-instruct-fp8")
      .url(url)
      .build();

    memory = new ChatMemory();
    memory.addMessage(SystemMessage.of("You are an helpful assistant"));
  }

  public String chat(String message) {
    memory.addMessage(UserMessage.text(message));

    var parameters = ChatParameters.builder()
      .maxCompletionTokens(0)
      .build();
      
    var response = chatService.chat(memory.getMemory(), parameters).toText();
    memory.addMessage(AssistantMessage.text(response));
    return response;
  }
}
