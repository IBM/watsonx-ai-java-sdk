/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.Thinking;
import com.ibm.watsonx.ai.chat.model.ThinkingEffort;
import com.ibm.watsonx.ai.deployment.DeploymentChatRequest;

/**
 * Abstract base for requests sent to {@code ChatService} and {@code DeploymentService}
 *
 * @see ChatRequest
 * @see DeploymentChatRequest
 */
public abstract class NativeChatRequest extends BaseChatRequest {
    protected final ChatParameters parameters;
    protected final Thinking thinking;

    protected <T extends Builder<T>> NativeChatRequest(Builder<T> builder) {
        super(builder);
        parameters = builder.parameters;
        thinking = builder.thinking;
    }

    /**
     * Returns the chat parameters.
     *
     * @return the chat parameters, or {@code null} if not set
     */
    @Override
    public ChatParameters parameters() {
        return parameters;
    }

    /**
     * Returns the thinking configuration.
     *
     * @return the thinking configuration, or {@code null} if not set
     */
    public Thinking thinking() {
        return thinking;
    }

    /**
     * Abstract builder holding the fields shared chat requests.
     *
     * @param <T> the concrete builder subclass
     */
    public abstract static class Builder<T extends Builder<T>> extends BaseChatRequest.Builder<T> {
        protected ChatParameters parameters;
        protected Thinking thinking;

        protected Builder() {}

        /**
         * Sets the parameters controlling the chat model's behavior.
         *
         * @see ChatParameters
         * @param parameters a {@link ChatParameters}.
         */
        @SuppressWarnings("unchecked")
        public T parameters(ChatParameters parameters) {
            this.parameters = parameters;
            return (T) this;
        }

        /**
         * Enables or disables reasoning for the chat request.
         * <p>
         * This method provides a simple way to toggle reasoning behavior without specifying any particular configuration. When {@code true},
         * reasoning is enabled using the default {@link Thinking} settings. When {@code false}, reasoning is disabled entirely.
         *
         * @param enabled {@code true} to enable reasoning with default settings, {@code false} to disable reasoning
         */
        public T thinking(boolean enabled) {
            return thinking(Thinking.builder().enabled(enabled).build());
        }

        /**
         * Sets the reasoning extraction tags for the chat request.
         * <p>
         * This method is intended for models that return reasoning and response content within the same text string. The provided
         * {@link ExtractionTags} define which XML-like tags (for example, {@code <think>} and {@code <response>}) should be used to automatically
         * extract the reasoning and response segments.
         *
         * <p>
         * Equivalent to calling:
         *
         * <pre>{@code
         * builder.thinking(Thinking.of(tags));
         * }</pre>
         *
         * @param tags an {@link ExtractionTags} instance defining the reasoning and response tags
         */
        public T thinking(ExtractionTags tags) {
            if (isNull(tags))
                return thinking((Thinking) null);

            return thinking(Thinking.of(tags));
        }

        /**
         * Sets the reasoning effort for the chat request.
         * <p>
         * The provided {@link ThinkingEffort} controls how much reasoning the model applies when generating a response. This method should be used
         * with models that already separate reasoning and response automatically.
         *
         * <p>
         * Equivalent to calling:
         *
         * <pre>{@code
         * builder.thinking(Thinking.of(ThinkingEffort));
         * }</pre>
         *
         * @param thinkingEffort the desired {@link ThinkingEffort} level
         */
        public T thinking(ThinkingEffort thinkingEffort) {
            if (isNull(thinkingEffort))
                return thinking((Thinking) null);

            return thinking(Thinking.of(thinkingEffort));
        }

        /**
         * Sets the reasoning configuration for the chat request.
         * <p>
         * The provided {@link Thinking} instance defines how the LLM should handle reasoning output.
         * <p>
         * If the {@link Thinking} instance includes {@link ExtractionTags}, they will be used to automatically extract reasoning and response
         * segments from models that return both parts within a single text string (for example, models in the <b>ibm/granite-3-3-8b-instruct</b>).
         * <p>
         * If {@link ExtractionTags} are omitted, the model is assumed to already provide reasoning and response as separate fields.
         *
         * @param thinking a {@link Thinking} configuration defining how reasoning output is extracted and the level of reasoning effort
         */
        @SuppressWarnings("unchecked")
        public T thinking(Thinking thinking) {
            this.thinking = thinking;
            return (T) this;
        }
    }
}
