/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.jackson2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.jackson2.provider.JacksonProvider;

public class JacksonProviderTest {

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
}
