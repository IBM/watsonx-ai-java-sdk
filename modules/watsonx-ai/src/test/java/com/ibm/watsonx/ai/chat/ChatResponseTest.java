/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.TextChatResponse.DetectionEntry;
import com.ibm.watsonx.ai.chat.TextChatResponse.DetectionResult;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult.Position;
import com.ibm.watsonx.ai.gateway.ModelGatewayChatResponse;

public class ChatResponseTest {

    private static final Map<String, List<ModerationResult>> MODERATIONS =
        Map.of("output", List.of(new ModerationResult(0.9f, false, new Position(0, 4), "EmailAddress", "test")));

    private static final Map<String, List<DetectionEntry>> DETECTIONS =
        Map.of("output", List.of(new DetectionEntry(0, List.of(
            new DetectionResult("en_syntax_rbr_pii", "pii", "PhoneNumber", 0.8, "123", 0, 3)))));

    private static TextChatResponse.Builder<?> textChatResponse() {
        return TextChatResponse.builder()
            .id("chat-1")
            .object("chat.completion")
            .model("ibm/granite-3-3-8b-instruct")
            .modelId("ibm/granite-3-3-8b-instruct")
            .modelVersion("1.0.0")
            .createdAt("2026-08-04T10:00:00.000Z")
            .moderations(MODERATIONS)
            .detections(DETECTIONS);
    }

    private static ModelGatewayChatResponse.Builder<?> gatewayChatResponse() {
        return ModelGatewayChatResponse.builder()
            .id("chat-1")
            .object("chat.completion")
            .model("gpt-4o")
            .serviceTier("default")
            .systemFingerprint("fp_abc123")
            .cached(false);
    }

    @Test
    void should_be_equal_when_all_fields_match() {
        assertEquals(textChatResponse().build(), textChatResponse().build());
        assertEquals(textChatResponse().build().hashCode(), textChatResponse().build().hashCode());
        assertEquals(gatewayChatResponse().build(), gatewayChatResponse().build());
        assertEquals(gatewayChatResponse().build().hashCode(), gatewayChatResponse().build().hashCode());
    }

    @Test
    void should_compare_base_fields() {
        assertNotEquals(textChatResponse().build(), textChatResponse().id("chat-2").build());
        assertNotEquals(textChatResponse().build(), textChatResponse().object("other").build());
        assertNotEquals(textChatResponse().build(), textChatResponse().model("other").build());
    }

    @Test
    void should_compare_text_chat_response_fields() {

        var reference = textChatResponse().build();

        assertNotEquals(reference, textChatResponse().modelId("other").build());
        assertNotEquals(reference, textChatResponse().modelVersion("2.0.0").build());
        assertNotEquals(reference, textChatResponse().createdAt("2026-08-05T10:00:00.000Z").build());
        assertNotEquals(reference, textChatResponse().moderations(null).build());
        assertNotEquals(reference, textChatResponse().detections(null).build());

        assertNotEquals(reference.hashCode(), textChatResponse().modelId("other").build().hashCode());
        assertNotEquals(reference.hashCode(), textChatResponse().modelVersion("2.0.0").build().hashCode());
        assertNotEquals(reference.hashCode(), textChatResponse().createdAt("2026-08-05T10:00:00.000Z").build().hashCode());
        assertNotEquals(reference.hashCode(), textChatResponse().moderations(null).build().hashCode());
        assertNotEquals(reference.hashCode(), textChatResponse().detections(null).build().hashCode());
    }

    @Test
    void should_compare_gateway_chat_response_fields() {

        var reference = gatewayChatResponse().build();

        assertNotEquals(reference, gatewayChatResponse().serviceTier("flex").build());
        assertNotEquals(reference, gatewayChatResponse().systemFingerprint("fp_other").build());
        assertNotEquals(reference, gatewayChatResponse().cached(true).build());

        assertNotEquals(reference.hashCode(), gatewayChatResponse().serviceTier("flex").build().hashCode());
        assertNotEquals(reference.hashCode(), gatewayChatResponse().systemFingerprint("fp_other").build().hashCode());
        assertNotEquals(reference.hashCode(), gatewayChatResponse().cached(true).build().hashCode());
    }

    @Test
    void should_not_be_equal_across_the_response_hierarchy() {

        var chatResponse = ChatResponse.builder().id("chat-1").object("chat.completion").model("gpt-4o").build();
        var textChatResponse = TextChatResponse.builder().id("chat-1").object("chat.completion").model("gpt-4o").build();
        var gatewayChatResponse = ModelGatewayChatResponse.builder().id("chat-1").object("chat.completion").model("gpt-4o").build();

        assertNotEquals(chatResponse, textChatResponse);
        assertNotEquals(textChatResponse, chatResponse);
        assertNotEquals(textChatResponse, gatewayChatResponse);
        assertNotEquals(gatewayChatResponse, textChatResponse);
    }
}
