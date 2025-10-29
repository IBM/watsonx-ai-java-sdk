/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Represents the reasoning configuration used by the LLM.
 * <p>
 * A {@code Thinking} instance defines how the LLM should handle reasoning output — specifying both how reasoning and response segments are extracted
 * and the level of reasoning effort to apply during generation.
 * <p>
 * <b>Note on {@link #extractionTags}:</b>
 * <ul>
 * <li>If {@code extractionTags} is provided, it is used to automatically extract the reasoning and response segments from the LLM's output. This is
 * necessary for models that return both reasoning and response within a single text string. (e.g. the <b>ibm/granite-3-3-8b-instruct</b>)</li>
 * <li>If {@code extractionTags} is {@code null}, the LLM is assumed to provide reasoning and response as separate fields. (e.g. the
 * <b>openai/gpt-oss-120b</b>)</li>
 * </ul>
 *
 * <p>
 * <b>Usage examples:</b>
 *
 * <pre>{@code
 *
 * // Using reasoning with a model like openai/gpt-oss-120b
 * Thinking.of(ThinkingEffort.HIGH);
 *
 * // Using reasoning with a model like ibm/granite-3-3-8b-instruct
 * Thinking.of(ExtractionTags.of("think", "response"));
 * }</pre>
 *
 * @see ExtractionTags
 * @see ThinkingEffort
 */
public final class Thinking {

    private final Boolean enabled;
    private final Boolean includeReasoning;
    private final ExtractionTags extractionTags;
    private final ThinkingEffort thinkingEffort;

    private Thinking(Builder builder) {
        enabled = builder.enabled;
        includeReasoning = builder.includeReasoning;
        extractionTags = builder.extractionTags;
        thinkingEffort = builder.thinkingEffort;
    }

    public Boolean getEnabled() {
        if (isNull(enabled) && (nonNull(includeReasoning) || nonNull(extractionTags) || nonNull(thinkingEffort)))
            return true;

        return enabled;
    }

    public Boolean getIncludeReasoning() {
        return includeReasoning;
    }

    public ExtractionTags getExtractionTags() {
        return extractionTags;
    }

    public ThinkingEffort getThinkingEffort() {
        return thinkingEffort;
    }

    public static Thinking of(ExtractionTags extractionTags) {
        return builder().extractionTags(extractionTags).build();
    }

    public static Thinking of(ThinkingEffort thinkingEffort) {
        return builder().thinkingEffort(thinkingEffort).build();
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    public final static class Builder {
        private Boolean enabled;
        private Boolean includeReasoning;
        private ExtractionTags extractionTags;
        private ThinkingEffort thinkingEffort;

        private Builder() {}


        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets whether reasoning should be included in the LLM response.
         * <p>
         * When {@code true}, the assistant’s reasoning process will be included in the model output. When {@code false}, the reasoning content will
         * be omitted.
         * <p>
         *
         * @param includeReasoning {@code true} to include reasoning in the model output, {@code false} to exclude it
         */
        public Builder includeReasoning(Boolean includeReasoning) {
            this.includeReasoning = includeReasoning;
            return this;
        }

        /**
         * Sets the extraction tags used to separate reasoning and response segments from the LLM’s output.
         * <p>
         * The provided {@link ExtractionTags} define the XML-like tags (for example, {@code <think>} and {@code <response>}) used to automatically
         * extract the reasoning and response parts from the generated text.
         * <p>
         * This configuration is required for models that return both reasoning and response within a single text string (for example, models in the
         * <b>ibm/granite-3-3-8b-instruct</b>).
         *
         * @param extractionTags an {@link ExtractionTags} instance defining the reasoning and response tags
         */
        public Builder extractionTags(ExtractionTags extractionTags) {
            this.extractionTags = extractionTags;
            return this;
        }

        /**
         * Sets the reasoning effort level to control how much reasoning the model applies when generating responses.
         * <p>
         * The {@link ThinkingEffort} parameter allows fine-tuning of the model’s internal reasoning intensity — for example, {@code LOW},
         * {@code MEDIUM}, or {@code HIGH}.
         *
         * @param thinkingEffort the desired {@link ThinkingEffort} level
         */
        public Builder thinkingEffort(ThinkingEffort thinkingEffort) {
            this.thinkingEffort = thinkingEffort;
            return this;
        }

        /**
         * Builds a {@link Thinking} instance using the configured parameters.
         *
         * @return a new instance of {@link Thinking}
         */
        public Thinking build() {
            return new Thinking(this);
        }
    }
}
