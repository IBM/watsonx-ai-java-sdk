/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;

public class JsonTest {

    record Person(String firstName, String lastName, String nickname) {}

    @Test
    void should_serialize_with_snake_case_and_without_nulls() {
        var json = Json.toJson(new Person("Alan", "Wake", null));

        assertEquals("{\"first_name\":\"Alan\",\"last_name\":\"Wake\"}", json);
    }

    @Test
    void should_deserialize_generic_type() {
        var people = Json.fromJson("[{\"first_name\":\"Alan\",\"last_name\":\"Wake\"}]", TypeToken.listOf(Person.class));

        assertEquals(List.of(new Person("Alan", "Wake", null)), people);
    }

    @Test
    void should_validate_json_objects() {
        assertTrue(Json.isValidObject("{\"name\":\"Alan\"}"));
        assertFalse(Json.isValidObject("[\"Alan\"]"));
    }
}
