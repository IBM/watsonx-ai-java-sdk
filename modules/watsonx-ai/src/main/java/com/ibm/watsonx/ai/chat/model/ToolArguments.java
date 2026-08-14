/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static java.util.Objects.isNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ibm.watsonx.ai.core.Json;

/**
 * Provides generic access to arguments from a tool call.
 */
public final class ToolArguments {

    private final Map<String, Object> raw;

    ToolArguments(Map<String, Object> raw) {
        this.raw = isNull(raw) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(raw));
    }

    /**
     * Returns the value associated with the specified key.
     *
     * @param <T> the expected type
     * @param key the argument name
     * @return the value cast to {@code T}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) raw.get(key);
    }

    /**
     * Checks if the argument map contains a value for the specified key.
     *
     * @param key the argument name to check
     * @return {@code true} if the key is present in the argument map, {@code false} otherwise
     */
    public boolean contains(String key) {
        return raw.containsKey(key);
    }

    /*
     * This method is needed by Jackson for serializing the object.
     */
    Map<String, Object> getRaw() {
        return raw;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((raw == null) ? 0 : raw.hashCode());
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
        ToolArguments other = (ToolArguments) obj;
        if (raw == null) {
            if (other.raw != null)
                return false;
        } else if (!raw.equals(other.raw))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Json.toJson(raw);
    }
}
