/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

public class JsonTest {

    record Person(String name, String lastname) {};

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
}
