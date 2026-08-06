/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.UserMessage;

public class DeploymentChatRequestTest {

    @Test
    void should_round_trip_via_toBuilder() {
        var original = DeploymentChatRequest.builder()
            .deploymentId("dep-1")
            .messages(UserMessage.text("hello"))
            .build();
        var rebuilt = original.toBuilder().build();
        assertEquals("dep-1", rebuilt.deploymentId());
        assertNotNull(rebuilt.messages());
    }

    @Test
    void should_include_deploymentId_in_toString() {
        var request = DeploymentChatRequest.builder()
            .deploymentId("dep-42")
            .messages(UserMessage.text("hi"))
            .build();
        assertTrue(request.toString().contains("dep-42"));
    }
}
