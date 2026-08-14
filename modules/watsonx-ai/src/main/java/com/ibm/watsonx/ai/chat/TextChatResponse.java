/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toUnmodifiableMap;
import java.util.List;
import java.util.Map;

/**
 * Response returned by the text chat and deployment endpoints.
 */
public class TextChatResponse extends ChatResponse {

    /**
     * Represents a single moderation result detected in the chat response.
     * <p>
     * A moderation result describes a specific match found by the moderation system, including its probability score, whether it was found in the
     * input or output text, its position within the text, the detected entity type, and optionally the matched word.
     *
     * @param score the probability that this is a real match (0.0 to 1.0)
     * @param input {@code true} if this was found in the input text, {@code false} if found in the output
     * @param position the range within the text where the match was found
     * @param entity the entity type identified by the moderation (e.g., {@code "EmailAddress"})
     * @param word the text that was identified for this entity
     */
    public record ModerationResult(float score, boolean input, Position position, String entity, String word) {

        /**
         * Represents a range of text identified by a moderation result. The {@code end} index is exclusive.
         *
         * @param start the start index of the range (inclusive), must be &ge; 0
         * @param end the end index of the range (exclusive), must be &ge; 0
         */
        public record Position(int start, int end) {}
    }

    /**
     * Represents a detection entry associated with a specific choice.
     *
     * @param choiceIndex the index of the choice this detection refers to
     * @param results the list of detection results found for the choice
     */
    public record DetectionEntry(int choiceIndex, List<DetectionResult> results) {
        public DetectionEntry {
            results = isNull(results) ? null : List.copyOf(results);
        }
    }

    /**
     * Represents a single detection result produced by a detector.
     *
     * @param detectorId the identifier of the detector (e.g., {@code "en_syntax_rbr_pii"})
     * @param detectionType the type of detection (e.g., {@code "pii"})
     * @param detection the specific detection label (e.g., {@code "PhoneNumber"})
     * @param score the probability that this is a real match (0.0 to 1.0)
     * @param text the text that was identified
     * @param start the start index of the match (inclusive)
     * @param end the end index of the match (exclusive)
     */
    public record DetectionResult(String detectorId, String detectionType, String detection, double score, String text, int start, int end) {}

    private final String modelId;
    private final String modelVersion;
    private final String createdAt;
    private final Map<String, List<ModerationResult>> moderations;
    private final Map<String, List<DetectionEntry>> detections;

    protected TextChatResponse(Builder<?> builder) {
        super(builder);
        modelId = builder.modelId;
        modelVersion = builder.modelVersion;
        createdAt = builder.createdAt;
        moderations = isNull(builder.moderations) ? null
            : builder.moderations.entrySet().stream()
                .collect(toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
        detections = isNull(builder.detections) ? null
            : builder.detections.entrySet().stream()
                .collect(toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
    }

    /**
     * Returns the id of the model used to generate the response.
     *
     * @return the model id
     */
    public String modelId() {
        return modelId;
    }

    /**
     * Returns the version of the model that generated the response.
     *
     * @return the model version
     */
    public String modelVersion() {
        return modelVersion;
    }

    /**
     * Returns the formatted creation timestamp of the response.
     *
     * @return the formatted creation time
     */
    public String createdAt() {
        return createdAt;
    }

    /**
     * Returns the moderation results detected in the chat response, keyed by detector name.
     *
     * @return a map from detector name to its list of {@link ModerationResult}
     */
    public Map<String, List<ModerationResult>> moderations() {
        return moderations;
    }

    /**
     * Returns the detection results reported in the chat response, keyed by the target position (e.g. {@code "input"}, {@code "output"}).
     *
     * @return a map from target position to its list of {@link DetectionEntry}
     */
    public Map<String, List<DetectionEntry>> detections() {
        return detections;
    }

    /**
     * Creates a builder initialized with the current state of this {@code TextChatResponse}.
     *
     * @return a new {@link Builder} instance pre-populated with this response's data
     */
    @Override
    public Builder<?> toBuilder() {
        return new Builder<>()
            .id(this.id())
            .object(this.object())
            .model(this.model())
            .choices(this.choices())
            .created(this.created())
            .usage(this.usage())
            .extractionTags(this.extractionTags())
            .modelId(this.modelId)
            .modelVersion(this.modelVersion)
            .createdAt(this.createdAt)
            .moderations(this.moderations)
            .detections(this.detections);
    }

    /**
     * Returns a new {@link Builder} instance for {@link TextChatResponse}.
     *
     * @return a new {@link Builder} instance
     */
    public static Builder<?> builder() {
        return new Builder<>();
    }

    /**
     * Builder for constructing {@link TextChatResponse} instances.
     *
     * @param <B> the concrete builder subclass
     */
    @SuppressWarnings("unchecked")
    public static class Builder<B extends Builder<B>> extends ChatResponse.Builder<B> {

        /**
         * Creates a new {@code Builder}.
         */
        public Builder() {}

        private String modelId;
        private String modelVersion;
        private String createdAt;
        private Map<String, List<ModerationResult>> moderations;
        private Map<String, List<DetectionEntry>> detections;

        /**
         * Sets the id of the model used to generate the response.
         *
         * @param modelId the model id
         */
        public B modelId(String modelId) {
            this.modelId = modelId;
            return (B) this;
        }

        /**
         * Sets the version of the model that generated the response.
         *
         * @param modelVersion the model version
         */
        public B modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return (B) this;
        }

        /**
         * Sets the formatted creation timestamp of the response.
         *
         * @param createdAt the formatted creation time
         */
        public B createdAt(String createdAt) {
            this.createdAt = createdAt;
            return (B) this;
        }

        /**
         * Sets the moderation results detected in the chat response, keyed by detector name.
         *
         * @param moderations a map from detector name to its list of {@link ModerationResult}
         */
        public B moderations(Map<String, List<ModerationResult>> moderations) {
            this.moderations = moderations;
            return (B) this;
        }

        /**
         * Sets the detection results reported in the chat response.
         *
         * @param detections a map from target position to its list of {@link DetectionEntry}
         */
        public B detections(Map<String, List<DetectionEntry>> detections) {
            this.detections = detections;
            return (B) this;
        }

        /**
         * Builds a {@link TextChatResponse} instance using the configured parameters.
         *
         * @return a new instance of {@link TextChatResponse}
         */
        @Override
        public TextChatResponse build() {
            return new TextChatResponse(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((modelId == null) ? 0 : modelId.hashCode());
        result = prime * result + ((modelVersion == null) ? 0 : modelVersion.hashCode());
        result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
        result = prime * result + ((moderations == null) ? 0 : moderations.hashCode());
        result = prime * result + ((detections == null) ? 0 : detections.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        TextChatResponse other = (TextChatResponse) obj;
        if (modelId == null) {
            if (other.modelId != null)
                return false;
        } else if (!modelId.equals(other.modelId))
            return false;
        if (modelVersion == null) {
            if (other.modelVersion != null)
                return false;
        } else if (!modelVersion.equals(other.modelVersion))
            return false;
        if (createdAt == null) {
            if (other.createdAt != null)
                return false;
        } else if (!createdAt.equals(other.createdAt))
            return false;
        if (moderations == null) {
            if (other.moderations != null)
                return false;
        } else if (!moderations.equals(other.moderations))
            return false;
        if (detections == null) {
            if (other.detections != null)
                return false;
        } else if (!detections.equals(other.detections))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TextChatResponse [id=" + id() + ", object=" + object() + ", modelId=" + modelId + ", model=" + model() + ", choices=" + choices()
            + ", created=" + created() + ", modelVersion=" + modelVersion + ", createdAt=" + createdAt + ", usage=" + usage() + ", extractionTags="
            + extractionTags() + ", moderations=" + moderations + ", detections=" + detections + "]";
    }
}
