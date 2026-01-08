/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import java.util.Map;
import com.ibm.watsonx.ai.core.Json;

/**
 * Provides generic access to arguments from a tool call.
 */
public final class ToolArguments {

    private final Map<String, Object> raw;

    ToolArguments(Map<String, Object> raw) {
        this.raw = raw;
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
    public String toString() {
        return Json.toJson(raw);
    }
}
