/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static java.util.Objects.isNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Container class for different moderation configurations used in a chat request.
 * <p>
 * Supports various moderation types such as Hate and Profanity (HAP), Personally Identifiable Information (PII), and Granite Guardian.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ChatModeration.builder()
 *     .hap(h -> h.input(0.8f).output(0.9f).mask(true))
 *     .pii(p -> p.input(true).output(false).mask(false))
 *     .graniteGuardian(g -> g.input(0.85f).mask(true))
 *     .inputRanges(List.of(InputRanges.of(0, 100)))
 *     .build();
 * }</pre>
 */
public final class ChatModeration {

    private final Hap hap;
    private final Pii pii;
    private final GraniteGuardian graniteGuardian;
    private final List<InputRanges> inputRanges;

    private ChatModeration(Builder builder) {
        hap = builder.hap;
        pii = builder.pii;
        graniteGuardian = builder.graniteGuardian;
        inputRanges = isNull(builder.inputRanges) ? null : List.copyOf(builder.inputRanges);
    }

    /**
     * Returns the Hate and Profanity (HAP) moderation configuration.
     *
     * @return the HAP moderation settings, or {@code null} if not configured
     */
    public Hap hap() {
        return hap;
    }

    /**
     * Returns the Personally Identifiable Information (PII) moderation configuration.
     *
     * @return the PII moderation settings, or {@code null} if not configured
     */
    public Pii pii() {
        return pii;
    }

    /**
     * Returns the Granite Guardian moderation configuration.
     *
     * @return the Granite Guardian moderation settings, or {@code null} if not configured
     */
    public GraniteGuardian graniteGuardian() {
        return graniteGuardian;
    }

    /**
     * Returns the list of input ranges to which moderation should be applied.
     *
     * @return the list of input ranges, or {@code null} if not configured
     */
    public List<InputRanges> inputRanges() {
        return inputRanges;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatModeration} instances with configurable parameters.
     */
    public static final class Builder {

        private Hap hap;
        private Pii pii;
        private GraniteGuardian graniteGuardian;
        private List<InputRanges> inputRanges;

        private Builder() {}

        /**
         * Configures the Hate and Profanity (HAP) moderation via a builder consumer.
         *
         * <pre>{@code
         * .hap(h -> h.input(0.8f).output(0.9f).mask(true))
         * }</pre>
         *
         * @param consumer a consumer that configures the {@link Hap.Builder}
         */
        public Builder hap(Consumer<Hap.Builder> consumer) {
            var b = new Hap.Builder();
            consumer.accept(b);
            this.hap = b.build();
            return this;
        }

        /**
         * Configures the Personally Identifiable Information (PII) moderation via a builder consumer.
         *
         * <pre>{@code
         * .pii(p -> p.input(true).output(false).mask(false))
         * }</pre>
         *
         * @param consumer a consumer that configures the {@link Pii.Builder}
         */
        public Builder pii(Consumer<Pii.Builder> consumer) {
            var b = new Pii.Builder();
            consumer.accept(b);
            this.pii = b.build();
            return this;
        }

        /**
         * Configures the Granite Guardian moderation via a builder consumer.
         *
         * <pre>{@code
         * .graniteGuardian(g -> g.input(0.85f).mask(true))
         * }</pre>
         *
         * @param consumer a consumer that configures the {@link GraniteGuardian.Builder}
         */
        public Builder graniteGuardian(Consumer<GraniteGuardian.Builder> consumer) {
            var b = new GraniteGuardian.Builder();
            consumer.accept(b);
            this.graniteGuardian = b.build();
            return this;
        }

        /**
         * Sets the list of input ranges to which moderation should be applied. Only the specified ranges of the input text will be evaluated.
         *
         * @param inputRanges the list of {@link InputRanges}
         */
        public Builder inputRanges(List<InputRanges> inputRanges) {
            this.inputRanges = inputRanges;
            return this;
        }

        /**
         * Builds a {@link ChatModeration} instance using the configured parameters.
         *
         * @return a new instance of {@link ChatModeration}
         */
        public ChatModeration build() {
            return new ChatModeration(this);
        }
    }

    /**
     * Base class for detector configurations. Holds a map of properties that is serialized as a flat JSON object.
     */
    private static abstract class Detector {

        private final Map<String, Object> properties;

        Detector(DetectorBuilder<?> builder) {
            properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        }

        public Map<String, Object> properties() {
            return properties;
        }

        @Override
        public int hashCode() {
            return properties.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Detector other = (Detector) obj;
            return properties.equals(other.properties);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [properties=" + properties + "]";
        }
    }

    /**
     * Abstract builder for {@link Detector} subclasses.
     */
    @SuppressWarnings("unchecked")
    private static abstract class DetectorBuilder<T extends DetectorBuilder<T>> {

        private final Map<String, Object> properties = new HashMap<>();

        T addProperty(String name, Object value) {
            properties.put(name, value);
            return (T) this;
        }
    }

    /**
     * Hate and Profanity (HAP) moderation configuration.
     */
    public static final class Hap extends Detector {

        private Hap(Builder builder) {
            super(builder);
        }

        /**
         * Builder class for constructing {@link Hap} instances with configurable parameters.
         */
        public static final class Builder extends DetectorBuilder<Builder> {

            private Builder() {}

            /**
             * Enables HAP moderation on the input text with the given threshold.
             *
             * @param threshold the threshold score for triggering moderation
             */
            public Builder input(float threshold) {
                return addProperty("input",
                    Map.of(
                        "enabled", true,
                        "threshold", threshold));
            }

            /**
             * Enables HAP moderation on the output text with the given threshold.
             *
             * @param threshold the threshold score for triggering moderation
             */
            public Builder output(float threshold) {
                return addProperty("output",
                    Map.of(
                        "enabled", true,
                        "threshold", threshold));
            }

            /**
             * Configures masking behavior applied when HAP content is detected.
             *
             * @param removeEntityValue if {@code true}, the detected entity value is removed from the output
             */
            public Builder mask(boolean removeEntityValue) {
                return addProperty("mask", Map.of("remove_entity_value", removeEntityValue));
            }

            /**
             * Builds a {@link Hap} instance using the configured parameters.
             *
             * @return a new instance of {@link Hap}
             */
            Hap build() {
                return new Hap(this);
            }
        }
    }

    /**
     * Personally Identifiable Information (PII) moderation configuration.
     */
    public static final class Pii extends Detector {

        private Pii(Builder builder) {
            super(builder);
        }

        /**
         * Builder class for constructing {@link Pii} instances with configurable parameters.
         */
        public static final class Builder extends DetectorBuilder<Builder> {

            private Builder() {}

            /**
             * Enables or disables PII moderation on the input text.
             *
             * @param enabled whether PII detection on the input text is enabled
             */
            public Builder input(boolean enabled) {
                return addProperty("input", Map.of("enabled", enabled));
            }

            /**
             * Enables or disables PII moderation on the output text.
             *
             * @param enabled whether PII detection on the output text is enabled
             */
            public Builder output(boolean enabled) {
                return addProperty("output", Map.of("enabled", enabled));
            }

            /**
             * Configures masking behavior applied when PII is detected.
             *
             * @param removeEntityValue if {@code true}, the detected entity value is removed from the output
             */
            public Builder mask(boolean removeEntityValue) {
                return addProperty("mask", Map.of("remove_entity_value", removeEntityValue));
            }

            /**
             * Builds a {@link Pii} instance using the configured parameters.
             *
             * @return a new instance of {@link Pii}
             */
            Pii build() {
                return new Pii(this);
            }
        }
    }

    /**
     * Granite Guardian moderation configuration.
     */
    public static final class GraniteGuardian extends Detector {

        private GraniteGuardian(Builder builder) {
            super(builder);
        }

        /**
         * Builder class for constructing {@link GraniteGuardian} instances with configurable parameters.
         */
        public static final class Builder extends DetectorBuilder<Builder> {

            private Builder() {}

            /**
             * Enables Granite Guardian moderation on the input text with the given threshold.
             *
             * @param threshold the threshold score for triggering moderation
             */
            public Builder input(float threshold) {
                return addProperty("input",
                    Map.of(
                        "enabled", true,
                        "threshold", threshold));
            }

            /**
             * Configures masking behavior applied when Granite Guardian detects content.
             *
             * @param removeEntityValue if {@code true}, the detected entity value is removed from the output
             */
            public Builder mask(boolean removeEntityValue) {
                return addProperty("mask", Map.of("remove_entity_value", removeEntityValue));
            }

            /**
             * Builds a {@link GraniteGuardian} instance using the configured parameters.
             *
             * @return a new instance of {@link GraniteGuardian}
             */
            GraniteGuardian build() {
                return new GraniteGuardian(this);
            }
        }
    }

    /**
     * Represents a range within the input text to which moderation is applied. The end index is exclusive.
     *
     * @param start the start index of the range (inclusive), must be &ge; 0
     * @param end the end index of the range (exclusive), must be &ge; 0
     */
    public record InputRanges(Integer start, Integer end) {

        /**
         * Creates a new {@link InputRanges} instance.
         *
         * @param start the start index of the range (inclusive)
         * @param end the end index of the range (exclusive)
         * @return a new {@link InputRanges} instance
         */
        public static InputRanges of(Integer start, Integer end) {
            return new InputRanges(start, end);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hap == null) ? 0 : hap.hashCode());
        result = prime * result + ((pii == null) ? 0 : pii.hashCode());
        result = prime * result + ((graniteGuardian == null) ? 0 : graniteGuardian.hashCode());
        result = prime * result + ((inputRanges == null) ? 0 : inputRanges.hashCode());
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
        ChatModeration other = (ChatModeration) obj;
        if (hap == null) {
            if (other.hap != null)
                return false;
        } else if (!hap.equals(other.hap))
            return false;
        if (pii == null) {
            if (other.pii != null)
                return false;
        } else if (!pii.equals(other.pii))
            return false;
        if (graniteGuardian == null) {
            if (other.graniteGuardian != null)
                return false;
        } else if (!graniteGuardian.equals(other.graniteGuardian))
            return false;
        if (inputRanges == null) {
            if (other.inputRanges != null)
                return false;
        } else if (!inputRanges.equals(other.inputRanges))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ChatModeration [hap=" + hap + ", pii=" + pii + ", graniteGuardian=" + graniteGuardian + ", inputRanges=" + inputRanges + "]";
    }
}
