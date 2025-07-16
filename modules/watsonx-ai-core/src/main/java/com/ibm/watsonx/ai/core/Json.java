/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.requireNonNull;
import java.util.ServiceLoader;
import com.ibm.watsonx.ai.core.spi.json.JsonAdapter;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.core.spi.json.jackson.JacksonAdapter;

/**
 * The Json class provides utility methods for JSON serialization and deserialization. It uses a {@link JsonAdapter} to handle the actual conversion
 * between JSON and Java objects. The adapter can be loaded via {@link ServiceLoader} and falls back to a {@link JacksonAdapter} if none is found.
 */
public class Json {

    private static final JsonAdapter adapter = loadAdapter();

    /**
     * Prevents direct instantiation of the {@code Builder}.
     */
    protected Json() {}

    /**
     * Deserializes a JSON string into an object of the specified class.
     *
     * @param json the JSON content
     * @param clazz the target class
     * @param <T> the type of the resulting object
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return adapter.fromJson(json, clazz);
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
        return adapter.fromJson(json, typeToken);
    }

    /**
     * Serializes the given object into a JSON string.
     *
     * @param object the object to serialize
     * @return the JSON string
     */
    public static String toJson(Object object) {
        return adapter.toJson(object);
    }

    /**
     * Pretty-prints the given object into a JSON string.
     *
     * @param object the object to pretty-print
     * @return a JSON-formatted string representation of the object
     */
    public static String prettyPrint(Object object) {
        requireNonNull(object);
        return adapter.prettyPrint(object);
    }

    /**
     * Attempts to load a {@link JsonAdapter} via {@link ServiceLoader}.
     * <p>
     * Falls back to {@link JacksonAdapter} if none is found.
     */
    private static JsonAdapter loadAdapter() {
        return ServiceLoader.load(JsonAdapter.class)
            .findFirst().orElse(new JacksonAdapter());
    }
}
