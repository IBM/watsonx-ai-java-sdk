/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import static java.util.Objects.nonNull;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.chatbot.Tools.SendEmailArguments;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.AssistantMessage;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.ToolMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.foundationmodel.FoundationModel;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

public class AiService {

    private static final Config config = ConfigProvider.getConfig();
    private static final String SYSTEM_MESSAGE = """
        You are a helpful assistant. Your task is to answer user questions.
        Since the response will be written in a CLI, keep your answers concise to ensure readability.""";

    private static final Tool EMAIL_TOOL = Tool.of(
        "send_email",
        "Send an email to one or more users",
        JsonSchema.object()
            .property("email", JsonSchema.string())
            .property("subject", JsonSchema.string())
            .property("body", JsonSchema.string())
            .required("email", "subject", "body")
    );

    private String modelId;
    private final ChatService chatService;
    private final FoundationModelService foundationModelService;
    private final ChatMemory memory;

    public AiService() {
        var url = URI.create(config.getValue("WATSONX_URL", String.class));
        var apiKey = config.getValue("WATSONX_API_KEY", String.class);
        var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);
        modelId = config.getOptionalValue("WATSONX_MODEL_ID", String.class).orElse("mistralai/mistral-medium-2505");

        chatService = ChatService.builder()
            .apiKey(apiKey)
            .projectId(projectId)
            .timeout(Duration.ofSeconds(60))
            .modelId(modelId)
            .baseUrl(url)
            .build();

        foundationModelService = FoundationModelService.builder()
            .apiKey(apiKey)
            .baseUrl(url)
            .timeout(Duration.ofSeconds(60))
            .build();

        memory = new ChatMemory();
        memory.addMessage(SystemMessage.of(SYSTEM_MESSAGE));
    }

    public String chat(String userInput) {
        memory.addMessage(UserMessage.text(userInput));

        var parameters = ChatParameters.builder()
            .maxCompletionTokens(0)
            .temperature(0.0)
            .build();

        var response = chatService.chat(memory.getMemory(), parameters, EMAIL_TOOL);
        var assistantMessage = response.toAssistantMessage();

        if (nonNull(assistantMessage.toolCalls())) {
            assistantMessage = handleToolCalls(assistantMessage, parameters);
        }

        memory.addMessage(assistantMessage);
        return assistantMessage.content();
    }

    private AssistantMessage handleToolCalls(AssistantMessage assistantMessage, ChatParameters parameters) {
        List<ChatMessage> messages = new ArrayList<>(memory.getMemory());
        messages.add(assistantMessage);

        for (var toolCall : assistantMessage.toolCalls()) {
            var args = Json.fromJson(toolCall.function().arguments(), SendEmailArguments.class);
            var result = Tools.sendEmail(args.emails(), args.subject(), args.body());
            messages.add(ToolMessage.of(String.valueOf(result), toolCall.id()));
        }

        return chatService.chat(messages, parameters).toAssistantMessage();
    }

    public FoundationModel getModel() {
        return foundationModelService.getModels(Filter.of(modelId(modelId))).resources().get(0);
    }
}
