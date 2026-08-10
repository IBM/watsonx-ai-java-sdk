/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.deployment.DeploymentChatRequest;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatRequest;

/**
 * Abstract base holding the chat request fields shared by every {@link ChatProvider}.
 *
 * @see ChatRequest
 * @see DeploymentChatRequest
 * @see ModelGatewayChatRequest
 */
public abstract class BaseChatRequest {
    protected final List<ChatMessage> messages;
    protected final List<Tool> tools;

    protected <T extends Builder<T>> BaseChatRequest(Builder<T> builder) {
        messages = List.copyOf(requireNonNull(builder.messages, "messages cannot be null"));
        tools = nonNull(builder.tools) ? List.copyOf(builder.tools) : null;
    }

    /**
     * Returns the list of chat messages.
     *
     * @return the list of messages
     */
    public List<ChatMessage> messages() {
        return messages;
    }

    /**
     * Returns the list of tools available to the model.
     *
     * @return the list of tools, or {@code null} if not set
     */
    public List<Tool> tools() {
        return tools;
    }

    /**
     * Returns the chat parameters.
     *
     * @return the chat parameters, or {@code null} if not set
     */
    public abstract BaseChatParameters parameters();

    /**
     * Abstract builder holding the fields shared by every chat request.
     *
     * @param <T> the concrete builder subclass
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends Builder<T>> {
        protected List<ChatMessage> messages;
        protected List<Tool> tools;

        protected Builder() {}

        /**
         * Sets the conversation messages for the request, replacing any existing messages.
         * <p>
         * This method completely overwrites the current list of messages with the provided ones.
         * <p>
         * Use {@link #addMessages(ChatMessage...)} or {@link #addMessages(List)} to append messages instead.
         *
         * @param messages one or more {@link ChatMessage} objects to set
         */
        public T messages(ChatMessage... messages) {
            return messages(Arrays.asList(messages));
        }

        /**
         * Sets the conversation messages for the request, replacing any existing messages.
         * <p>
         * This method completely overwrites the current list of messages with the provided ones.
         * <p>
         * Use {@link #addMessages(ChatMessage...)} or {@link #addMessages(List)} to append messages instead.
         *
         * @param messages one or more {@link ChatMessage} objects to set
         */
        public T messages(List<? extends ChatMessage> messages) {
            this.messages = isNull(messages) ? null : new ArrayList<>(messages);
            return (T) this;
        }

        /**
         * Adds one or more messages to the existing list of messages for the chat request.
         * <p>
         * Unlike {@link #messages(ChatMessage...)}, which replaces the current list of messages, this method appends the provided messages to the
         * existing list.
         *
         * @param messages one or more {@link ChatMessage} objects to add
         */
        public T addMessages(ChatMessage... messages) {
            return addMessages(Arrays.asList(messages));
        }

        /**
         * Adds one or more messages to the existing list of messages for the chat request.
         * <p>
         * Unlike {@link #messages(ChatMessage...)}, which replaces the current list of messages, this method appends the provided messages to the
         * existing list.
         *
         * @param messages one or more {@link ChatMessage} objects to add
         */
        public T addMessages(List<? extends ChatMessage> messages) {
            if (isNull(messages) || messages.isEmpty())
                return (T) this;

            this.messages = requireNonNullElse(this.messages, new ArrayList<>());
            this.messages.addAll(messages);
            return (T) this;
        }

        /**
         * Sets the tools available for invocation by the model.
         *
         * @param executableTools list of {@link ExecutableTool} objects
         */
        public T tools(ExecutableTool... executableTools) {
            return tools(Arrays.stream(executableTools).map(ExecutableTool::schema).toList());
        }

        /**
         * Sets the tools available for invocation by the model.
         *
         * @param tools list of {@link Tool} objects
         */
        public T tools(Tool... tools) {
            return tools(List.of(tools));
        }

        /**
         * Sets the tools available for invocation by the model.
         *
         * @param tools list of {@link Tool} objects
         */
        public T tools(List<Tool> tools) {
            this.tools = isNull(tools) ? null : List.copyOf(tools);
            return (T) this;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((messages == null) ? 0 : messages.hashCode());
        result = prime * result + ((tools == null) ? 0 : tools.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BaseChatRequest other = (BaseChatRequest) obj;
        if (messages == null) {
            if (other.messages != null)
                return false;
        } else if (!messages.equals(other.messages))
            return false;
        if (tools == null) {
            if (other.tools != null)
                return false;
        } else if (!tools.equals(other.tools))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BaseChatRequest [messages=" + messages + ", tools=" + tools + "]";
    }
}
