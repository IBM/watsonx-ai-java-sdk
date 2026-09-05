/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.jackson2.provider;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.ibm.watsonx.ai.WatsonxJacksonModule;
import com.ibm.watsonx.ai.core.exception.JsonException;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

/**
 * Jackson 2 implementation of {@link JsonProvider}.
 */
public class JacksonProvider implements JsonProvider {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@code JacksonProvider} instance with default configuration.
     */
    public JacksonProvider() {
        this(new WatsonxJacksonModule());
    }

    /**
     * Constructs a {@code JacksonProvider} instance with the specified modules.
     *
     * @param modules modules to register with the mapper
     */
    public JacksonProvider(com.fasterxml.jackson.databind.Module... modules) {
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setDefaultPropertyInclusion(Include.NON_NULL)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModules(modules);
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON: '" + json + "'", e);
        }
    }

    @Override
    public <T> T fromJson(String json, TypeToken<T> type) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(type.getType());
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON: '" + json + "'", e);
        }
    }

    @Override
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public String prettyPrint(Object obj) {
        try {
            return (obj instanceof String str)
                ? objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(str))
                : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
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
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}
