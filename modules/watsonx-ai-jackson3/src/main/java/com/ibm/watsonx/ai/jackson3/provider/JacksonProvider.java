/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.jackson3.provider;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.watsonx.ai.WatsonxJacksonModule;
import com.ibm.watsonx.ai.core.exception.JsonException;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 implementation of {@link JsonProvider}.
 */
public class JacksonProvider implements JsonProvider {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@code JacksonProvider} instance with default configuration.
     */
    public JacksonProvider() {
        this.objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(Include.NON_NULL))
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .addModule(new WatsonxJacksonModule())
            .build();
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException e) {
            throw new JsonException("Failed to deserialize JSON: '" + json + "'", e);
        }
    }

    @Override
    public <T> T fromJson(String json, TypeToken<T> type) {
        try {
            JavaType javaType = objectMapper.constructType(type.getType());
            return objectMapper.readValue(json, javaType);
        } catch (JacksonException e) {
            throw new JsonException("Failed to deserialize JSON: '" + json + "'", e);
        }
    }

    @Override
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new JsonException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public String prettyPrint(Object obj) {
        try {
            return (obj instanceof String str)
                ? objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(str))
                : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JacksonException e) {
            return obj.toString();
        }
    }

    @Override
    public boolean isValidObject(String json) {
        if (isNull(json) || json.isBlank())
            return false;

        try {
            JsonNode node = objectMapper.readTree(json);
            return node.isObject();
        } catch (JacksonException e) {
            return false;
        }
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}
