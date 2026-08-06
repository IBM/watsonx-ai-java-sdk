/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.chat.ChatHandler;

@ExtendWith(MockitoExtension.class)
public class StreamingUtilsTest {

    @Test
    void should_unwrap_cause_and_notify_handler() {
        var handler = mock(ChatHandler.class);
        var cause = new RuntimeException("root cause");
        var wrapper = new RuntimeException("wrapper", cause);
        var result = StreamingUtils.handleError(wrapper, handler);
        assertEquals(cause, result);
        verify(handler).onError(cause);
    }

    @Test
    void should_pass_through_error_without_cause() {
        var handler = mock(ChatHandler.class);
        var error = new RuntimeException("no cause");
        var result = StreamingUtils.handleError(error, handler);
        assertEquals(error, result);
        verify(handler).onError(error);
    }
}
