/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.json;

import com.ibm.watsonx.ai.core.exception.JsonException;

/**
 * Service Provider Interface (SPI) for JSON serialization and deserialization.
 * <p>
 * Unless otherwise stated, implementations must signal serialization and deserialization failures (malformed JSON, unmappable types, etc.) by
 * throwing a {@link JsonException}. They must never throw checked exceptions or return {@code null} to indicate failure.
 */
public interface JsonProvider {

    /**
     * Deserializes a JSON string into an instance of the specified class.
     *
     * @param json the JSON content.
     * @param clazz the target class.
     * @param <T> the result type.
     * @return the deserialized object.
     * @throws JsonException if the content cannot be deserialized into the target class
     */
    <T> T fromJson(String json, Class<T> clazz);

    /**
     * Deserializes a JSON string into an object of the specified generic type.
     *
     * @param json the JSON content
     * @param typeToken the {@code TypeToken} representing the target generic type
     * @param <T> the type of the resulting object
     * @return the deserialized object
     * @throws JsonException if the content cannot be deserialized into the target type
     */
    <T> T fromJson(String json, TypeToken<T> typeToken);

    /**
     * Serializes an object to its JSON representation.
     *
     * @param object the object to serialize.
     * @return the JSON string.
     * @throws JsonException if the object cannot be serialized
     */
    String toJson(Object object);

    /**
     * Pretty-prints the given object into a JSON string.
     * <p>
     * This is a best-effort, presentation-oriented method: implementations should fall back to a plain representation of the object rather than
     * throwing if pretty-printing fails.
     *
     * @param object the object to pretty-print
     * @return a JSON-formatted string representation of the object
     */
    String prettyPrint(Object object);

    /**
     * Validates whether the given string is a valid JSON object.
     *
     * @param json the JSON string to validate
     * @return {@code true} if the string is a valid JSON object, {@code false} otherwise
     */
    boolean isValidObject(String json);
}
