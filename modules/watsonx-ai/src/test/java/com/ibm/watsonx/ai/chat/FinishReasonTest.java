/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.model.FinishReason;

public class FinishReasonTest {

    @Test
    void test_finish_reason() {
        assertEquals("cancelled", FinishReason.CANCELLED.value());
        assertEquals("error", FinishReason.ERROR.value());
        assertEquals(null, FinishReason.INCOMPLETE.value());
        assertEquals("length", FinishReason.LENGTH.value());
        assertEquals("stop", FinishReason.STOP.value());
        assertEquals("time_limit", FinishReason.TIME_LIMIT.value());
        assertEquals("tool_calls", FinishReason.TOOL_CALLS.value());

        assertEquals(FinishReason.CANCELLED, FinishReason.fromValue("cancelled"));
        assertEquals(FinishReason.ERROR, FinishReason.fromValue("error"));
        assertEquals(FinishReason.INCOMPLETE, FinishReason.fromValue(null));
        assertEquals(FinishReason.LENGTH, FinishReason.fromValue("length"));
        assertEquals(FinishReason.STOP, FinishReason.fromValue("stop"));
        assertEquals(FinishReason.TIME_LIMIT, FinishReason.fromValue("time_limit"));
        assertEquals(FinishReason.TOOL_CALLS, FinishReason.fromValue("tool_calls"));
        assertThrows(IllegalArgumentException.class, () -> FinishReason.fromValue("unknown"), "Unknown finish reason: unknown");
    }
}
