/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import java.util.List;
import java.util.ServiceLoader;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

/**
 * Utility methods for JSON serialization and deserialization using the configured {@link JsonProvider}.
 */
public final class Json {

    private static final JsonProvider provider = loadProvider();

    private Json() {}

    /**
     * Deserializes a JSON string into an object of the specified class.
     *
     * @param json the JSON content
     * @param clazz the target class
     * @param <T> the type of the resulting object
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return provider.fromJson(json, clazz);
    }

    /**
     * Deserializes a JSON string into an object of the specified generic type.
     *
     * @param json the JSON content
     * @param typeToken the target generic type
     * @param <T> the type of the resulting object
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, TypeToken<T> typeToken) {
        return provider.fromJson(json, typeToken);
    }

    /**
     * Serializes the given object into a JSON string.
     *
     * @param object the object to serialize
     * @return the JSON string
     */
    public static String toJson(Object object) {
        return provider.toJson(object);
    }

    /**
     * Pretty-prints the given object into a JSON string.
     *
     * @param object the object to pretty-print
     * @return a JSON-formatted string representation of the object
     */
    public static String prettyPrint(Object object) {
        return provider.prettyPrint(object);
    }

    /**
     * Validates whether the given string is a valid JSON object.
     *
     * @param json the JSON string to validate
     * @return {@code true} if the string is a valid JSON object, {@code false} otherwise
     */
    public static boolean isValidObject(String json) {
        return provider.isValidObject(json);
    }

    private static JsonProvider loadProvider() {
        var providers = ServiceLoader.load(JsonProvider.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .toList();

        return resolveProvider(providers);
    }

    static JsonProvider resolveProvider(List<JsonProvider> providers) {
        if (providers.isEmpty())
            throw new IllegalStateException(
                "No JsonProvider found. Add exactly one JSON binding, for example watsonx-ai-jackson2 or watsonx-ai-jackson3.");

        var explicit = providers.stream().filter(p -> !p.isDefault()).toList();
        var candidates = explicit.isEmpty() ? providers : explicit;

        if (candidates.size() > 1)
            throw new IllegalStateException("Multiple JsonProvider implementations found: " + candidates + ". Add exactly one JSON binding.");

        return candidates.get(0);
    }
}
