/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.exception.JsonException;
import com.ibm.watsonx.ai.core.spi.json.JsonProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

public class JsonTest {

    record Person(String name, String lastname) {};
    record User(String name) {};

    record StubJsonProvider(boolean isDefault) implements JsonProvider {

        @Override
        public <T> T fromJson(String json, Class<T> clazz) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T fromJson(String json, TypeToken<T> typeToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toJson(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String prettyPrint(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValidObject(String json) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void resolve_provider_should_throw_when_no_provider_found() {
        assertThrows(IllegalStateException.class, () -> Json.resolveProvider(List.of()));
    }

    @Test
    void resolve_provider_should_return_the_only_default_provider() {
        var provider = new StubJsonProvider(true);
        assertEquals(provider, Json.resolveProvider(List.of(provider)));
    }

    @Test
    void resolve_provider_should_return_the_only_explicit_provider() {
        var provider = new StubJsonProvider(false);
        assertEquals(provider, Json.resolveProvider(List.of(provider)));
    }

    @Test
    void resolve_provider_should_prefer_the_explicit_provider_over_defaults() {
        var explicit = new StubJsonProvider(false);
        assertEquals(explicit, Json.resolveProvider(List.of(new StubJsonProvider(true), explicit)));
    }

    @Test
    void resolve_provider_should_throw_when_multiple_defaults_and_no_explicit() {
        assertThrows(IllegalStateException.class,
            () -> Json.resolveProvider(List.of(new StubJsonProvider(true), new StubJsonProvider(true))));
    }

    @Test
    void resolve_provider_should_throw_when_multiple_explicit_providers() {
        assertThrows(IllegalStateException.class,
            () -> Json.resolveProvider(List.of(new StubJsonProvider(false), new StubJsonProvider(false))));
    }

    @Test
    void should_serialize_object_to_json_string() {
        var json = Json.toJson(new Person("Alan", "Wake"));
        assertEquals("{\"name\":\"Alan\",\"lastname\":\"Wake\"}", json);
    }

    @Test
    void should_deserialize_json_string_to_object() {
        var person = Json.fromJson("{\"name\":\"Alan\",\"lastname\":\"Wake\"}", Person.class);
        assertEquals(new Person("Alan", "Wake"), person);
    }

    @Test
    void should_pretty_print_json_string() {
        var json = Json.prettyPrint("{\"name\":\"Alan\",\"lastname\":\"Wake\"}");
        assertEquals("""
            {
              "name" : "Alan",
              "lastname" : "Wake"
            }""", json);
    }

    @Test
    void should_throw_exception_when_json_is_invalid() {
        var ex = assertThrows(RuntimeException.class, () -> Json.fromJson("{", String.class));
        assertEquals("Failed to deserialize JSON: '{'", ex.getMessage());
        ex = assertThrows(RuntimeException.class, () -> Json.fromJson("{", new TypeToken<Map<String, Object>>() {}));
        assertEquals("Failed to deserialize JSON: '{'", ex.getMessage());
    }

    @Test
    void should_return_false_when_json_is_not_a_object() {
        var json = "\"{\n  \"a@a.it\",\n  \"subject\": \"what time is it\",\n  \"body\": \"2025-12-25T10:24:30.896544949\"\n}\"";
        assertFalse(Json.isValidObject(json));
        assertFalse(Json.isValidObject(null));
        assertFalse(Json.isValidObject(""));
    }

    @Test
    void should_return_true_when_json_is_a_object() {
        var json = "{ \"name\": \"Alan\"}";
        assertTrue(Json.isValidObject(json));
    }

    @Test
    void should_deserialize_json_array_to_list_using_type_token() {
        var json = "[{ \"name\": \"Alan\"}]";
        var result = Json.fromJson(json, TypeToken.listOf(User.class));
        assertTrue(result.size() == 1);
        assertEquals("Alan", result.get(0).name());

        json = "[ \"Alan\", \"Wake\"]";
        var result2 = Json.fromJson(json, TypeToken.listOf(String.class));
        assertTrue(result2.size() == 2);
        assertEquals("Alan", result2.get(0));
        assertEquals("Wake", result2.get(1));
    }

    @Test
    void should_deserialize_json_object_to_map_using_parameterized_type_token() {
        var json = "{\"a\":1,\"b\":2}";
        Map<String, Integer> result = Json.fromJson(json, TypeToken.parameterizedOf(Map.class, String.class, Integer.class));
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    void type_tokens_for_different_types_should_not_be_equal() {
        assertNotEquals(TypeToken.listOf(String.class), TypeToken.listOf(Integer.class));
        assertNotEquals(
            TypeToken.parameterizedOf(Map.class, String.class, Integer.class),
            TypeToken.parameterizedOf(Map.class, String.class, String.class));
    }

    @Test
    void parameterized_of_should_validate_arguments() {
        assertThrows(IllegalArgumentException.class, () -> TypeToken.parameterizedOf(Map.class));
        assertThrows(NullPointerException.class, () -> TypeToken.parameterizedOf(null, String.class));
        assertThrows(NullPointerException.class, () -> TypeToken.parameterizedOf(Map.class, (Class<?>) null));
    }

    @Test
    void list_of_should_reject_null_element_type() {
        assertThrows(NullPointerException.class, () -> TypeToken.listOf(null));
    }

    @Test
    void should_throw_json_exception_when_deserialization_fails() {
        assertThrows(JsonException.class, () -> Json.fromJson("{", String.class));
        assertThrows(JsonException.class, () -> Json.fromJson("{", TypeToken.listOf(String.class)));
    }

    @Test
    void should_throw_json_exception_when_serialization_fails() {
        assertThrows(JsonException.class, () -> Json.toJson(new Object()));
    }

    @Test
    void should_pretty_print_non_string_object() {
        record Sample(String name) {}
        var result = Json.prettyPrint(new Sample("test"));
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    void type_token_equals_same_instance_returns_true() {
        TypeToken<List<String>> t = TypeToken.listOf(String.class);
        assertEquals(t, t);
    }

    @Test
    void type_token_not_equal_to_non_type_token_object() {
        TypeToken<List<String>> t = TypeToken.listOf(String.class);
        assertFalse(t.equals("not a token"));
    }

    @Test
    void type_token_hash_code_is_consistent_for_equal_tokens() {
        TypeToken<List<String>> t1 = TypeToken.listOf(String.class);
        TypeToken<List<String>> t2 = TypeToken.listOf(String.class);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void type_token_to_string_contains_type_name() {
        TypeToken<List<String>> token = TypeToken.listOf(String.class);
        assertNotNull(token.toString());
        assertTrue(token.toString().contains("String"));
    }

    @Test
    void type_token_parameterized_impl_exposes_raw_type_and_arguments() {
        TypeToken<Map<String, Integer>> token =
            TypeToken.parameterizedOf(Map.class, String.class, Integer.class);
        java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) token.getType();
        assertEquals(Map.class, pt.getRawType());
        assertEquals(null, pt.getOwnerType());
        assertEquals(2, pt.getActualTypeArguments().length);
    }

    @Test
    void type_token_parameterized_impl_equal_tokens_have_same_hash_code() {
        TypeToken<Map<String, Integer>> a = TypeToken.parameterizedOf(Map.class, String.class, Integer.class);
        TypeToken<Map<String, Integer>> b = TypeToken.parameterizedOf(Map.class, String.class, Integer.class);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void type_token_parameterized_impl_not_equal_to_different_args() {
        TypeToken<Map<String, Integer>> a = TypeToken.parameterizedOf(Map.class, String.class, Integer.class);
        TypeToken<Map<String, String>> b = TypeToken.parameterizedOf(Map.class, String.class, String.class);
        assertNotEquals(a, b);
    }

    @Test
    void type_token_parameterized_impl_to_string_contains_arg_names() {
        TypeToken<Map<String, Integer>> token =
            TypeToken.parameterizedOf(Map.class, String.class, Integer.class);
        assertNotNull(token.toString());
        assertTrue(token.toString().contains("String"));
    }

    @Test
    void json_exception_stores_message_and_cause() {
        RuntimeException cause = new RuntimeException("root");
        JsonException ex = new JsonException("wrap", cause);
        assertEquals("wrap", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
