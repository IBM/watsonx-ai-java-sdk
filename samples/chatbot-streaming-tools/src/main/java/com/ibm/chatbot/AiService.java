/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;

public class AiService {

    private static final Config config = ConfigProvider.getConfig();
    private static final String DEFAULT_MODEL_ID = "mistralai/mistral-small-3-1-24b-instruct-2503";
    private static final String SYSTEM_MESSAGE = """
        You are a helpful assistant. Your task is to answer user questions.
        Since the response will be written in a CLI, keep your answers concise to ensure readability.""";

    private static final Tool EMAIL_TOOL = Tool.of(
        "send_email",
        "Send an email",
        JsonSchema.object()
            .property("email", JsonSchema.string())
            .property("subject", JsonSchema.string())
            .property("body", JsonSchema.string())
            .required("email", "subject", "body")
    );

    private final String modelId;
    private final ChatService chatService;
    private final FoundationModelService foundationModelService;
    private final ChatMemory memory;

    public AiService() {
        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);
        modelId = config.getOptionalValue("WATSONX_MODEL_ID", String.class).orElse(DEFAULT_MODEL_ID);

        chatService = ChatService.builder()
            .apiKey(apiKey)
            .projectId(projectId)
            .modelId(modelId)
            .baseUrl(url)
            .build();

        foundationModelService = FoundationModelService.builder()
            .apiKey(apiKey)
            .baseUrl(url)
            .build();

        memory = new ChatMemory();
        memory.addMessage(SystemMessage.of(SYSTEM_MESSAGE));
    }

    public CompletableFuture<AssistantMessage> chat(String message, Consumer<String> onPartialResponse) {

        memory.addMessage(UserMessage.text(message));
        Map<String, ToolMessage> toolMessages = new ConcurrentHashMap<>();

        var future = new CompletableFuture<AssistantMessage>();
        chatService.chatStreaming(memory.getMemory(), List.of(EMAIL_TOOL), new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                onPartialResponse.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                var assistantMessage = completeResponse.toAssistantMessage();
                memory.addMessage(assistantMessage);

                if (!assistantMessage.hasToolCalls()) {
                    future.complete(assistantMessage);
                    return;
                } else {

                    assistantMessage.toolCalls().stream()
                        .map(toolCall -> toolCall.id())
                        .map(toolMessages::get)
                        .forEach(memory::addMessage);

                    chatService
                        .chatStreaming(memory.getMemory(), this)
                        .whenComplete((res, err) -> {
                            if (err != null)
                                future.completeExceptionally(err);
                            else
                                future.complete(assistantMessage);
                        });
                }
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                var toolMessage = completeToolCall.processTool(
                    (name, args) -> Tools.sendEmail(args.get("email"), args.get("subject"), args.get("body"))
                );
                toolMessages.put(completeToolCall.toolCall().id(), toolMessage);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });

        return future;
    }

    public FoundationModel getModelDetails() {
        return foundationModelService.getModel(modelId).orElseThrow();
    }
}