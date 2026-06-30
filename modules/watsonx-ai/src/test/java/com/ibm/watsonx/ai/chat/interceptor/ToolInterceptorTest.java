/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatResponse.ResultChoice;
import com.ibm.watsonx.ai.chat.model.ResultMessage;
import com.ibm.watsonx.ai.chat.model.ToolCall;

public class ToolInterceptorTest {

    @Test
    void should_not_duplicate_tool_calls_when_choice_has_multiple() {

        var toolCalls = List.of(
            ToolCall.of("call-1", "search", "{\"q\":\"a\"}"),
            ToolCall.of("call-2", "lookup", "{\"q\":\"b\"}"));

        var message = new ResultMessage("assistant", null, null, null, toolCalls);
        var choice = new ResultChoice(0, message, "tool_calls");
        var response = ChatResponse.build().choices(List.of(choice)).build();
        var context = new InterceptorContext(null, null, response);

        // Identity interceptor: each tool call must map to exactly itself, with no cross product.
        ToolInterceptor interceptor = (ctx, fc) -> fc;

        var result = interceptor.intercept(context);
        var resultToolCalls = result.get(0).message().toolCalls();

        assertEquals(2, resultToolCalls.size());
        assertEquals("call-1", resultToolCalls.get(0).id());
        assertEquals("search", resultToolCalls.get(0).function().name());
        assertEquals("call-2", resultToolCalls.get(1).id());
        assertEquals("lookup", resultToolCalls.get(1).function().name());
    }
}
