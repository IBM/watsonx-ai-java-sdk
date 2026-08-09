/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatClientContext;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatRequest;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatResponse;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatRestClient;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayTextChatRequest;

public class CustomModelGatewayChatRestClient extends ModelGatewayChatRestClient {

    CustomModelGatewayChatRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public ModelGatewayChatResponse chat(String transactionId, Duration timeout, ModelGatewayTextChatRequest gatewayRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'chat'");
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(String transactionId, Duration timeout, ModelGatewayTextChatRequest gatewayRequest,
        ChatClientContext<ModelGatewayChatRequest> context, ChatHandler handler) {
        throw new UnsupportedOperationException("Unimplemented method 'chatStreaming'");
    }

    public static final class CustomModelGatewayChatRestClientBuilderFactory implements ModelGatewayChatRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomModelGatewayChatRestClient.Builder();
        }
    }

    static final class Builder extends ModelGatewayChatRestClient.Builder {
        @Override
        public ModelGatewayChatRestClient build() {
            return new CustomModelGatewayChatRestClient(this);
        }
    }
}
