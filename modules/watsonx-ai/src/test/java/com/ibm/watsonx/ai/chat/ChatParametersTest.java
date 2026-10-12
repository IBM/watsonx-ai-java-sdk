/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;

public class ChatParametersTest {

    @Test
    void should_set_response_as_json_schema_from_JsonSchema_convenience_overload() {
        var schema = JsonSchema.object()
            .property("name", JsonSchema.string())
            .build();

        var params = ChatParameters.builder()
            .responseAsJsonSchema(schema)
            .build();

        assertEquals("json_schema", params.responseFormat());
        assertNotNull(params.jsonSchema());
        assertEquals(schema, params.jsonSchema().schema());
        assertTrue(params.jsonSchema().strict());
    }

    @Test
    void should_set_response_as_json_schema_with_name_and_strict_flag() {
        var schema = JsonSchema.object()
            .property("age", JsonSchema.integer())
            .build();

        var params = ChatParameters.builder()
            .responseAsJsonSchema("my-schema", schema, false)
            .build();

        assertEquals("json_schema", params.responseFormat());
        assertEquals("my-schema", params.jsonSchema().name());
        assertEquals(schema, params.jsonSchema().schema());
        assertFalse(params.jsonSchema().strict());
    }

    @Test
    void should_set_response_as_json_schema_from_map() {
        Map<String, Object> schemaMap = Map.of("type", "object");

        var params = ChatParameters.builder()
            .responseAsJsonSchema("map-schema", schemaMap, true)
            .build();

        assertEquals("json_schema", params.responseFormat());
        assertEquals("map-schema", params.jsonSchema().name());
        assertTrue(params.jsonSchema().strict());
    }

    @Test
    void should_set_response_as_json_schema_from_json_string() {
        var params = ChatParameters.builder()
            .responseAsJsonSchema("str-schema", "{\"type\":\"object\",\"properties\":{}}", true)
            .build();

        assertEquals("json_schema", params.responseFormat());
        assertEquals("str-schema", params.jsonSchema().name());
        assertTrue(params.jsonSchema().strict());
        assertInstanceOf(Map.class, params.jsonSchema().schema());
    }

    @Test
    void should_throw_when_json_string_schema_is_not_valid_json() {
        assertThrows(IllegalArgumentException.class, () -> ChatParameters.builder()
            .responseAsJsonSchema("bad", "not-json", true)
            .build());
    }

    @Test
    void should_throw_when_json_string_schema_is_array_not_object() {
        assertThrows(IllegalArgumentException.class, () -> ChatParameters.builder()
            .responseAsJsonSchema("bad", "[\"a\",\"b\"]", true)
            .build());
    }
}
