/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */


package com.ibm.watsonx.ai.gateway;

import static java.util.Objects.isNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.JsonSchemaObject;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.Prediction;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.Router;
import com.ibm.watsonx.ai.gateway.ModelGatewayParameters.StreamOptions;

/**
 * Payload request for the Model Gateway chat completions endpoint.
 */
public final class GatewayTextChatRequest {

    private final String model;
    private final List<ChatMessage> messages;
    private final List<Tool> tools;
    private final Object toolChoice;
    private final Double frequencyPenalty;
    private final Map<String, Integer> logitBias;
    private final Boolean logprobs;
    private final Integer topLogprobs;
    private final Integer maxCompletionTokens;
    private final Integer maxTokens;
    private final Integer n;
    private final Double presencePenalty;
    private final Integer seed;
    private final List<String> stop;
    private final Double temperature;
    private final Double topP;
    private final Long timeLimit;
    private final Map<String, Object> responseFormat;
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
    private final String user;
    private final Boolean stream;

    private GatewayTextChatRequest(Builder builder) {
        model = builder.model;
        messages = isNull(builder.messages) ? null : List.copyOf(builder.messages);
        tools = isNull(builder.tools) ? null : List.copyOf(builder.tools);
        toolChoice = builder.toolChoice instanceof Map<?, ?> map
            ? Collections.unmodifiableMap(new LinkedHashMap<>(map))
            : builder.toolChoice;
        frequencyPenalty = builder.frequencyPenalty;
        logitBias = isNull(builder.logitBias) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.logitBias));
        logprobs = builder.logprobs;
        topLogprobs = builder.topLogprobs;
        maxCompletionTokens = builder.maxCompletionTokens;
        maxTokens = builder.maxTokens;
        n = builder.n;
        presencePenalty = builder.presencePenalty;
        seed = builder.seed;
        stop = isNull(builder.stop) ? null : List.copyOf(builder.stop);
        temperature = builder.temperature;
        topP = builder.topP;
        timeLimit = builder.timeLimit;
        responseFormat = isNull(builder.responseFormat) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.responseFormat));
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
        user = builder.user;
        stream = builder.stream;
    }

    /**
     * Returns the model identifier.
     *
     * @return the model id
     */
    public String model() {
        return model;
    }

    /**
     * Returns the list of messages comprising the conversation.
     *
     * @return the message list
     */
    public List<ChatMessage> messages() {
        return messages;
    }

    /**
     * Returns the list of tools available to the model.
     *
     * @return the tool list, or {@code null} if none
     */
    public List<Tool> tools() {
        return tools;
    }

    /**
     * Returns the {@code tool_choice} value sent to the gateway.
     * <p>
     * This is a union: either the option string ({@code "auto"}, {@code "none"}, {@code "required"}) or a specific-tool object of the form
     * {@code {"type":"function","function":{"name":"my_function"}}}.
     *
     * @return the tool choice string or object
     */
    public Object toolChoice() {
        return toolChoice;
    }

    /**
     * Returns the frequency penalty applied to reduce token repetition.
     *
     * @return the frequency penalty
     */
    public Double frequencyPenalty() {
        return frequencyPenalty;
    }

    /**
     * Returns the logit bias map applied to specific tokens.
     *
     * @return the logit bias map
     */
    public Map<String, Integer> logitBias() {
        return logitBias;
    }

    /**
     * Returns whether log probabilities are returned for the generated tokens.
     *
     * @return whether log probabilities are enabled
     */
    public Boolean logprobs() {
        return logprobs;
    }

    /**
     * Returns the number of most likely tokens to return at each token position.
     *
     * @return the top logprobs count
     */
    public Integer topLogprobs() {
        return topLogprobs;
    }

    /**
     * Returns the maximum number of tokens that can be generated in the completion.
     *
     * @return the max completion tokens
     */
    public Integer maxCompletionTokens() {
        return maxCompletionTokens;
    }

    /**
     * Returns the legacy max tokens limit.
     *
     * @deprecated use {@link #maxCompletionTokens()} instead
     */
    @Deprecated
    public Integer maxTokens() {
        return maxTokens;
    }

    /**
     * Returns the number of completions to generate for each input message.
     *
     * @return the n value
     */
    public Integer n() {
        return n;
    }

    /**
     * Returns the presence penalty applied to encourage new topic generation.
     *
     * @return the presence penalty
     */
    public Double presencePenalty() {
        return presencePenalty;
    }

    /**
     * Returns the random number generator seed used for deterministic sampling.
     *
     * @return the seed
     */
    public Integer seed() {
        return seed;
    }

    /**
     * Returns the stop sequences that end generation when encountered.
     *
     * @return the stop sequences
     */
    public List<String> stop() {
        return stop;
    }

    /**
     * Returns the sampling temperature.
     *
     * @return the temperature
     */
    public Double temperature() {
        return temperature;
    }

    /**
     * Returns the nucleus sampling top-p threshold.
     *
     * @return the top-p value
     */
    public Double topP() {
        return topP;
    }

    /**
     * Returns the maximum time limit for the request in milliseconds.
     *
     * @return the time limit in milliseconds
     */
    public Long timeLimit() {
        return timeLimit;
    }

    /**
     * Returns the serialized response format map (e.g., {@code {"type":"json_object"}}).
     *
     * @return the response format map
     */
    public Map<String, Object> responseFormat() {
        return responseFormat;
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
     * Returns developer-defined metadata tags used for filtering completions.
     *
     * @return the metadata map
     */
    public Map<String, String> metadata() {
        return metadata;
    }

    /**
     * Returns the requested output modalities (e.g., {@code ["text"]}, {@code ["text","audio"]}).
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
     * Returns the reasoning effort value (e.g., {@code "low"}, {@code "medium"}, {@code "high"}).
     *
     * @return the reasoning effort string
     */
    public String reasoningEffort() {
        return reasoningEffort;
    }

    /**
     * Returns the service tier value for the request.
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
     * Returns the end-user identifier used for abuse monitoring.
     *
     * @return the user identifier
     */
    public String user() {
        return user;
    }

    /**
     * Returns whether the response should be streamed as server-sent events, or {@code null} for a non-streaming request.
     *
     * @return {@code true} for streaming, {@code null} for non-streaming
     */
    public Boolean stream() {
        return stream;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GatewayTextChatRequest} instances.
     */
    public static final class Builder {

        private String model;
        private List<ChatMessage> messages;
        private List<Tool> tools;
        private Object toolChoice;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private Boolean logprobs;
        private Integer topLogprobs;
        private Integer maxCompletionTokens;
        private Integer maxTokens;
        private Integer n;
        private Double presencePenalty;
        private Integer seed;
        private List<String> stop;
        private Double temperature;
        private Double topP;
        private Long timeLimit;
        private Map<String, Object> responseFormat;
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
        private String user;
        private Boolean stream;

        private Builder() {}

        /**
         * Sets the model identifier.
         *
         * @param model the model id
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the list of messages comprising the conversation.
         *
         * @param messages the message list
         */
        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Sets the tools available to the model.
         *
         * @param tools the tool list
         */
        public Builder tools(List<Tool> tools) {
            this.tools = tools;
            return this;
        }

        /**
         * Sets the {@code tool_choice} value.
         * <p>
         * Accepts either the option string ({@code "auto"}, {@code "none"}, {@code "required"}) or a specific-tool object of the form
         * {@code {"type":"function","function":{"name":"my_function"}}}.
         *
         * @param toolChoice the tool choice string or object
         */
        public Builder toolChoice(Object toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        /**
         * Sets the frequency penalty to reduce token repetition.
         *
         * @param frequencyPenalty the frequency penalty
         */
        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        /**
         * Sets the logit bias applied to specific tokens.
         *
         * @param logitBias a map from token ids to bias values
         */
        public Builder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        /**
         * Enables or disables the return of log probabilities for generated tokens.
         *
         * @param logprobs whether to return log probabilities
         */
        public Builder logprobs(Boolean logprobs) {
            this.logprobs = logprobs;
            return this;
        }

        /**
         * Sets the number of most likely tokens to return at each token position.
         *
         * @param topLogprobs the number of top tokens with log probabilities to return
         */
        public Builder topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;
            return this;
        }

        /**
         * Sets the maximum number of tokens that can be generated in the completion.
         *
         * @param maxCompletionTokens the maximum number of tokens
         */
        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        /**
         * Sets the legacy max tokens limit.
         *
         * @deprecated use {@link #maxCompletionTokens(Integer)} instead
         */
        @Deprecated
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * Sets the number of completions to generate for each input message.
         *
         * @param n the number of completions
         */
        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        /**
         * Sets the presence penalty to encourage new topic generation.
         *
         * @param presencePenalty the presence penalty
         */
        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        /**
         * Sets the random number generator seed for deterministic sampling.
         *
         * @param seed the seed value
         */
        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        /**
         * Sets the stop sequences that end generation when encountered.
         *
         * @param stop list of stop sequences
         */
        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        /**
         * Sets the sampling temperature. Higher values produce more random output.
         *
         * @param temperature the sampling temperature
         */
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * Sets the nucleus sampling threshold.
         *
         * @param topP the nucleus sampling threshold
         */
        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        /**
         * Sets the maximum time limit for the request in milliseconds.
         *
         * @param timeLimit the time limit in milliseconds
         */
        public Builder timeLimit(Long timeLimit) {
            this.timeLimit = timeLimit;
            return this;
        }

        /**
         * Sets the serialized response format map.
         *
         * @param responseFormat the response format map
         */
        public Builder responseFormat(Map<String, Object> responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        /**
         * Sets the JSON schema used to validate the model's structured output.
         *
         * @param jsonSchema the JSON schema object
         */
        public Builder jsonSchema(JsonSchemaObject jsonSchema) {
            if (jsonSchema != null) {
                this.responseFormat = new LinkedHashMap<>();
                this.responseFormat.put("type", "json_schema");
                var schemaMap = new LinkedHashMap<String, Object>();
                schemaMap.put("name", jsonSchema.name());
                schemaMap.put("schema", jsonSchema.schema());
                schemaMap.put("strict", jsonSchema.strict());
                this.responseFormat.put("json_schema", schemaMap);
            }
            return this;
        }

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
         * Sets developer-defined metadata tags used for filtering completions.
         *
         * @param metadata the metadata map
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the requested output modalities.
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
         * Sets the reasoning effort constraint value.
         *
         * @param reasoningEffort the reasoning effort string
         */
        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        /**
         * Sets the service tier for the request.
         *
         * @param serviceTier the service tier string
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
         * Sets the end-user identifier for abuse monitoring.
         *
         * @param user the user identifier
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets whether the response should be streamed as server-sent events.
         *
         * @param stream {@code true} for streaming, {@code null} for non-streaming
         */
        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        /**
         * Builds a {@link GatewayTextChatRequest} instance.
         *
         * @return a new {@link GatewayTextChatRequest}
         */
        public GatewayTextChatRequest build() {
            return new GatewayTextChatRequest(this);
        }
    }
}
