/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.client.impl;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatRestClient;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;

public class CustomChatRestClient extends ChatRestClient {

    CustomChatRestClient(Builder builder) {
        super(builder);
    }

    @Override
    public ChatResponse chat(String transactionId, TextChatRequest textChatRequest) {
        throw new UnsupportedOperationException("Unimplemented method 'chat'");
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(String transactionId, ExtractionTags extractionTags, TextChatRequest textChatRequest,
        ChatHandler handler) {
        throw new UnsupportedOperationException("Unimplemented method 'chatStreaming'");
    }

    public static final class CustomChatRestClientBuilderFactory implements ChatRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new CustomChatRestClient.Builder();
        }
    }

    static final class Builder extends ChatRestClient.Builder {
        @Override
        public ChatRestClient build() {
            return new CustomChatRestClient(this);
        }
    }
}
