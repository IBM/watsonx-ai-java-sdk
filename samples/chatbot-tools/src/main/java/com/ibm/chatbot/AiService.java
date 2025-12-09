/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.chatbot;

import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import java.net.URI;
import java.time.Duration;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
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
            .property("emails", JsonSchema.array().items(JsonSchema.string()))
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
        modelId = config.getOptionalValue("WATSONX_MODEL_ID", String.class).orElse("ibm/granite-4-h-small");

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
        memory.addMessage(assistantMessage);

        if (assistantMessage.hasToolCalls()) {
            var toolMessages =
                assistantMessage.processTools((name, args) -> Tools.sendEmail(args.get("emails"), args.get("subject"), args.get("body")));
            memory.addMessages(toolMessages);
            assistantMessage = chatService.chat(memory.getMemory(), parameters, EMAIL_TOOL).toAssistantMessage();
        }

        return assistantMessage.content();
    }

    public FoundationModel getModel() {
        return foundationModelService.getModels(Filter.of(modelId(modelId))).resources().get(0);
    }
}
