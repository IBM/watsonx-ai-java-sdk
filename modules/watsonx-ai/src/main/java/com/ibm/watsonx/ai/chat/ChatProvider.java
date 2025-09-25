/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * Interface representing a provider capable of executing chat interactions with language models.
 *
 * @see ChatService
 * @see DeploymentService
 */
public interface ChatProvider {

    /**
     * Sends a chat request to the model using the provided messages, tools, and parameters.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, include
     * {@link Tool} definitions for function-calling models, and customize the generation behavior via {@link ChatParameters}.
     *
     * @param chatRequest the chat request
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(ChatRequest chatRequest);

    /**
     * Sends a streaming chat request using the provided messages, tools, and parameters.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param chatRequest the chat request
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(ChatRequest chatRequest, ChatHandler handler);
}
