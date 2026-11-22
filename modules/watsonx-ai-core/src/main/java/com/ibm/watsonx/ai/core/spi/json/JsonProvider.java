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
     * Pretty-prints a string only when it contains exactly one JSON object. Arrays, scalars, JSON null, malformed input, additional content and
     * absent or blank input are returned unchanged. This is a best-effort, presentation-oriented operation.
     *
     * @param json the JSON content to pretty-print
     * @return the formatted JSON object, or the original input when it is not exactly one JSON object
     */
    default String prettyPrintObject(String json) {
        return isValidObject(json) ? prettyPrint(json) : json;
    }

    /**
     * Validates whether the entire string contains exactly one JSON object. Leading and trailing whitespace are allowed, but additional JSON values
     * or other non-whitespace content must be rejected. Arrays, scalars, JSON null and malformed input return {@code false}.
     *
     * @param json the JSON string to validate
     * @return {@code true} if the string is a valid JSON object, {@code false} otherwise
     */
    boolean isValidObject(String json);

    /**
     * Indicates whether this is a bundled default implementation rather than an explicit choice.
     *
     * @return {@code true} if this is a default implementation, {@code false} otherwise
     */
    default boolean isDefault() {
        return false;
    }
}
