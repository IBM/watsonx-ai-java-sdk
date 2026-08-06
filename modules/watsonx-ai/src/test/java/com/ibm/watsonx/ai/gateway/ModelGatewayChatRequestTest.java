/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class ModelGatewayChatRequestTest {

    @Test
    void should_round_trip_via_toBuilder() {
        var original = ModelGatewayChatRequest.builder()
            .messages(UserMessage.text("hello"))
            .build();
        assertNotNull(original.toBuilder().build());
    }

    @Test
    void should_include_class_name_in_toString() {
        var request = ModelGatewayChatRequest.builder()
            .messages(UserMessage.text("hi"))
            .build();
        assertTrue(request.toString().contains("ModelGatewayChatRequest"));
    }
}
