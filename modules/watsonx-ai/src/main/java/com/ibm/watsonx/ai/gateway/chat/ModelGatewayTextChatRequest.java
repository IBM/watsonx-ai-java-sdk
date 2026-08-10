/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.chat;

import static java.util.Objects.isNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.JsonSchemaObject;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.Prediction;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.Router;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters.StreamOptions;

/**
 * Payload request for the Model Gateway chat completions endpoint.
 */
public final class ModelGatewayTextChatRequest {

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

    private ModelGatewayTextChatRequest(Builder builder) {
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
     * Builder for constructing {@link ModelGatewayTextChatRequest} instances.
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
         * Builds a {@link ModelGatewayTextChatRequest} instance.
         *
         * @return a new {@link ModelGatewayTextChatRequest}
         */
        public ModelGatewayTextChatRequest build() {
            return new ModelGatewayTextChatRequest(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + ((messages == null) ? 0 : messages.hashCode());
        result = prime * result + ((tools == null) ? 0 : tools.hashCode());
        result = prime * result + ((toolChoice == null) ? 0 : toolChoice.hashCode());
        result = prime * result + ((frequencyPenalty == null) ? 0 : frequencyPenalty.hashCode());
        result = prime * result + ((logitBias == null) ? 0 : logitBias.hashCode());
        result = prime * result + ((logprobs == null) ? 0 : logprobs.hashCode());
        result = prime * result + ((topLogprobs == null) ? 0 : topLogprobs.hashCode());
        result = prime * result + ((maxCompletionTokens == null) ? 0 : maxCompletionTokens.hashCode());
        result = prime * result + ((maxTokens == null) ? 0 : maxTokens.hashCode());
        result = prime * result + ((n == null) ? 0 : n.hashCode());
        result = prime * result + ((presencePenalty == null) ? 0 : presencePenalty.hashCode());
        result = prime * result + ((seed == null) ? 0 : seed.hashCode());
        result = prime * result + ((stop == null) ? 0 : stop.hashCode());
        result = prime * result + ((temperature == null) ? 0 : temperature.hashCode());
        result = prime * result + ((topP == null) ? 0 : topP.hashCode());
        result = prime * result + ((timeLimit == null) ? 0 : timeLimit.hashCode());
        result = prime * result + ((responseFormat == null) ? 0 : responseFormat.hashCode());
        result = prime * result + ((audio == null) ? 0 : audio.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((modalities == null) ? 0 : modalities.hashCode());
        result = prime * result + ((parallelToolCalls == null) ? 0 : parallelToolCalls.hashCode());
        result = prime * result + ((prediction == null) ? 0 : prediction.hashCode());
        result = prime * result + ((reasoningEffort == null) ? 0 : reasoningEffort.hashCode());
        result = prime * result + ((serviceTier == null) ? 0 : serviceTier.hashCode());
        result = prime * result + ((store == null) ? 0 : store.hashCode());
        result = prime * result + ((streamOptions == null) ? 0 : streamOptions.hashCode());
        result = prime * result + ((router == null) ? 0 : router.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((stream == null) ? 0 : stream.hashCode());
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
        ModelGatewayTextChatRequest other = (ModelGatewayTextChatRequest) obj;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
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
        if (toolChoice == null) {
            if (other.toolChoice != null)
                return false;
        } else if (!toolChoice.equals(other.toolChoice))
            return false;
        if (frequencyPenalty == null) {
            if (other.frequencyPenalty != null)
                return false;
        } else if (!frequencyPenalty.equals(other.frequencyPenalty))
            return false;
        if (logitBias == null) {
            if (other.logitBias != null)
                return false;
        } else if (!logitBias.equals(other.logitBias))
            return false;
        if (logprobs == null) {
            if (other.logprobs != null)
                return false;
        } else if (!logprobs.equals(other.logprobs))
            return false;
        if (topLogprobs == null) {
            if (other.topLogprobs != null)
                return false;
        } else if (!topLogprobs.equals(other.topLogprobs))
            return false;
        if (maxCompletionTokens == null) {
            if (other.maxCompletionTokens != null)
                return false;
        } else if (!maxCompletionTokens.equals(other.maxCompletionTokens))
            return false;
        if (maxTokens == null) {
            if (other.maxTokens != null)
                return false;
        } else if (!maxTokens.equals(other.maxTokens))
            return false;
        if (n == null) {
            if (other.n != null)
                return false;
        } else if (!n.equals(other.n))
            return false;
        if (presencePenalty == null) {
            if (other.presencePenalty != null)
                return false;
        } else if (!presencePenalty.equals(other.presencePenalty))
            return false;
        if (seed == null) {
            if (other.seed != null)
                return false;
        } else if (!seed.equals(other.seed))
            return false;
        if (stop == null) {
            if (other.stop != null)
                return false;
        } else if (!stop.equals(other.stop))
            return false;
        if (temperature == null) {
            if (other.temperature != null)
                return false;
        } else if (!temperature.equals(other.temperature))
            return false;
        if (topP == null) {
            if (other.topP != null)
                return false;
        } else if (!topP.equals(other.topP))
            return false;
        if (timeLimit == null) {
            if (other.timeLimit != null)
                return false;
        } else if (!timeLimit.equals(other.timeLimit))
            return false;
        if (responseFormat == null) {
            if (other.responseFormat != null)
                return false;
        } else if (!responseFormat.equals(other.responseFormat))
            return false;
        if (audio == null) {
            if (other.audio != null)
                return false;
        } else if (!audio.equals(other.audio))
            return false;
        if (metadata == null) {
            if (other.metadata != null)
                return false;
        } else if (!metadata.equals(other.metadata))
            return false;
        if (modalities == null) {
            if (other.modalities != null)
                return false;
        } else if (!modalities.equals(other.modalities))
            return false;
        if (parallelToolCalls == null) {
            if (other.parallelToolCalls != null)
                return false;
        } else if (!parallelToolCalls.equals(other.parallelToolCalls))
            return false;
        if (prediction == null) {
            if (other.prediction != null)
                return false;
        } else if (!prediction.equals(other.prediction))
            return false;
        if (reasoningEffort == null) {
            if (other.reasoningEffort != null)
                return false;
        } else if (!reasoningEffort.equals(other.reasoningEffort))
            return false;
        if (serviceTier == null) {
            if (other.serviceTier != null)
                return false;
        } else if (!serviceTier.equals(other.serviceTier))
            return false;
        if (store == null) {
            if (other.store != null)
                return false;
        } else if (!store.equals(other.store))
            return false;
        if (streamOptions == null) {
            if (other.streamOptions != null)
                return false;
        } else if (!streamOptions.equals(other.streamOptions))
            return false;
        if (router == null) {
            if (other.router != null)
                return false;
        } else if (!router.equals(other.router))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (stream == null) {
            if (other.stream != null)
                return false;
        } else if (!stream.equals(other.stream))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ModelGatewayTextChatRequest [model=" + model + ", messages=" + messages + ", tools=" + tools + ", toolChoice=" + toolChoice
            + ", frequencyPenalty=" + frequencyPenalty + ", logitBias=" + logitBias + ", logprobs=" + logprobs + ", topLogprobs=" + topLogprobs
            + ", maxCompletionTokens=" + maxCompletionTokens + ", maxTokens=" + maxTokens + ", n=" + n + ", presencePenalty=" + presencePenalty
            + ", seed=" + seed + ", stop=" + stop + ", temperature=" + temperature + ", topP=" + topP + ", timeLimit=" + timeLimit
            + ", responseFormat=" + responseFormat + ", audio=" + audio + ", metadata=" + metadata + ", modalities=" + modalities
            + ", parallelToolCalls=" + parallelToolCalls + ", prediction=" + prediction + ", reasoningEffort=" + reasoningEffort + ", serviceTier="
            + serviceTier + ", store=" + store + ", streamOptions=" + streamOptions + ", router=" + router + ", user=" + user + ", stream=" + stream
            + "]";
    }
}
