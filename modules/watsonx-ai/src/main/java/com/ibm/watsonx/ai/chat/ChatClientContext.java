/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;

/**
 * Holds the context data for a chat interaction.
 */
public class ChatClientContext {
    private final ChatProvider chatProvider;
    private final ChatRequest chatRequest;
    private final ToolInterceptor toolInterceptor;
    private final ExtractionTags extractionTags;

    private ChatClientContext(Builder builder) {
        chatProvider = builder.chatProvider;
        chatRequest = builder.chatRequest;
        toolInterceptor = builder.toolInterceptor;
        extractionTags = builder.extractionTags;
    }

    /**
     * Returns the chat provider.
     *
     * @return the chat provider
     */
    public ChatProvider chatProvider() {
        return chatProvider;
    }

    /**
     * Returns chat request.
     *
     * @return the chat request
     */
    public ChatRequest chatRequest() {
        return chatRequest;
    }

    /**
     * Returns the tool interceptor.
     *
     * @return the tool interceptor
     */
    public ToolInterceptor toolInterceptor() {
        return toolInterceptor;
    }

    /**
     * Returns the extraction tags.
     *
     * @return the extraction tags
     */
    public ExtractionTags extractionTags() {
        return extractionTags;
    }

    /**
     * Creates a new builder instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ChatClientContext} instances.
     */
    public static class Builder {
        private ChatProvider chatProvider;
        private ChatRequest chatRequest;
        private ToolInterceptor toolInterceptor;
        private ExtractionTags extractionTags;

        private Builder() {}

        /**
         * Sets the chat provider.
         *
         * @param chatProvider the chat provider
         */
        public Builder chatProvider(ChatProvider chatProvider) {
            this.chatProvider = chatProvider;
            return this;
        }

        /**
         * Sets the chat request.
         *
         * @param chatRequest the chat request
         */
        public Builder chatRequest(ChatRequest chatRequest) {
            this.chatRequest = chatRequest;
            return this;
        }

        /**
         * Sets the tool interceptor.
         *
         * @param toolInterceptor the tool interceptor
         */
        public Builder toolInterceptor(ToolInterceptor toolInterceptor) {
            this.toolInterceptor = toolInterceptor;
            return this;
        }

        /**
         * Sets the extraction tags.
         *
         * @param extractionTags the extractions tags
         */
        public Builder extractionTags(ExtractionTags extractionTags) {
            this.extractionTags = extractionTags;
            return this;
        }

        /**
         * Builds the {@link ChatClientContext} instance.
         *
         * @return a new {@link ChatClientContext}
         */
        public ChatClientContext build() {
            return new ChatClientContext(this);
        }
    }
}
