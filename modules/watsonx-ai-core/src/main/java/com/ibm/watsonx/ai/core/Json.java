/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.List;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import com.ibm.watsonx.ai.core.exception.JsonException;
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

    /**
     * Validates an object or a JSON string encoding an object. Exactly one encoding layer is accepted, and the entire input must be consumed. Arrays,
     * scalars, JSON null, malformed input and absent or blank input return {@code false}.
     *
     * @param json the JSON content to validate
     * @return {@code true} if the content represents a JSON object directly or through one JSON string
     */
    public static boolean isValidObjectOrEncodedObject(String json) {
        return nonNull(normalizeObjectOrNull(json));
    }

    /**
     * Returns object JSON text from an object or a JSON string encoding an object. Exactly one encoding layer is accepted, and the entire input must
     * be consumed. Direct object text is returned unchanged. Encoded input returns the decoded text, preserving its whitespace and property order.
     * This method validates JSON syntax without validating an object schema.
     *
     * @param json the JSON object or JSON string encoding an object
     * @return the validated object JSON text
     * @throws JsonException if the input is absent, blank or does not represent exactly one JSON object
     */
    public static String normalizeObject(String json) {
        var object = normalizeObjectOrNull(json);
        if (isNull(object))
            throw new JsonException("Expected exactly one JSON object or a JSON string encoding one");
        return object;
    }

    private static String normalizeObjectOrNull(String json) {

        if (isNull(json))
            return null;

        int start = 0;

        while (start < json.length() && isJsonWhitespace(json.charAt(start)))
            start++;

        if (start == json.length())
            return null;

        if (json.charAt(start) != '"')
            return isValidObject(json) ? json : null;

        int end = start + 1;
        for (; end < json.length(); end++) {
            if (json.charAt(end) == '\\')
                end++;
            else if (json.charAt(end) == '"')
                break;
        }

        if (end >= json.length() || !json.substring(end + 1).chars().allMatch(Json::isJsonWhitespace))
            return null;

        try {
            var object = fromJson(json, String.class);
            return isValidObject(object) ? object : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isJsonWhitespace(int character) {
        return character == ' ' || character == '\t' || character == '\r' || character == '\n';
    }

    private static JsonProvider loadProvider() {
        return loadProvider(Thread.currentThread().getContextClassLoader(), Json.class.getClassLoader());
    }

    static JsonProvider loadProvider(ClassLoader primary, ClassLoader fallback) {
        var providers = findProviders(primary);

        if (providers.isEmpty() && fallback != primary)
            providers = findProviders(fallback);

        return resolveProvider(providers);
    }

    private static List<JsonProvider> findProviders(ClassLoader classLoader) {
        return ServiceLoader.load(JsonProvider.class, classLoader)
            .stream()
            .map(ServiceLoader.Provider::get)
            .toList();
    }

    static JsonProvider resolveProvider(List<JsonProvider> providers) {
        if (providers.isEmpty())
            throw new IllegalStateException(
                "No JsonProvider found. Add exactly one JSON binding dependency to your classpath: watsonx-ai-jackson2 or watsonx-ai-jackson3. "
                    + "If using a custom provider, register it via META-INF/services/com.ibm.watsonx.ai.core.spi.json.JsonProvider.");

        var explicit = providers.stream().filter(p -> !p.isDefault()).toList();
        var candidates = explicit.isEmpty() ? providers : explicit;

        if (candidates.size() > 1) {
            var names = new StringJoiner(", ");
            for (var candidate : candidates)
                names.add(candidate.getClass().getName());
            throw new IllegalStateException(
                "Multiple JsonProvider implementations found: " + names + ". Keep exactly one and exclude the others from the classpath");
        }

        return candidates.get(0);
    }
}
