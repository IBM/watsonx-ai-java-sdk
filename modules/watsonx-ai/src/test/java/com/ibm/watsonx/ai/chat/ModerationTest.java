/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.chat.ChatModeration.InputRanges;
import com.ibm.watsonx.ai.core.Json;

public class ModerationTest {

    @Test
    void should_serialize_moderation_parameters_correctly() throws Exception {

        var EXPECTED = """
            {
                "hap": {
                  "input": {
                    "enabled": true,
                    "threshold": 0.8
                  },
                  "output": {
                    "enabled": true,
                    "threshold": 0.9
                  },
                  "mask": {
                    "remove_entity_value": true
                  }
                },
                "pii": {
                  "input": {
                    "enabled": true
                  },
                  "output": {
                    "enabled": false
                  },
                  "mask": {
                    "remove_entity_value": false
                  }
                },
                "granite_guardian": {
                  "input": {
                    "enabled": true,
                    "threshold": 0.85
                  },
                  "mask": {
                    "remove_entity_value": true
                  }
                },
                "input_ranges": [
                  {
                    "start": 0,
                    "end": 50
                  },
                  {
                    "start": 100,
                    "end": 150
                  }
                ]
            }""";

        var moderation = ChatModeration.builder()
            .hap(h -> h.input(0.8f).output(0.9f).mask(true))
            .pii(p -> p.input(true).output(false).mask(false))
            .graniteGuardian(g -> g.input(0.85f).mask(true))
            .inputRanges(List.of(InputRanges.of(0, 50), InputRanges.of(100, 150)))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(moderation), true);
    }

    @Test
    void should_be_equal_when_configurations_match() {

        var a = ChatModeration.builder()
            .hap(h -> h.input(0.8f).mask(true))
            .pii(p -> p.output(true))
            .graniteGuardian(g -> g.input(0.85f))
            .inputRanges(List.of(InputRanges.of(0, 50)))
            .build();

        var b = ChatModeration.builder()
            .hap(h -> h.input(0.8f).mask(true))
            .pii(p -> p.output(true))
            .graniteGuardian(g -> g.input(0.85f))
            .inputRanges(List.of(InputRanges.of(0, 50)))
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void should_not_be_equal_when_detector_properties_differ() {

        var a = ChatModeration.builder().hap(h -> h.input(0.8f)).build();
        var b = ChatModeration.builder().hap(h -> h.input(0.9f)).build();

        assertNotEquals(a, b);
        assertNotEquals(a.hap(), b.hap());
    }

    @Test
    void should_not_be_equal_when_detector_types_differ_even_with_same_properties() {

        var hap = ChatModeration.builder().hap(h -> h.output(0.8f)).build().hap();
        var graniteGuardian = ChatModeration.builder().graniteGuardian(g -> g.input(0.8f)).build().graniteGuardian();

        assertNotEquals(hap, graniteGuardian);
    }

    @Test
    void should_satisfy_equals_contract() {

        var moderation = ChatModeration.builder().hap(h -> h.input(0.8f)).build();

        // Reflexive
        assertEquals(moderation, moderation);
        assertEquals(moderation.hap(), moderation.hap());

        // Null
        assertNotEquals(moderation, null);
        assertNotEquals(moderation.hap(), null);

        // Different class
        assertNotEquals(moderation, "not a ChatModeration");
        assertNotEquals(moderation.hap(), "not a detector");
    }

    @Test
    void should_detect_field_by_field_differences_in_equals() {

        var base = ChatModeration.builder()
            .hap(h -> h.input(0.8f))
            .pii(p -> p.output(true))
            .graniteGuardian(g -> g.input(0.85f))
            .inputRanges(List.of(InputRanges.of(0, 50)))
            .build();

        // Same as base
        var same = ChatModeration.builder()
            .hap(h -> h.input(0.8f))
            .pii(p -> p.output(true))
            .graniteGuardian(g -> g.input(0.85f))
            .inputRanges(List.of(InputRanges.of(0, 50)))
            .build();
        assertEquals(base, same);

        // hap differs
        var hapDiff = ChatModeration.builder()
            .hap(h -> h.input(0.5f))
            .pii(p -> p.output(true))
            .graniteGuardian(g -> g.input(0.85f))
            .inputRanges(List.of(InputRanges.of(0, 50)))
            .build();
        assertNotEquals(base, hapDiff);

        // pii differs
        var piiDiff = ChatModeration.builder()
            .hap(h -> h.input(0.8f))
            .pii(p -> p.output(false))
            .graniteGuardian(g -> g.input(0.85f))
            .inputRanges(List.of(InputRanges.of(0, 50)))
            .build();
        assertNotEquals(base, piiDiff);

        // graniteGuardian differs
        var ggDiff = ChatModeration.builder()
            .hap(h -> h.input(0.8f))
            .pii(p -> p.output(true))
            .graniteGuardian(g -> g.input(0.5f))
            .inputRanges(List.of(InputRanges.of(0, 50)))
            .build();
        assertNotEquals(base, ggDiff);

        // inputRanges differs
        var rangesDiff = ChatModeration.builder()
            .hap(h -> h.input(0.8f))
            .pii(p -> p.output(true))
            .graniteGuardian(g -> g.input(0.85f))
            .inputRanges(List.of(InputRanges.of(0, 100)))
            .build();
        assertNotEquals(base, rangesDiff);

        // Null vs non-null on each field
        var empty = ChatModeration.builder().build();
        assertNotEquals(empty, base);
        assertNotEquals(base, empty);
        assertNotEquals(empty, ChatModeration.builder().hap(h -> h.input(0.8f)).build());
        assertNotEquals(empty, ChatModeration.builder().pii(p -> p.output(true)).build());
        assertNotEquals(empty, ChatModeration.builder().graniteGuardian(g -> g.input(0.85f)).build());
        assertNotEquals(empty, ChatModeration.builder().inputRanges(List.of(InputRanges.of(0, 50))).build());
    }
}
