/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.TextChatResponse.DetectionEntry;
import com.ibm.watsonx.ai.chat.TextChatResponse.DetectionResult;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult;
import com.ibm.watsonx.ai.chat.TextChatResponse.ModerationResult.Position;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.ExtractionTags.Think;
import com.ibm.watsonx.ai.chat.model.FinishReason;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatResponse;

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

    private static ChatResponse singleChoiceResponse(ResultMessage message, String finishReason) {
        return ChatResponse.builder()
            .id("chat-1")
            .object("chat.completion")
            .model("ibm/granite-3-3-8b-instruct")
            .choices(List.of(new ResultChoice(0, message, finishReason)))
            .build();
    }

    private static ResultMessage resultMessage(String content, String reasoningContent, String refusal, List<ToolCall> toolCalls) {
        return new ResultMessage("assistant", content, reasoningContent, refusal, toolCalls);
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

    @Test
    void should_throw_when_the_response_contains_no_choices() {

        var noChoices = ChatResponse.builder().id("chat-1").build();

        var ex = assertThrows(EmptyChatResponseException.class, noChoices::toAssistantMessage);
        assertEquals("The chat response contains no choices", ex.getMessage());
        assertEquals(FinishReason.INCOMPLETE, ex.finishReason());
        assertEquals(EmptyChatResponseException.NO_CHOICE, ex.index());
        assertSame(noChoices, ex.response());

        var emptyChoices = ChatResponse.builder().id("chat-1").choices(List.of()).build();
        assertThrows(EmptyChatResponseException.class, emptyChoices::toAssistantMessages);
    }

    @Test
    void should_throw_when_the_model_generates_no_usable_output() {

        for (String content : new String[] { null, "", "   ", "\n" }) {

            var response = singleChoiceResponse(resultMessage(content, null, null, null), "stop");

            var ex = assertThrows(EmptyChatResponseException.class, response::toAssistantMessage);
            assertEquals("The model generated no content, tool calls or refusal (finish reason: STOP)", ex.getMessage());
            assertEquals(FinishReason.STOP, ex.finishReason());
            assertEquals(0, ex.index());
            assertSame(response, ex.response());
        }
    }

    @Test
    void should_throw_when_the_tool_calls_are_empty() {
        var response = singleChoiceResponse(resultMessage("", null, null, List.of()), "stop");
        assertThrows(EmptyChatResponseException.class, response::toAssistantMessage);
    }

    @Test
    void should_throw_when_only_the_thinking_is_present() {

        var response = singleChoiceResponse(resultMessage(null, "The user is asking for", null, null), "length");

        var ex = assertThrows(EmptyChatResponseException.class, response::toAssistantMessage);
        assertEquals(FinishReason.LENGTH, ex.finishReason());
    }

    @Test
    void should_throw_when_the_choice_contains_no_message() {

        var response = ChatResponse.builder().choices(List.of(new ResultChoice(0, null, "stop"))).build();

        var ex = assertThrows(EmptyChatResponseException.class, response::toAssistantMessage);
        assertEquals("The choice at index 0 contains no message", ex.getMessage());
    }

    @Test
    void should_throw_when_the_extraction_tags_leave_no_response() {

        var response = ChatResponse.builder()
            .extractionTags(ExtractionTags.of(new Think("<think>", "</think>")))
            .choices(List.of(new ResultChoice(0, resultMessage("<think>Only reasoning</think>", null, null, null), "stop")))
            .build();

        var ex = assertThrows(EmptyChatResponseException.class, response::toAssistantMessage);
        assertEquals(FinishReason.STOP, ex.finishReason());
    }

    @Test
    void should_report_the_finish_reason_and_the_index_of_the_empty_choice() {

        var response = ChatResponse.builder()
            .choices(List.of(
                new ResultChoice(0, resultMessage("First answer", null, null, null), "stop"),
                new ResultChoice(1, resultMessage(null, null, null, null), "length")))
            .build();

        var ex = assertThrows(EmptyChatResponseException.class, response::toAssistantMessages);
        assertEquals(1, ex.index());
        assertEquals(FinishReason.LENGTH, ex.finishReason());
    }

    @Test
    void should_convert_when_the_refusal_is_the_only_output() {

        var response = singleChoiceResponse(resultMessage(null, null, "I cannot help with that", null), "stop");

        var assistantMessage = response.toAssistantMessage();
        assertNull(assistantMessage.content());
        assertEquals("I cannot help with that", assistantMessage.refusal());
    }

    @Test
    void should_convert_when_the_tool_calls_are_the_only_output() {

        var toolCalls = List.of(ToolCall.of("call-1", "get_weather", "{\"city\":\"Rome\"}"));
        var response = singleChoiceResponse(resultMessage(null, null, null, toolCalls), "tool_calls");

        var assistantMessage = response.toAssistantMessage();
        assertNull(assistantMessage.content());
        assertTrue(assistantMessage.hasToolCalls());
    }

    @Test
    void should_convert_every_choice_when_all_of_them_have_content() {

        var response = ChatResponse.builder()
            .choices(List.of(
                new ResultChoice(0, resultMessage("First answer", null, null, null), "stop"),
                new ResultChoice(1, resultMessage("Second answer", "The user is asking for", null, null), "stop")))
            .build();

        var assistantMessages = response.toAssistantMessages();
        assertEquals(2, assistantMessages.size());
        assertEquals("First answer", assistantMessages.get(0).content());
        assertEquals("Second answer", assistantMessages.get(1).content());
        assertEquals("The user is asking for", assistantMessages.get(1).thinking());
    }

    @Test
    void should_not_be_equal_when_null_fields_differ_in_chat_response() {
        assertNotEquals(ChatResponse.builder().build(), ChatResponse.builder().id("id1").build());
        assertNotEquals(ChatResponse.builder().object("o").build(), ChatResponse.builder().build());
        assertNotEquals(ChatResponse.builder().model("m").build(), ChatResponse.builder().build());
        assertNotEquals(ChatResponse.builder().choices(List.of()).build(), ChatResponse.builder().build());
        assertNotEquals(ChatResponse.builder().created(1L).build(), ChatResponse.builder().build());
        assertNotEquals(ChatResponse.builder().usage(new com.ibm.watsonx.ai.chat.model.ChatUsage(1, 2, 3)).build(), ChatResponse.builder().build());
    }

    @Test
    void should_round_trip_chat_response_via_toBuilder() {
        var original = ChatResponse.builder().id("id1").model("m1").object("chat.completion").created(100L).build();
        assertEquals(original, original.toBuilder().build());
    }

    @Test
    void should_include_id_in_chat_response_toString() {
        assertTrue(ChatResponse.builder().id("test-id").build().toString().contains("test-id"));
    }

    @Test
    void should_not_be_equal_when_null_fields_differ_in_text_chat_response() {
        assertNotEquals(TextChatResponse.builder().build(), TextChatResponse.builder().modelVersion("v1").build());
        assertNotEquals(TextChatResponse.builder().build(), TextChatResponse.builder().createdAt("2025").build());
    }

    @Test
    void should_not_be_equal_when_null_fields_differ_in_gateway_response() {
        assertNotEquals(ModelGatewayChatResponse.builder().build(), ModelGatewayChatResponse.builder().systemFingerprint("fp1").build());
        assertNotEquals(ModelGatewayChatResponse.builder().build(), ModelGatewayChatResponse.builder().cached(true).build());
    }

    @Test
    void should_round_trip_gateway_response_via_toBuilder() {
        var orig = ModelGatewayChatResponse.builder().serviceTier("default").systemFingerprint("fp1").cached(false).build();
        assertEquals(orig, orig.toBuilder().build());
    }

    @Test
    void should_include_serviceTier_in_gateway_response_toString() {
        assertTrue(ModelGatewayChatResponse.builder().serviceTier("default").build().toString().contains("default"));
    }
}
