/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.ToolRegistry;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.FunctionCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.Json;

public class ChatBot {

    private static final Logger logger = LoggerFactory.getLogger(ChatBot.class);
    private static final Config config = ConfigProvider.getConfig();
    private final ToolRegistry toolRegistry;
    private final ChatService chatService;
    private final ChatMemory memory;

    public ChatBot() {
        var url = config.getValue("WATSONX_URL", String.class);
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);
        var defaultParameters = ChatParameters.builder().maxCompletionTokens(0).build();

        toolRegistry = ToolRegistry.builder()
            .register(new SendEmailTool(), new TimeTool())
            .build();

        memory = new ChatMemory();
        memory.addMessage(SystemMessage.of("""
            You are a helpful assistant. Your task is to answer user questions.
            If you need to use tools, ensure the user gives you the necessary arguments to execute tool calls.
            Since the response will be written in a CLI, keep your answers concise to ensure readability."""));

        chatService = ChatService.builder()
            .baseUrl(url)
            .projectId(projectId)
            .apiKey(apiKey)
            .modelId("ibm/granite-4-h-small")
            .parameters(defaultParameters)
            .tools(toolRegistry.tools())
            .toolInterceptor((ctx, fc) -> Json.isValidObject(fc.arguments()) ? fc : sanitize(ctx, fc))
            .build();
    }


    /**
     * Processes a chat message and returns the assistant's response asynchronously.
     *
     * @param message the user's input message to process
     * @return a CompletableFuture that completes with the final AssistantMessage containing the assistant's response. The future may complete
     *         exceptionally if an error occurs during processing.
     */
    public CompletableFuture<AssistantMessage> chat(String message) {

        memory.addMessage(UserMessage.text(message));

        var future = new CompletableFuture<AssistantMessage>();
        chatService.chatStreaming(memory.getMemory(), new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                var assistantMessage = completeResponse.toAssistantMessage();
                memory.addMessage(assistantMessage);

                if (!assistantMessage.hasToolCalls()) {
                    future.complete(assistantMessage);
                    return;

                } else {

                    var toolMessages = assistantMessage.processTools(toolRegistry::execute);
                    memory.addMessages(toolMessages);

                    try {
                        assistantMessage = chatService.chatStreaming(memory.getMemory(), this).get().toAssistantMessage();
                        future.complete(assistantMessage);
                    } catch (Exception e) {
                        var err = nonNull(e.getCause()) ? e.getCause() : e;
                        future.completeExceptionally(err);
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }

            @Override
            public boolean failOnFirstError() {
                return true;
            }
        });

        return future;
    }

    private FunctionCall sanitize(InterceptorContext ctx, FunctionCall fc) {
        logger.info("Invoked sanitize");

        String toolName = fc.name();
        String arguments = fc.arguments();
        ChatRequest request = ctx.request();
        ChatParameters parameters = ChatParameters.builder().responseAsJson().build();

        String schema = request.tools().stream()
            .map(Tool::function)
            .filter(f -> f.name().equals(toolName)).findFirst()
            .map(Json::prettyPrint)
            .orElseThrow(() -> new IllegalArgumentException("Tool not found"));

        String conversation = request.messages().stream()
            .filter(m -> m instanceof UserMessage || (m instanceof AssistantMessage am && !am.hasToolCalls()))
            .map(m -> {

                if (m instanceof UserMessage userMessage)
                    return "User: %s\n".formatted(userMessage.text());
                else if (m instanceof AssistantMessage assistantMessage && !assistantMessage.hasToolCalls())
                    return "Assistant: %s\n".formatted(assistantMessage.content());

                // Should never happen.
                throw new IllegalArgumentException("Message type not supported");
            })
            .collect(Collectors.joining("\n"));

        List<ChatMessage> messages = List.of(
            SystemMessage.of("""
                You are a helpful assistant. Your task is to sanitize the input json.
                Analyze the conversation with the user to understand what are the json fields that need to be sanitized.

                Conversation:
                %s""".formatted(conversation)),
            UserMessage.text("""
                Fix the following json: %s
                The schema of the tool is: %s""".formatted(arguments, schema))
        );

        String fixedJson = chatService.chat(messages, parameters).toAssistantMessage().content();
        JsonNode jsonNode = Json.fromJson(fixedJson, JsonNode.class);
        return FunctionCall.of(toolName, jsonNode.toString());
    }
}