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
 * Abstract class for all detectors.
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

    public String getName() {
        return name;
    }

    public Map<String, Object> getProperties() {
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

        public T threshold(float threshold) {
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
