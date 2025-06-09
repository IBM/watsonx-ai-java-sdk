package com.ibm.watsonx.runtime.chat;

import static com.ibm.watsonx.core.Json.fromJson;
import static com.ibm.watsonx.core.Json.toJson;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.List;
import com.ibm.watsonx.runtime.WatsonxService;
import com.ibm.watsonx.runtime.chat.model.ChatMessage;
import com.ibm.watsonx.runtime.chat.model.ChatParameters;
import com.ibm.watsonx.runtime.chat.model.ChatRequest;
import com.ibm.watsonx.runtime.chat.model.ChatResponse;
import com.ibm.watsonx.runtime.chat.model.Tool;

public final class ChatService extends WatsonxService {

    public ChatService(Builder builder) {
        super(builder);
    }

    public ChatResponse chat(ChatMessage... messages) {
        return chat(Arrays.asList(messages), null, null);
    }

    public ChatResponse chat(List<ChatMessage> messages) {
        return chat(messages, null, null);
    }

    public ChatResponse chat(List<ChatMessage> messages, List<Tool> tools) {
        return chat(messages, tools, null);
    }

    public ChatResponse chat(List<ChatMessage> messages, Tool... tools) {
        return chat(messages, Arrays.asList(tools), null);
    }

    public ChatResponse chat(List<ChatMessage> messages, ChatParameters parameters) {
        return chat(messages, null, parameters);
    }

    public ChatResponse chat(List<ChatMessage> messages, List<Tool> tools, ChatParameters parameters) {

        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());
        var modelId = requireNonNullElse(parameters.modelId(), this.modelId);
        var projectId = nonNull(parameters.projectId()) ? parameters.projectId() : this.projectId;
        var spaceId = nonNull(parameters.spaceId()) ? parameters.spaceId() : this.spaceId;

        if (isNull(projectId) && isNull(spaceId))
            throw new RuntimeException("Either projectId or spaceId must be provided");

        var chatRequest = ChatRequest.builder()
            .modelId(modelId)
            .projectId(projectId)
            .spaceId(spaceId)
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .timeLimit(isNull(parameters.timeLimit()) && nonNull(timeout) ? timeout.toMillis() : parameters.timeLimit())
            .build();

        var httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString() + "/ml/v1/text/chat?version=%s".formatted(version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(chatRequest)))
                .build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpReponse.body(), ChatResponse.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void streamMessages(List<ChatMessage> messages, ChatParameters parameters) {

    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ChatService} instances with configurable parameters.
     */
    public static class Builder extends WatsonxService.Builder<Builder>  {

        /**
         * Builds a {@link ChatService} instance using the configured parameters.
         *
         * @return a new instance of {@link ChatService}
         */
        public ChatService build() {
            return new ChatService(this);
        }
    }
}
