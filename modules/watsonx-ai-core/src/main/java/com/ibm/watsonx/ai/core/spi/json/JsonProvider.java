/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.json;

/**
 * Service Provider Interface (SPI) for JSON serialization and deserialization.
 */
public interface JsonProvider {

    /**
     * Deserializes a JSON string into an instance of the specified class.
     *
     * @param json the JSON content.
     * @param clazz the target class.
     * @param <T> the result type.
     * @return the deserialized object.
     */
    <T> T fromJson(String json, Class<T> clazz);

    /**
     * Deserializes a JSON string into an object of the specified generic type.
     *
     * @param json the JSON content
     * @param typeToken the {@code TypeToken} representing the target generic type
     * @param <T> the type of the resulting object
     * @return the deserialized object
     */
    <T> T fromJson(String json, TypeToken<T> typeToken);

    /**
     * Serializes an object to its JSON representation.
     *
     * @param object the object to serialize.
     * @return the JSON string.
     */
    String toJson(Object object);

    /**
     * Pretty-prints the given object into a JSON string.
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
