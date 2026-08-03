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
import com.ibm.watsonx.ai.gateway.ModelGatewayChatRequest;
import com.ibm.watsonx.ai.gateway.ModelGatewayChatResponse;
import com.ibm.watsonx.ai.gateway.ModelGatewayRestClient;
import com.ibm.watsonx.ai.gateway.ModelGatewayTextChatRequest;

public class CustomModelGatewayRestClient extends ModelGatewayRestClient {

    CustomModelGatewayRestClient(Builder builder) {
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

    public static final class CustomModelGatewayRestClientBuilderFactory implements ModelGatewayRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomModelGatewayRestClient.Builder();
        }
    }

    static final class Builder extends ModelGatewayRestClient.Builder {
        @Override
        public ModelGatewayRestClient build() {
            return new CustomModelGatewayRestClient(this);
        }
    }
}
