/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.BaseChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.FunctionCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.model.ToolMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatParameters;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatRequest;
import com.ibm.watsonx.ai.gateway.chat.ModelGatewayChatService;

/**
 * Checks tool interception, local execution and streaming completion delivery against the chat and Model Gateway endpoints. Chat defaults to Granite
 * 4 H Small. Gateway uses {@code WATSONX_GATEWAY_CHAT_MODEL_CLAUDE}. Override either model with {@code WATSONX_TOOL_COMPLETION_CHAT_MODEL} or
 * {@code WATSONX_TOOL_COMPLETION_GATEWAY_MODEL}. Tool execution only returns synthetic strings.
 */
final class ToolCallCompletionSupportTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(120);
    private static final String TOOL_NAME = "draft_city_plan";
    private static final List<String> CITIES = List.of("rome", "oslo", "lisbon");

    private ToolCallCompletionSupportTest() {}

    static ChatService chatService(List<FunctionCall> intercepted) {
        return ChatService.builder()
            .baseUrl(System.getenv("WATSONX_URL"))
            .apiKey(System.getenv("WATSONX_API_KEY"))
            .projectId(System.getenv("WATSONX_PROJECT_ID"))
            .modelId(configuredModel("WATSONX_TOOL_COMPLETION_CHAT_MODEL", "ibm/granite-4-h-small"))
            .timeout(TIMEOUT)
            .toolInterceptor((context, function) -> {
                intercepted.add(function);
                return function;
            })
            .build();
    }

    static ModelGatewayChatService gatewayService(List<FunctionCall> intercepted) {
        var model = configuredModel("WATSONX_TOOL_COMPLETION_GATEWAY_MODEL", System.getenv("WATSONX_GATEWAY_CHAT_MODEL_CLAUDE"));
        assumeTrue(model != null && !model.isBlank(),
            "Set WATSONX_TOOL_COMPLETION_GATEWAY_MODEL or WATSONX_GATEWAY_CHAT_MODEL_CLAUDE");
        return ModelGatewayChatService.builder()
            .baseUrl(System.getenv("WATSONX_URL"))
            .apiKey(System.getenv("WATSONX_API_KEY"))
            .modelId(model)
            .timeout(TIMEOUT)
            .toolInterceptor((context, function) -> {
                intercepted.add(function);
                return function;
            })
            .build();
    }

    private static String configuredModel(String variable, String fallback) {
        var configured = System.getenv(variable);
        return configured == null || configured.isBlank() ? fallback : configured.trim();
    }

    static ChatRequest chatRequest(String nonce) {
        return ChatRequest.builder()
            .messages(UserMessage.text(prompt(nonce)))
            .tools(tool())
            .parameters(ChatParameters.builder().toolChoiceOption(ToolChoiceOption.REQUIRED).maxCompletionTokens(2048).build())
            .build();
    }

    static ModelGatewayChatRequest gatewayRequest(String nonce) {
        return ModelGatewayChatRequest.builder()
            .messages(UserMessage.text(prompt(nonce)))
            .tools(tool())
            .parameters(ModelGatewayChatParameters.builder()
                .toolChoiceOption(ToolChoiceOption.REQUIRED)
                .parallelToolCalls(true)
                .maxCompletionTokens(2048)
                .build())
            .build();
    }

    private static Tool tool() {
        return Tool.of(TOOL_NAME, "Draft a synthetic travel plan without external effects.", JsonSchema.object()
            .property("city", JsonSchema.string())
            .property("request_id", JsonSchema.string())
            .property("notes", JsonSchema.string().description("A travel plan of about 25 words"))
            .required("city", "request_id", "notes"));
    }

    private static String prompt(String nonce) {
        return """
            Produce exactly THREE draft_city_plan function calls in this single response, one for rome, one for oslo and one for lisbon.
            Do not wait for tool results between calls. Set city to the lowercase city name.
            Set request_id to the city name followed by a hyphen and this unique request nonce: %s
            Preserve the nonce exactly. Give each call its own notes string of about 25 English words describing food, museums and walks.
            These are synthetic drafts. All necessary information is available. Return only the three function calls, without prose.
            """.formatted(nonce);
    }

    static ChatResponse await(CompletableFuture<ChatResponse> future) throws Exception {
        try {
            return future.get(TIMEOUT.toSeconds() + 10, TimeUnit.SECONDS);
        } finally {
            if (!future.isDone())
                future.cancel(true);
        }
    }

    static List<ToolMessage> verifyResponse(ChatResponse response, String nonce, List<FunctionCall> intercepted) {
        assertNotNull(response);
        assertEquals(1, response.choices().size(), response.toString());
        var tools = response.toAssistantMessage().toolCalls();
        assertNotNull(tools, response.toString());
        assertEquals(3, tools.size(), response.toString());
        assertEquals(tools.stream().map(ToolCall::function).toList(), intercepted, "Each tool must be intercepted exactly once");
        var ids = new HashSet<String>();
        var results = new ArrayList<ToolMessage>();
        for (var tool : tools) {
            assertNotNull(tool.id());
            assertFalse(tool.id().isBlank());
            assertTrue(ids.add(tool.id()), "Duplicate tool call ID: " + tool.id());
            results.add(processTool(tool, nonce));
        }
        assertEquals(CITIES.stream().map(city -> "drafted:" + city + "-" + nonce).sorted().toList(),
            results.stream().map(ToolMessage::content).sorted().toList());
        return results;
    }

    private static ToolMessage processTool(ToolCall tool, String nonce) {
        var result = tool.processTool((name, arguments) -> {
            assertEquals(TOOL_NAME, name);
            assertNotNull(arguments);
            String city = arguments.get("city");
            assertNotNull(city);
            city = city.toLowerCase(Locale.ROOT);
            assertTrue(CITIES.contains(city), "Unexpected city: " + city);
            String requestId = arguments.get("request_id");
            assertEquals(city + "-" + nonce, requestId);
            String notes = arguments.get("notes");
            assertNotNull(notes);
            assertFalse(notes.isBlank());
            return "drafted:" + requestId;
        });
        assertEquals(tool.id(), result.toolCallId());
        return result;
    }

    static void verifyStreaming(ChatResponse response, String nonce, List<FunctionCall> intercepted, CompletionHandler handler) {
        assertTrue(handler.errors.isEmpty(), () -> "Streaming errors: " + handler.errors);
        assertSame(response, handler.response);
        assertFalse(handler.events.isEmpty());
        assertEquals("response", handler.events.get(handler.events.size() - 1), handler.events.toString());
        assertEquals(1, handler.events.stream().filter("response"::equals).count());
        assertTrue(handler.completed.size() >= 2, "Need multiple completed tool callbacks");
        var processed = verifyResponse(response, nonce, intercepted);
        var finalTools = response.toAssistantMessage().toolCalls();
        assertEquals(finalTools, handler.completed.stream().map(CompletedToolCall::toolCall).toList(),
            "Completed tool callbacks must match the final tools without duplication");
        assertEquals(processed, handler.processed, "Tools must be executable with the same arguments in completion callbacks");
        for (int i = 0; i < handler.completed.size(); i++) {
            var completed = handler.completed.get(i);
            assertEquals(response.choices().get(0).index(), completed.index());
            assertNotNull(completed.toolCall().index());
            var index = completed.toolCall().index();
            int completion = handler.events.indexOf("complete:" + index);
            int lastPartial = handler.events.lastIndexOf("partial:" + index);
            assertTrue(lastPartial >= 0 && lastPartial < completion, "Tool fragments must precede completion: " + handler.events);
            if (i > 0) {
                var previous = handler.completed.get(i - 1).toolCall().index();
                assertTrue(handler.events.indexOf("complete:" + previous) < handler.events.indexOf("partial:" + index),
                    "Previous tool must complete before the next tool's first argument fragment: " + handler.events);
            }
        }
    }

    static final class CompletionHandler implements ChatHandler {
        private final String nonce;
        private final List<String> events = new ArrayList<>();
        private final List<CompletedToolCall> completed = new ArrayList<>();
        private final List<ToolMessage> processed = new ArrayList<>();
        private final List<Throwable> errors = new ArrayList<>();
        private ChatResponse response;

        CompletionHandler(String nonce) {
            this.nonce = nonce;
        }

        @Override
        public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
            events.add("text");
        }

        @Override
        public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
            events.add("thinking");
        }

        @Override
        public void onPartialToolCall(PartialToolCall partialToolCall) {
            events.add("partial:" + partialToolCall.toolIndex());
        }

        @Override
        public void onCompleteToolCall(CompletedToolCall completedToolCall) {
            events.add("complete:" + completedToolCall.toolCall().index());
            completed.add(completedToolCall);
            processed.add(processTool(completedToolCall.toolCall(), nonce));
        }

        @Override
        public void onCompleteResponse(ChatResponse completeResponse) {
            events.add("response");
            response = completeResponse;
        }

        @Override
        public void onError(Throwable error) {
            events.add("error");
            errors.add(error);
        }

        @Override
        public boolean failOnFirstError() {
            return true;
        }
    }
}
