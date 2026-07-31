/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */


package com.ibm.watsonx.ai.gateway;

import com.ibm.watsonx.ai.chat.BaseChatRequest;
import com.ibm.watsonx.ai.chat.ChatProvider;

/**
 * Extends {@link ChatProvider} with covariant return types for the Model Gateway backend.
 *
 * @see ModelGatewayService
 */
public interface ModelGatewayChatProvider extends ChatProvider {

    /**
     * Sends a chat request to the Model Gateway.
     *
     * @param chatRequest the chat request
     * @return a {@link ModelGatewayChatResponse}
     */
    @Override
    ModelGatewayChatResponse chat(BaseChatRequest chatRequest);
}
