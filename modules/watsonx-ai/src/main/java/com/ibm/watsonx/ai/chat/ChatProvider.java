/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.deployment.DeploymentService;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatService;

/**
 * Interface representing a provider capable of executing chat interactions with language models.
 *
 * @param <R> the concrete chat request type accepted by this provider
 * @param <C> the concrete chat response type returned by this provider
 * @see ChatService
 * @see DeploymentService
 * @see ModelGatewayChatService
 */
public interface ChatProvider<R extends BaseChatRequest, C extends ChatResponse> {

    /**
     * Sends a chat request.
     *
     * @param chatRequest the chat request object
     * @return the provider-specific {@link ChatResponse} containing the model's reply
     */
    public C chat(R chatRequest);

    /**
     * Sends a streaming chat request.
     * <p>
     * This method initiates an asynchronous chat operation where partial responses are delivered incrementally through the provided
     * {@link ChatHandler}.
     *
     * @param chatRequest the chat request object
     * @param handler a {@link ChatHandler} implementation that receives partial responses, the complete response, and error notifications
     * @return a {@link CompletableFuture} that completes with the final {@link ChatResponse}
     */
    public CompletableFuture<ChatResponse> chatStreaming(R chatRequest, ChatHandler handler);
}
