/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import java.util.Map;

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
     * @return the value cast to {@code T}, or {@code null} if the key is not present
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) raw.get(key);
    }

    Map<String, Object> getRaw() {
        return raw;
    }
}
