/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static java.util.Objects.isNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents grounding hints that provide spatial information about extracted fields.
 * <p>
 * Grounding hints contain bounding box coordinates and page numbers for fields extracted from documents, allowing you to locate where specific
 * information was found in the original document.
 */
public final class GroundingHints {

    /**
     * Represents spatial data for a field, including its bounding box and page number.
     *
     * @param normalizedBbox normalized bounding box coordinates [x1, y1, x2, y2] in range [0, 1]
     * @param pageNumber the page number where the field was found (1-based)
     */
    public static record FieldData(List<Double> normalizedBbox, Integer pageNumber) {
        public FieldData {
            normalizedBbox = isNull(normalizedBbox) ? null : List.copyOf(normalizedBbox);
        }

        public static FieldData of(List<Double> normalizedBbox, Integer pageNumber) {
            return new FieldData(normalizedBbox, pageNumber);
        }
    }

    private final Map<String, FieldData> fieldMap;

    private GroundingHints(Builder builder) {
        this.fieldMap = isNull(builder.fields) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.fields));
    }

    /**
     * Gets the map of field names to their spatial data.
     *
     * @return the map of field spatial data
     */
    public Map<String, FieldData> fields() {
        return fieldMap;
    }

    /**
     * Gets the set of field names that have grounding hints.
     *
     * @return the set of field names
     */
    public Set<String> fieldNames() {
        return fieldMap != null ? fieldMap.keySet() : Set.of();
    }

    /**
     * Checks if grounding hints exist for the specified field.
     *
     * @param fieldName the name of the field to check
     * @return true if grounding hints exist for the field
     */
    public boolean hasField(String fieldName) {
        return fieldMap != null && fieldMap.containsKey(fieldName);
    }

    /**
     * Gets the spatial data for the specified field.
     *
     * @param fieldName the name of the field
     * @return the field spatial data, or null if not found
     */
    public FieldData field(String fieldName) {
        return fieldMap != null ? fieldMap.get(fieldName) : null;
    }

    /**
     * Gets the normalized bounding box for the specified field.
     *
     * @param fieldName the name of the field
     * @return the normalized bounding box coordinates, or null if not found
     */
    public List<Double> bbox(String fieldName) {
        FieldData field = field(fieldName);
        return field != null ? field.normalizedBbox() : null;
    }

    /**
     * Gets the page number where the specified field was found.
     *
     * @param fieldName the name of the field
     * @return the page number (1-based), or null if not found
     */
    public Integer pageNumber(String fieldName) {
        FieldData field = field(fieldName);
        return field != null ? field.pageNumber() : null;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fieldMap == null) ? 0 : fieldMap.hashCode());
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
        GroundingHints other = (GroundingHints) obj;
        if (fieldMap == null) {
            if (other.fieldMap != null)
                return false;
        } else if (!fieldMap.equals(other.fieldMap))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GroundingHints [fields=" + fieldMap + "]";
    }

    /**
     * Builder class for constructing {@link GroundingHints} instances.
     */
    public static final class Builder {
        private Map<String, FieldData> fields = new HashMap<>();

        private Builder() {}

        /**
         * Sets the fields map directly (used by Jackson deserialization).
         *
         * @param fields the map of field names to their spatial data
         * @return this builder instance
         */
        public Builder fields(Map<String, FieldData> fields) {
            this.fields = fields != null ? fields : new HashMap<>();
            return this;
        }

        /**
         * Adds a field with its spatial data.
         *
         * @param fieldName the name of the field
         * @param fieldData the spatial data for the field
         * @return this builder instance
         */
        public Builder add(String fieldName, FieldData fieldData) {
            this.fields.put(fieldName, fieldData);
            return this;
        }

        /**
         * Builds a {@link GroundingHints} instance.
         *
         * @return a new {@link GroundingHints} instance
         */
        public GroundingHints build() {
            return new GroundingHints(this);
        }
    }
}
