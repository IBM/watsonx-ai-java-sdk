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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters;

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

    @Test
    void should_reject_trailing_content_in_json_schema() {
        for (var schema : List.of("{}{}", "{} trailing", "{}[]", "{} null")) {
            assertThrows(IllegalArgumentException.class, () -> ChatParameters.builder().responseAsJsonSchema(schema), schema);
            assertThrows(IllegalArgumentException.class, () -> ChatParameters.builder().responseAsJsonSchema("schema", schema, true), schema);
        }
    }

    @Test
    void should_preserve_parameter_order_through_builders_and_wire_requests() {
        var bias = new LinkedHashMap<String, Integer>();
        bias.put("second", 2);
        bias.put("first", 1);
        var choices = new LinkedHashSet<>(List.of("second", "first"));
        var parameters = ChatParameters.builder().logitBias(bias).guidedChoice(choices).toolChoice("search").build();
        var gateway = ModelGatewayChatParameters.builder().logitBias(bias).toolChoice("search").build();
        var wire = TextChatRequest.builder().logitBias(parameters.logitBias()).guidedChoice(parameters.guidedChoice())
            .toolChoice(parameters.toolChoice()).build();
        bias.clear();
        choices.clear();

        for (var map : List.of(parameters.logitBias(), gateway.logitBias(), wire.logitBias()))
            assertEquals(List.of("second", "first"), new ArrayList<>(map.keySet()));
        for (var map : List.of(parameters.toolChoice(), gateway.toolChoice(), wire.toolChoice()))
            assertEquals(List.of("type", "function"), new ArrayList<>(map.keySet()));
        assertEquals(List.of("second", "first"), new ArrayList<>(parameters.guidedChoice()));
        assertEquals(List.of("second", "first"), new ArrayList<>(wire.guidedChoice()));
        assertEquals(List.of("second", "first"),
            new ArrayList<>(ChatParameters.builder().guidedChoice("second", "first").build().guidedChoice()));
        assertThrows(IllegalArgumentException.class, () -> ChatParameters.builder().guidedChoice("same", "same"));
        choices.add(null);
        assertThrows(NullPointerException.class, () -> ChatParameters.builder().guidedChoice(choices));
        bias.put("token", null);
        assertThrows(NullPointerException.class, () -> ChatParameters.builder().logitBias(bias));
        bias.clear();
        bias.put(null, 1);
        assertThrows(NullPointerException.class, () -> ModelGatewayChatParameters.builder().logitBias(bias));
    }

}
