/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.jackson3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.exception.JsonException;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.jackson3.provider.JacksonProvider;

public class JacksonProviderTest {

    private static final String SECRET = "synthetic-secret-12345";
    private static final String NESTED_SECRET = "synthetic-nested-secret-67890";

    private final JacksonProvider provider = new JacksonProvider();

    @Test
    void should_validate_complete_json_objects() {
        for (var json : List.of(
            "{}", "{\"name\":\"Alan\"}", " \n {\"name\":\"Alan\"} \t\r\n",
            "{\"text\":\"quoted \\\"value\\\" and \\\\ slash\",\"nested\":{\"ok\":true,\"values\":[1,null,{}]}}"))
            assertTrue(provider.isValidObject(json), json);
    }

    @Test
    void should_reject_values_that_are_not_exactly_one_json_object() {
        var encoded = provider.toJson("{}");
        var invalidValues = Arrays.asList(
            null, "", " \t\r\n", "null", "true", "42", "[]", "[\"Alan\"]", "\"text\"",
            "{", "{\"name\":}", "\"{", "\"{}\\q\"",
            "{}{}", "{} trailing", "{}[]", "{} null", "{\"type\":\"object\"}{\"type\":\"string\"}",
            provider.toJson("{\"name\":\"Alan\"}"), encoded,
            encoded + "{}", encoded + " trailing", encoded + " null", encoded + encoded,
            provider.toJson("{}{}"), provider.toJson("{} trailing"), provider.toJson("[]"), provider.toJson("null"),
            provider.toJson(encoded), provider.toJson(provider.toJson(encoded)),
            "\f{}", "\u2003{}", "{}\f", "{}\u2003", "\f" + encoded, encoded + "\u2003", provider.toJson("\f{}"));

        for (var json : invalidValues)
            assertFalse(provider.isValidObject(json), String.valueOf(json));
    }

    @Test
    void should_pretty_print_complete_json_objects_with_the_existing_format() {
        for (var json : List.of("{}", "{\"name\":\"Alan\",\"nested\":{\"values\":[1,null,true]}}",
            " \n {\"text\":\"quoted \\\"value\\\" and \\\\ slash\"} \t\r\n"))
            assertEquals(provider.prettyPrint(json), provider.prettyPrintObject(json), json);
    }

    @Test
    void should_preserve_input_when_pretty_printing_content_other_than_one_object() {
        for (var json : Arrays.asList(null, "", " \t\r\n", "null", "true", "42", "\"text\"", "[1,2]",
            "{", "{\"name\":}", "{}{}", "{} trailing", "{}[]", "{} null", "\"{}\"", "\f{}", "{}\u2003"))
            assertEquals(json, provider.prettyPrintObject(json), String.valueOf(json));
    }

    @Test
    void should_preserve_custom_pretty_printing_in_subclasses() {
        var custom = new JacksonProvider() {
            @Override
            public String prettyPrint(Object object) {
                return "custom: " + object;
            }
        };

        assertEquals("custom: {\"name\":\"Alan\"}", custom.prettyPrintObject("{\"name\":\"Alan\"}"));
    }

    @Test
    void should_preserve_custom_object_validation_in_subclasses() {
        var custom = new JacksonProvider() {
            @Override
            public boolean isValidObject(String json) {
                return false;
            }

            @Override
            public String prettyPrint(Object object) {
                throw new AssertionError("Rejected input must not be formatted");
            }
        };

        assertEquals("{\"name\":\"Alan\"}", custom.prettyPrintObject("{\"name\":\"Alan\"}"));
    }

    @Test
    void should_omit_payload_values_and_causes_from_class_mapping_errors() {
        for (var json : List.of("\"" + SECRET + "\"", SECRET, "{\"password\":\"" + SECRET + "\"")) {
            var exception = assertThrows(JsonException.class, () -> provider.fromJson(json, Integer.class));

            assertTrue(exception.getMessage().startsWith("Failed to deserialize JSON ("));
            assertTrue(exception.getMessage().contains("line 1, column "));
            assertSafeDiagnostic(exception);
        }
    }

    @Test
    void should_omit_payload_values_and_causes_from_generic_mapping_errors() {
        for (var json : List.of("[\"" + SECRET + "\"]", "[" + SECRET + "]", "[\"" + SECRET + "\"")) {
            var exception = assertThrows(JsonException.class, () -> provider.fromJson(json, TypeToken.listOf(Integer.class)));

            assertTrue(exception.getMessage().startsWith("Failed to deserialize JSON ("));
            assertTrue(exception.getMessage().contains("line 1, column "));
            assertSafeDiagnostic(exception);
        }
    }

    @Test
    void should_omit_values_and_nested_causes_from_serialization_errors() {
        var accessed = new AtomicBoolean();
        var value = new AbstractList<Object>() {
            @Override
            public Object get(int index) {
                accessed.set(true);
                throw new IllegalArgumentException(SECRET, new IllegalStateException(NESTED_SECRET));
            }

            @Override
            public int size() {
                return 1;
            }
        };

        var exception = assertThrows(JsonException.class, () -> provider.toJson(Map.of("value", value)));

        assertTrue(accessed.get(), "Serialization must reach the failing element");
        assertTrue(exception.getMessage().startsWith("Failed to serialize object to JSON ("));
        assertSafeDiagnostic(exception);
    }

    @Test
    void should_preserve_actual_values_during_serialization_and_deserialization() {
        var credentials = Map.of("password", SECRET, "access_token", NESTED_SECRET);

        var json = provider.toJson(credentials);

        assertTrue(json.contains(SECRET));
        assertTrue(json.contains(NESTED_SECRET));
        assertEquals(credentials, provider.fromJson(json, TypeToken.parameterizedOf(Map.class, String.class, String.class)));
    }

    private void assertSafeDiagnostic(JsonException exception) {
        var output = new StringWriter();
        exception.printStackTrace(new PrintWriter(output));

        assertFalse(output.toString().contains(SECRET));
        assertFalse(output.toString().contains(NESTED_SECRET));
        assertNull(exception.getCause());
        assertEquals(0, exception.getSuppressed().length);
    }
}
