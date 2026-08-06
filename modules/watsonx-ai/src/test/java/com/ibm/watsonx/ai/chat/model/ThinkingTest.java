/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.ExtractionTags.Response;
import com.ibm.watsonx.ai.chat.model.ExtractionTags.Think;

public class ThinkingTest {

    @Test
    void should_be_equal_when_all_fields_match() {
        assertEquals(Thinking.of(ThinkingEffort.HIGH), Thinking.of(ThinkingEffort.HIGH));
        assertEquals(Thinking.of(ThinkingEffort.HIGH).hashCode(), Thinking.of(ThinkingEffort.HIGH).hashCode());
    }

    @Test
    void should_be_equal_when_all_fields_are_null() {
        var a = Thinking.builder().build();
        var b = Thinking.builder().build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void should_be_equal_to_itself() {
        var t = Thinking.of(ThinkingEffort.HIGH);
        assertEquals(t, t);
    }

    @Test
    void should_not_be_equal_to_null() {
        assertNotEquals(null, Thinking.of(ThinkingEffort.HIGH));
    }

    @Test
    void should_not_be_equal_to_different_type() {
        assertNotEquals("string", Thinking.of(ThinkingEffort.HIGH));
    }

    @Test
    void should_not_be_equal_when_effort_differs() {
        assertNotEquals(Thinking.of(ThinkingEffort.HIGH), Thinking.of(ThinkingEffort.LOW));
    }

    @Test
    void should_not_be_equal_when_includeReasoning_differs() {
        assertNotEquals(Thinking.builder().includeReasoning(true).build(), Thinking.builder().build());
        assertNotEquals(
            Thinking.builder().includeReasoning(true).build(),
            Thinking.builder().includeReasoning(false).build());
    }

    @Test
    void should_not_be_equal_when_extractionTags_differs() {
        var tags = ExtractionTags.of(new Think("<think>", "</think>"), new Response("<response>", "</response>"));
        assertNotEquals(Thinking.of(tags), Thinking.builder().build());
    }

    @Test
    void should_not_be_equal_when_both_have_different_extractionTags() {
        var tags1 = ExtractionTags.of(new Think("<think>", "</think>"), new Response("<r>", "</r>"));
        var tags2 = ExtractionTags.of(new Think("<t>", "</t>"), new Response("<r>", "</r>"));
        assertNotEquals(Thinking.of(tags1), Thinking.of(tags2));
    }

    @Test
    void should_produce_non_null_toString() {
        assertNotNull(Thinking.builder().enabled(true).thinkingEffort(ThinkingEffort.HIGH).build().toString());
    }
}
