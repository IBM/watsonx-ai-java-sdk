/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a configurable content detector used by the detection service.
 * <p>
 * This is an abstract base class, one of its concrete implementations should be used instead:
 * <ul>
 * <li>{@link Pii} — detects personally identifiable information (PII)</li>
 * <li>{@link Hap} — detects hate or profanity content</li>
 * <li>{@link GraniteGuardian} — performs general content moderation</li>
 * </ul>
 * <p>
 * Each detector exposes a builder for fluent configuration of parameters.
 * <p>
 * Detectors are used in a {@code DetectionService} request to specify which kinds of content analysis to perform.
 *
 * @see Pii
 * @see Hap
 * @see GraniteGuardian
 */
public abstract class BaseDetector {
    private final String name;
    private final Map<String, Object> properties;

    protected BaseDetector(Builder<?> builder) {
        name = requireNonNull(builder.name, "name cannot be null");
        properties = requireNonNullElse(builder.properties, new HashMap<>());
    }

    public String name() {
        return name;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    /**
     * Abstract builder class for constructing a {@link BaseDetector}.
     */
    @SuppressWarnings("unchecked")
    static abstract class Builder<T> {
        private String name;
        private Map<String, Object> properties;

        /**
         * Creates a builder with the given detector name.
         *
         * @param name the name of the detector
         */
        protected Builder(String name) {
            this.name = name;
            properties = new HashMap<>();
        }

        public T threshold(Double threshold) {
            return addProperty("threshold", threshold);
        }

        /**
         * Adds a property to the detector configuration.
         *
         * @param name the property name
         * @param value the property value
         */
        public T addProperty(String name, Object value) {
            properties.put(name, value);
            return (T) this;
        }
    }
}
