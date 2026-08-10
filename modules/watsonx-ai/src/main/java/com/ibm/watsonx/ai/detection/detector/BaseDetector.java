/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.detection.detector;

import static java.util.Objects.requireNonNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a configurable content detector used by the detection service.
 * <p>
 * This is an abstract base class, one of its concrete implementations should be used instead:
 * <ul>
 * <li>{@link Pii} - detects personally identifiable information (PII)</li>
 * <li>{@link Hap} - detects hate or profanity content</li>
 * <li>{@link GraniteGuardian} - performs general content moderation</li>
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

    /**
     * Creates a new detector from the given builder.
     *
     * @param builder the builder holding the detector configuration
     */
    protected BaseDetector(Builder<?> builder) {
        name = requireNonNull(builder.name, "name cannot be null");
        properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
    }

    /**
     * Returns the name of the detector.
     *
     * @return the detector name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the detector configuration properties.
     *
     * @return a map of property names to their values
     */
    public Map<String, Object> properties() {
        return properties;
    }

    @Override
    public String toString() {
        return "BaseDetector [name=" + name + ", properties=" + properties + "]";
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

        /**
         * Sets the detection threshold.
         * <p>
         * The threshold determines the minimum confidence score required for a detection to be reported.
         *
         * @param threshold the threshold value
         */
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
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
        BaseDetector other = (BaseDetector) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }
}
