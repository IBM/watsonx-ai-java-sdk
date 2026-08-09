/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters;

/**
 * Parameters specific to the Model Gateway chat endpoint.
 * <p>
 * Extends {@link BaseChatParameters} with gateway-specific fields such as {@code reasoningEffort}, {@code serviceTier}, {@code router},
 * {@code prediction}, {@code modalities}, {@code audio}, and others.
 * <p>
 * Use the builder to construct instances. All collections are immutable and defensively copied.
 * <p>
 * <b>Example usage</b>:
 *
 * <pre>{@code
 * ModelGatewayChatParameters params = ModelGatewayChatParameters.builder()
 *     .temperature(0.7)
 *     .maxCompletionTokens(2048)
 *     .reasoningEffort(ReasoningEffort.MEDIUM)
 *     .serviceTier(ServiceTier.PRIORITY)
 *     .build();
 * }</pre>
 *
 * @see BaseChatParameters
 * @see ModelGatewayChatService
 */
public final class ModelGatewayChatParameters extends BaseChatParameters {

    /**
     * Constrains reasoning effort for reasoning models.
     */
    public enum ReasoningEffort {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high");

        private final String value;

        ReasoningEffort(String value) {
            this.value = value;
        }

        /**
         * Returns the wire string value.
         *
         * @return the wire value
         */
        public String value() {
            return value;
        }

        /**
         * Returns the {@code ReasoningEffort} matching the given wire value.
         *
         * @param value the wire string value (for example {@code "low"})
         * @return the matching {@code ReasoningEffort}
         * @throws IllegalArgumentException if no effort matches the given value
         */
        public static ReasoningEffort fromValue(String value) {
            for (var effort : values()) {
                if (effort.value.equals(value))
                    return effort;
            }
            throw new IllegalArgumentException("Unknown reasoning effort: " + value);
        }
    }

    /**
     * Specifies the service tier for the request.
     */
    public enum ServiceTier {
        AUTO("auto"),
        DEFAULT("default"),
        FLEX("flex"),
        PRIORITY("priority");

        private final String value;

        ServiceTier(String value) {
            this.value = value;
        }

        /**
         * Returns the wire string value.
         *
         * @return the wire value
         */
        public String value() {
            return value;
        }
    }

    /**
     * Configuration for a Predicted Output, which can speed up responses when large parts are known ahead of time.
     *
     * @param type the prediction type - must be {@code "content"}
     * @param content the content to match during generation
     */
    public record Prediction(String type, Object content) {}

    /**
     * Options for streaming responses. Only set when {@code stream} is {@code true}.
     *
     * @param includeUsage whether to include usage statistics in the streaming response
     */
    public record StreamOptions(Boolean includeUsage) {}

    /**
     * Cache configuration for a request. Only supported for non-streaming requests.
     *
     * @param enabled whether caching is enabled for this request
     * @param filter optional filter criteria for caching
     * @param threshold the similarity threshold for cache hit; required when caching is enabled
     */
    public record Cache(boolean enabled, Object filter, Double threshold) {}

    /**
     * Model routing configuration for the request.
     *
     * @param cache cache settings for this request
     */
    public record Router(Cache cache) {}

    private final Map<String, String> audio;
    private final Map<String, String> metadata;
    private final List<String> modalities;
    private final Boolean parallelToolCalls;
    private final Prediction prediction;
    private final String reasoningEffort;
    private final String serviceTier;
    private final Boolean store;
    private final StreamOptions streamOptions;
    private final Router router;
    private final Integer maxTokens;
    private final String user;

    private ModelGatewayChatParameters(Builder builder) {
        super(builder);
        audio = isNull(builder.audio) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.audio));
        metadata = isNull(builder.metadata) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
        modalities = isNull(builder.modalities) ? null : List.copyOf(builder.modalities);
        parallelToolCalls = builder.parallelToolCalls;
        prediction = builder.prediction;
        reasoningEffort = builder.reasoningEffort;
        serviceTier = builder.serviceTier;
        store = builder.store;
        streamOptions = builder.streamOptions;
        router = builder.router;
        maxTokens = builder.maxTokens;
        user = builder.user;
    }

    /**
     * Returns the audio output parameters.
     *
     * @return the audio parameters map
     */
    public Map<String, String> audio() {
        return audio;
    }

    /**
     * Returns developer-defined tags and values used for filtering completions.
     *
     * @return the metadata map
     */
    public Map<String, String> metadata() {
        return metadata;
    }

    /**
     * Returns the output modalities requested.
     *
     * @return the modalities list
     */
    public List<String> modalities() {
        return modalities;
    }

    /**
     * Returns whether parallel function calling during tool use is enabled.
     *
     * @return {@code true} if parallel tool calls are enabled, {@code false} if disabled
     */
    public Boolean parallelToolCalls() {
        return parallelToolCalls;
    }

    /**
     * Returns the predicted output configuration.
     *
     * @return the predicted output configuration
     */
    public Prediction prediction() {
        return prediction;
    }

    /**
     * Returns the reasoning effort constraint string value.
     *
     * @return the reasoning effort string
     */
    public String reasoningEffort() {
        return reasoningEffort;
    }

    /**
     * Returns the service tier string value.
     *
     * @return the service tier string
     */
    public String serviceTier() {
        return serviceTier;
    }

    /**
     * Returns whether the output should be stored for model distillation or evals.
     *
     * @return {@code true} if storage is enabled
     */
    public Boolean store() {
        return store;
    }

    /**
     * Returns the streaming options.
     *
     * @return the streaming options
     */
    public StreamOptions streamOptions() {
        return streamOptions;
    }

    /**
     * Returns the router configuration.
     *
     * @return the router configuration
     */
    public Router router() {
        return router;
    }

    /**
     * Returns the legacy maximum number of tokens. Deprecated in favor of {@link #maxCompletionTokens()}.
     *
     * @return the legacy max tokens, or {@code null} if not set
     * @deprecated use {@link #maxCompletionTokens()} instead
     */
    @Deprecated
    public Integer maxTokens() {
        return maxTokens;
    }

    /**
     * Returns the end-user identifier for abuse monitoring.
     *
     * @return the user identifier
     */
    public String user() {
        return user;
    }

    /**
     * Returns a new {@link Builder} instance pre-populated with this object's values.
     *
     * @return a new {@link Builder}
     */
    public Builder toBuilder() {
        var b = new Builder()
            .modelId(modelId)
            .transactionId(transactionId)
            .frequencyPenalty(frequencyPenalty)
            .logitBias(logitBias)
            .logprobs(logprobs)
            .topLogprobs(topLogprobs)
            .maxCompletionTokens(maxCompletionTokens)
            .n(n)
            .presencePenalty(presencePenalty)
            .seed(seed)
            .stop(stop)
            .temperature(temperature)
            .topP(topP)
            .audio(audio)
            .metadata(metadata)
            .modalities(modalities)
            .parallelToolCalls(parallelToolCalls)
            .prediction(prediction)
            .serviceTier(serviceTier)
            .store(store)
            .streamOptions(streamOptions)
            .router(router)
            .maxTokens(maxTokens)
            .user(user);

        if (timeLimit != null)
            b.timeLimit(java.time.Duration.ofMillis(timeLimit));

        if (toolChoiceOption != null)
            b.toolChoiceOption(BaseChatParameters.ToolChoiceOption.fromValue(toolChoiceOption));

        if (reasoningEffort != null)
            b.reasoningEffort(ReasoningEffort.fromValue(reasoningEffort));

        if (nonNull(responseFormat)) {
            switch(BaseChatParameters.ResponseFormat.from(responseFormat)) {
                case TEXT -> b.responseAsText();
                case JSON -> b.responseAsJson();
                case JSON_SCHEMA -> {
                    if (nonNull(jsonSchema))
                        b.responseAsJsonSchema(jsonSchema.name(), jsonSchema.schema(), jsonSchema.strict());
                }
            }
        }

        if (nonNull(toolChoice)) {
            var function = toolChoice.get("function");
            if (function instanceof Map<?, ?> functionMap && nonNull(functionMap.get("name")))
                b.toolChoice(String.valueOf(functionMap.get("name")));
        }

        return b;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * ModelGatewayChatParameters params = ModelGatewayChatParameters.builder()
     *     .temperature(0.7)
     *     .maxCompletionTokens(2048)
     *     .reasoningEffort(ReasoningEffort.MEDIUM)
     *     .serviceTier(ServiceTier.PRIORITY)
     *     .build();
     * }</pre>
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ModelGatewayChatParameters} instances.
     */
    public static final class Builder extends BaseChatParameters.Builder<Builder> {

        private Map<String, String> audio;
        private Map<String, String> metadata;
        private List<String> modalities;
        private Boolean parallelToolCalls;
        private Prediction prediction;
        private String reasoningEffort;
        private String serviceTier;
        private Boolean store;
        private StreamOptions streamOptions;
        private Router router;
        private Integer maxTokens;
        private String user;

        private Builder() {}

        /**
         * Sets the audio output parameters.
         *
         * @param audio the audio parameters map
         */
        public Builder audio(Map<String, String> audio) {
            this.audio = audio;
            return this;
        }

        /**
         * Sets developer-defined tags used for filtering completions.
         *
         * @param metadata the metadata map
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the requested output modalities (e.g., {@code ["text"]}, {@code ["text","audio"]}).
         *
         * @param modalities the modalities list
         */
        public Builder modalities(List<String> modalities) {
            this.modalities = modalities;
            return this;
        }

        /**
         * Enables or disables parallel function calling during tool use.
         *
         * @param parallelToolCalls {@code true} to enable, {@code false} to disable
         */
        public Builder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        /**
         * Sets the predicted output configuration.
         *
         * @param prediction the prediction configuration
         */
        public Builder prediction(Prediction prediction) {
            this.prediction = prediction;
            return this;
        }

        /**
         * Sets the reasoning effort constraint.
         *
         * @param reasoningEffort the {@link ReasoningEffort} value
         */
        public Builder reasoningEffort(ReasoningEffort reasoningEffort) {
            this.reasoningEffort = reasoningEffort == null ? null : reasoningEffort.value();
            return this;
        }

        /**
         * Sets the reasoning effort constraint.
         *
         * @param reasoningEffort the reasoning effort value
         */
        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        /**
         * Sets the service tier using the typed enum.
         *
         * @param serviceTier the {@link ServiceTier} value
         */
        public Builder serviceTier(ServiceTier serviceTier) {
            this.serviceTier = serviceTier == null ? null : serviceTier.value();
            return this;
        }

        /**
         * Sets the service tier by raw string value.
         *
         * @param serviceTier the service tier string (e.g., {@code "auto"})
         */
        public Builder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        /**
         * Sets whether the output should be stored for model distillation or evals.
         *
         * @param store {@code true} to store the output
         */
        public Builder store(Boolean store) {
            this.store = store;
            return this;
        }

        /**
         * Sets the streaming options. Only used when {@code stream} is {@code true}.
         *
         * @param streamOptions the streaming options
         */
        public Builder streamOptions(StreamOptions streamOptions) {
            this.streamOptions = streamOptions;
            return this;
        }

        /**
         * Sets the router configuration.
         *
         * @param router the router configuration
         */
        public Builder router(Router router) {
            this.router = router;
            return this;
        }

        /**
         * Sets the legacy maximum tokens limit.
         *
         * @param maxTokens the maximum number of tokens
         * @return this builder instance
         * @deprecated use {@link #maxCompletionTokens(Integer)} instead
         */
        @Deprecated
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * Sets the end-user identifier for abuse monitoring.
         *
         * @param user the user identifier
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Builds a {@link ModelGatewayChatParameters} instance.
         *
         * @return a new {@link ModelGatewayChatParameters}
         */
        public ModelGatewayChatParameters build() {
            return new ModelGatewayChatParameters(this);
        }
    }
}
