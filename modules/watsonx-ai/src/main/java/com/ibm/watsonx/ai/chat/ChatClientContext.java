/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import com.ibm.watsonx.ai.chat.interceptor.MessageInterceptor;
import com.ibm.watsonx.ai.chat.interceptor.PartialResponseInterceptor;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;

/**
 * Holds the context data for a chat interaction.
 *
 * @param <R> the concrete chat request type handled by the provider
 */
public class ChatClientContext<R extends BaseChatRequest> {
    private final ChatProvider<R, ?> chatProvider;
    private final R chatRequest;
    private final MessageInterceptor<R> messageInterceptor;
    private final PartialResponseInterceptor<R> partialResponseInterceptor;
    private final ToolInterceptor<R> toolInterceptor;
    private final ExtractionTags extractionTags;

    private ChatClientContext(Builder<R> builder) {
        chatProvider = builder.chatProvider;
        chatRequest = builder.chatRequest;
        messageInterceptor = builder.messageInterceptor;
        partialResponseInterceptor = builder.partialResponseInterceptor;
        toolInterceptor = builder.toolInterceptor;
        extractionTags = builder.extractionTags;
    }

    /**
     * Returns the chat provider.
     *
     * @return the chat provider
     */
    public ChatProvider<R, ?> chatProvider() {
        return chatProvider;
    }

    /**
     * Returns the chat request.
     *
     * @return the chat request
     */
    public R chatRequest() {
        return chatRequest;
    }

    /**
     * Returns the message interceptor.
     *
     * @return the message interceptor
     */
    public MessageInterceptor<R> messageInterceptor() {
        return messageInterceptor;
    }

    /**
     * Returns the partial response interceptor.
     *
     * @return the partial response interceptor
     */
    public PartialResponseInterceptor<R> partialResponseInterceptor() {
        return partialResponseInterceptor;
    }

    /**
     * Returns the tool interceptor.
     *
     * @return the tool interceptor
     */
    public ToolInterceptor<R> toolInterceptor() {
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
     * @param <R> the concrete chat request type handled by the provider
     * @return a new {@link Builder}
     */
    public static <R extends BaseChatRequest> Builder<R> builder() {
        return new Builder<>();
    }

    /**
     * Builder for constructing {@link ChatClientContext} instances.
     *
     * @param <R> the concrete chat request type handled by the provider
     */
    public static class Builder<R extends BaseChatRequest> {
        private ChatProvider<R, ?> chatProvider;
        private R chatRequest;
        private MessageInterceptor<R> messageInterceptor;
        private PartialResponseInterceptor<R> partialResponseInterceptor;
        private ToolInterceptor<R> toolInterceptor;
        private ExtractionTags extractionTags;

        private Builder() {}

        /**
         * Sets the chat provider.
         *
         * @param chatProvider the chat provider
         */
        public Builder<R> chatProvider(ChatProvider<R, ?> chatProvider) {
            this.chatProvider = chatProvider;
            return this;
        }

        /**
         * Sets the chat request.
         *
         * @param chatRequest the chat request
         */
        public Builder<R> chatRequest(R chatRequest) {
            this.chatRequest = chatRequest;
            return this;
        }

        /**
         * Sets the message interceptor.
         *
         * @param messageInterceptor the message interceptor
         */
        public Builder<R> messageInterceptor(MessageInterceptor<R> messageInterceptor) {
            this.messageInterceptor = messageInterceptor;
            return this;
        }

        /**
         * Sets the partial response interceptor.
         *
         * @param partialResponseInterceptor the partial response interceptor
         */
        public Builder<R> partialResponseInterceptor(PartialResponseInterceptor<R> partialResponseInterceptor) {
            this.partialResponseInterceptor = partialResponseInterceptor;
            return this;
        }

        /**
         * Sets the tool interceptor.
         *
         * @param toolInterceptor the tool interceptor
         */
        public Builder<R> toolInterceptor(ToolInterceptor<R> toolInterceptor) {
            this.toolInterceptor = toolInterceptor;
            return this;
        }

        /**
         * Sets the extraction tags.
         *
         * @param extractionTags the extractions tags
         */
        public Builder<R> extractionTags(ExtractionTags extractionTags) {
            this.extractionTags = extractionTags;
            return this;
        }

        /**
         * Builds the {@link ChatClientContext} instance.
         *
         * @return a new {@link ChatClientContext}
         */
        public ChatClientContext<R> build() {
            return new ChatClientContext<>(this);
        }
    }
}
