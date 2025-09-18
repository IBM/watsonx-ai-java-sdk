/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.requireNonNull;
import java.util.ServiceLoader;
import com.ibm.watsonx.ai.core.provider.JacksonProvider;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

/**
 * The Json class provides utility methods for JSON serialization and deserialization. It uses a {@link JsonProvider} to handle the actual conversion
 * between JSON and Java objects. The provider can be loaded via {@link ServiceLoader} and falls back to a {@link JacksonProvider} if none is found.
 */
public final class Json {

    private static final JsonProvider provider = loadProvider();

    /**
     * Prevents direct instantiation of the {@code Builder}.
     */
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
        requireNonNull(object);
        return provider.prettyPrint(object);
    }

    /**
     * Attempts to load a {@link JsonProvider} via {@link ServiceLoader}.
     * <p>
     * Falls back to {@link JacksonProvider} if none is found.
     */
    private static JsonProvider loadProvider() {
        return ServiceLoader.load(JsonProvider.class)
            .findFirst().orElse(new JacksonProvider());
    }
}
