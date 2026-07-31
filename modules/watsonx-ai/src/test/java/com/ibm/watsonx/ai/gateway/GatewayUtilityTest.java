/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class GatewayUtilityTest {

    private static final String MODEL_ID = "openai/gpt-4o";
    private static final long TIME_LIMIT = 10_000L;

    private static ChatRequest requestWith(ChatParameters parameters) {
        return ChatRequest.builder()
            .messages(List.of(UserMessage.text("Hi")))
            .parameters(parameters)
            .build();
    }

    static Stream<Arguments> nativeOnlyFields() {
        return Stream.of(
            arguments("projectId", ChatParameters.builder().projectId("my-project").build()),
            arguments("spaceId", ChatParameters.builder().spaceId("my-space").build()),
            arguments("crypto", ChatParameters.builder().crypto("crn:v1:crypto").build()),
            arguments("guidedChoice", ChatParameters.builder().guidedChoice("yes", "no").build()),
            arguments("guidedRegex", ChatParameters.builder().guidedRegex("[0-9]+").build()),
            arguments("guidedGrammar", ChatParameters.builder().guidedGrammar("root ::= \"x\"").build()),
            arguments("repetitionPenalty", ChatParameters.builder().repetitionPenalty(1.2).build()),
            arguments("lengthPenalty", ChatParameters.builder().lengthPenalty(1.5).build()),
            arguments("context", ChatParameters.builder().context("some context").build()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nativeOnlyFields")
    void should_throw_when_native_only_field_set_on_request(String fieldName, ChatParameters parameters) {
        var request = requestWith(parameters);

        var ex = assertThrows(IllegalArgumentException.class,
            () -> GatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT));

        assertTrue(ex.getMessage().contains(fieldName), () -> "message should name the offending field but was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("request parameters"),
            () -> "message should identify the request parameters as the source but was: " + ex.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nativeOnlyFields")
    void should_throw_when_native_only_field_set_on_defaults(String fieldName, ChatParameters defaults) {
        var request = ChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var ex = assertThrows(IllegalArgumentException.class,
            () -> GatewayUtility.buildGatewayRequest(request, defaults, MODEL_ID, TIME_LIMIT));

        assertTrue(ex.getMessage().contains(fieldName), () -> "message should name the offending field but was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("default parameters"),
            () -> "message should identify the default parameters as the source but was: " + ex.getMessage());
    }

    @Test
    void should_list_all_offending_fields() {
        var parameters = ChatParameters.builder()
            .projectId("my-project")
            .spaceId("my-space")
            .repetitionPenalty(1.1)
            .temperature(0.7) // a common field, must NOT be reported
            .build();
        var request = requestWith(parameters);

        var ex = assertThrows(IllegalArgumentException.class,
            () -> GatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT));

        assertTrue(ex.getMessage().contains("projectId"), ex::getMessage);
        assertTrue(ex.getMessage().contains("spaceId"), ex::getMessage);
        assertTrue(ex.getMessage().contains("repetitionPenalty"), ex::getMessage);
        assertFalse(ex.getMessage().contains("temperature"),
            () -> "common field temperature must not be reported but was: " + ex.getMessage());
    }

    @Test
    void should_throw_on_streaming_overload_too() {
        var request = requestWith(ChatParameters.builder().guidedRegex("[0-9]+").build());

        var ex = assertThrows(IllegalArgumentException.class,
            () -> GatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT, true));

        assertTrue(ex.getMessage().contains("guidedRegex"), ex::getMessage);
    }

    @Test
    void should_not_throw_for_gateway_parameters() {
        var parameters = ModelGatewayParameters.builder()
            .temperature(0.7)
            .reasoningEffort(ModelGatewayParameters.ReasoningEffort.LOW)
            .store(true)
            .build();
        var request = ChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).parameters(parameters).build();

        var result = assertDoesNotThrow(() -> GatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT));

        assertNotNull(result);
        assertEquals(MODEL_ID, result.model());
    }

    @Test
    void should_not_throw_for_chat_parameters_with_only_common_fields() {
        var parameters = ChatParameters.builder()
            .temperature(0.7)
            .maxCompletionTokens(100)
            .build();
        var request = requestWith(parameters);

        var result = assertDoesNotThrow(() -> GatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT));

        assertNotNull(result);
        assertEquals(MODEL_ID, result.model());
    }

    @Test
    void should_not_throw_when_no_parameters_supplied() {
        var request = ChatRequest.builder().messages(List.of(UserMessage.text("Hi"))).build();

        var result = assertDoesNotThrow(() -> GatewayUtility.buildGatewayRequest(request, null, MODEL_ID, TIME_LIMIT));

        assertNotNull(result);
        assertEquals(MODEL_ID, result.model());
    }
}
