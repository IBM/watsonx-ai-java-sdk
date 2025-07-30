/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import java.util.Arrays;
import java.util.List;
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
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public default ChatResponse chat(ChatMessage... messages) {
        return chat(Arrays.asList(messages), null, null);
    }

    /**
     * Sends a chat request to the model using the provided messages.
     *
     * @param messages the list of chat messages representing the conversation history
     * @return a {@link ChatResponse} object containing the model's reply
     */

    public default ChatResponse chat(List<ChatMessage> messages) {
        return chat(messages, null, null);
    }

    /**
     * Sends a chat request to the model using the provided messages and tools.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, include
     * {@link Tool} definitions for function-calling models.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public default ChatResponse chat(List<ChatMessage> messages, List<Tool> tools) {
        return chat(messages, tools, null);
    }

    /**
     * Sends a chat request to the model using the provided messages and tools.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, include
     * {@link Tool} definitions for function-calling models.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public default ChatResponse chat(List<ChatMessage> messages, Tool... tools) {
        return chat(messages, Arrays.asList(tools), null);
    }

    /**
     * Sends a chat request to the model using the provided messages, and parameters.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, and customize
     * the generation behavior via {@link ChatParameters}.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param parameters parameters to customize the output generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public default ChatResponse chat(List<ChatMessage> messages, ChatParameters parameters) {
        return chat(messages, null, parameters);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public default CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, ChatHandler handler) {
        return chatStreaming(messages, null, null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param tools the list of tools that the model may use
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public default CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, List<Tool> tools, ChatHandler handler) {
        return chatStreaming(messages, tools, null, handler);
    }

    /**
     * Sends a streaming chat request using the provided messages.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param parameters additional optional parameters for the chat invocation
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public default CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, ChatParameters parameters, ChatHandler handler) {
        return chatStreaming(messages, null, parameters, handler);
    }

    /**
     * Sends a chat request to the model using the provided messages, tools, and parameters.
     * <p>
     * This method performs a full chat completion call. It allows you to define the conversation history through {@link ChatMessage}s, include
     * {@link Tool} definitions for function-calling models, and customize the generation behavior via {@link ChatParameters}.
     *
     * @param messages the list of chat messages representing the conversation history
     * @param tools list of tools the model may call during generation
     * @param parameters parameters to customize the output generation
     * @return a {@link ChatResponse} object containing the model's reply
     */
    public ChatResponse chat(List<ChatMessage> messages, List<Tool> tools, ChatParameters parameters);

    /**
     * Sends a streaming chat request using the provided messages, tools, and parameters.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param messages the list of chat messages forming the prompt history
     * @param tools the list of tools that the model may use
     * @param parameters additional optional parameters for the chat invocation
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     */
    public CompletableFuture<Void> chatStreaming(List<ChatMessage> messages, List<Tool> tools, ChatParameters parameters, ChatHandler handler);
}
