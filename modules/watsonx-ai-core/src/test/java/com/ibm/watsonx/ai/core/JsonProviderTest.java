/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import java.lang.reflect.Method;
import com.ibm.watsonx.ai.core.exception.JsonException;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

public class JsonProviderTest implements JsonProvider {

    private static final Object MAPPER;

    static {
        try {
            Class<?> includeClass = Class.forName("com.fasterxml.jackson.annotation.JsonInclude$Include");
            Object nonNull = includeClass.getField("NON_NULL").get(null);

            Class<?> stratClass = Class.forName("com.fasterxml.jackson.databind.PropertyNamingStrategies");
            Object snakeCase = stratClass.getField("SNAKE_CASE").get(null);

            Class<?> featureClass = Class.forName("com.fasterxml.jackson.databind.DeserializationFeature");
            Object failOnUnknown = featureClass.getField("FAIL_ON_UNKNOWN_PROPERTIES").get(null);

            Class<?> mapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Object mapper = mapperClass.getDeclaredConstructor().newInstance();

            Method configure = mapperClass.getMethod("configure", featureClass, boolean.class);
            configure.invoke(mapper, failOnUnknown, false);

            Method setInclusion = mapperClass.getMethod("setDefaultPropertyInclusion",
                Class.forName("com.fasterxml.jackson.annotation.JsonInclude$Include"));
            setInclusion.invoke(mapper, nonNull);

            Method setNaming = mapperClass.getMethod("setPropertyNamingStrategy",
                Class.forName("com.fasterxml.jackson.databind.PropertyNamingStrategy"));
            setNaming.invoke(mapper, snakeCase);

            MAPPER = mapper;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            Method readValue = MAPPER.getClass().getMethod("readValue", String.class, Class.class);
            return type.cast(readValue.invoke(MAPPER, json, type));
        } catch (Exception e) {
            throw new JsonException("Failed to deserialize JSON: '" + json + "'", e);
        }
    }

    @Override
    public <T> T fromJson(String json, TypeToken<T> typeToken) {
        try {
            Class<?> mapperClass = MAPPER.getClass();
            Method getTypeFactory = mapperClass.getMethod("getTypeFactory");
            Object typeFactory = getTypeFactory.invoke(MAPPER);
            Method constructType = typeFactory.getClass().getMethod("constructType", java.lang.reflect.Type.class);
            Object javaType = constructType.invoke(typeFactory, typeToken.getType());
            Class<?> javaTypeClass = Class.forName("com.fasterxml.jackson.databind.JavaType");
            Method readValue = mapperClass.getMethod("readValue", String.class, javaTypeClass);
            @SuppressWarnings("unchecked")
            T result = (T) readValue.invoke(MAPPER, json, javaType);
            return result;
        } catch (Exception e) {
            throw new JsonException("Failed to deserialize JSON: '" + json + "'", e);
        }
    }

    @Override
    public String toJson(Object obj) {
        try {
            Method writeValueAsString = MAPPER.getClass().getMethod("writeValueAsString", Object.class);
            return (String) writeValueAsString.invoke(MAPPER, obj);
        } catch (Exception e) {
            throw new JsonException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public String prettyPrint(Object obj) {
        try {
            Class<?> mapperClass = MAPPER.getClass();
            Method writerMethod = mapperClass.getMethod("writerWithDefaultPrettyPrinter");
            Object writer = writerMethod.invoke(MAPPER);
            if (obj instanceof String str) {
                Method readTree = mapperClass.getMethod("readTree", String.class);
                Object tree = readTree.invoke(MAPPER, str);
                Method writeStr = writer.getClass().getMethod("writeValueAsString", Object.class);
                return (String) writeStr.invoke(writer, tree);
            }
            Method writeStr = writer.getClass().getMethod("writeValueAsString", Object.class);
            return (String) writeStr.invoke(writer, obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    @Override
    public boolean isValidObject(String json) {
        if (json == null || json.isBlank())
            return false;
        try {
            Class<?> mapperClass = MAPPER.getClass();
            Method readTree = mapperClass.getMethod("readTree", String.class);
            Object node = readTree.invoke(MAPPER, json);
            Method isObject = node.getClass().getMethod("isObject");
            return (Boolean) isObject.invoke(node);
        } catch (Exception e) {
            return false;
        }
    }
}
