/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatProvider;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.interceptor.MessageInterceptor;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters;
import com.ibm.watsonx.ai.textgeneration.TextGenerationProvider;
import com.ibm.watsonx.ai.textgeneration.TextGenerationRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.textgeneration.TextRequest;
import com.ibm.watsonx.ai.timeseries.ForecastRequest;
import com.ibm.watsonx.ai.timeseries.ForecastRequest.Parameters;
import com.ibm.watsonx.ai.timeseries.ForecastResponse;
import com.ibm.watsonx.ai.timeseries.TimeSeriesProvider;
import com.ibm.watsonx.ai.timeseries.TimeSeriesRequest;

/**
 * Service class to interact with IBM watsonx.ai Deployment APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * DeploymentService deploymentService = DeploymentService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
 *     .build();
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}.
 *
 * @see AuthenticationProvider
 */
public class DeploymentService extends WatsonxService implements ChatProvider, TextGenerationProvider, TimeSeriesProvider {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);
    private final DeploymentRestClient client;
    private final MessageInterceptor messageInterceptor;
    private final ToolInterceptor toolInterceptor;

    private DeploymentService(Builder builder) {
        super(builder);
        client = DeploymentRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticationProvider(builder.getAuthenticationProvider())
            .build();
        messageInterceptor = builder.messageInterceptor;
        toolInterceptor = builder.toolInterceptor;
    }

    /**
     * Retrieves deployment details with the specified parameters.
     *
     * @param parameters the {@link FindByIdRequest} specifying a different deployment, project or space ID, and other options
     * @return the {@link DeploymentResource} with the retrieved deployment details
     */
    public DeploymentResource findById(FindByIdRequest parameters) {
        requireNonNull(parameters, "FindByIdRequest cannot be null");
        requireNonNull(parameters.getDeploymentId(), "deploymentId must be provided");

        if (isNull(parameters.getProjectId()) && isNull(parameters.getSpaceId()))
            throw new IllegalArgumentException("Either projectId or spaceId must be provided");

        return client.findById(parameters);
    }

    @Override
    public TextGenerationResponse generate(TextGenerationRequest textGenerationRequest) {
        requireNonNull(textGenerationRequest, "textGenerationRequest cannot be null");

        // The input parameter can be null, if the prompt_template is used.

        var input = textGenerationRequest.getInput();
        var moderation = textGenerationRequest.getModeration();
        var deploymentId = requireNonNull(textGenerationRequest.getDeploymentId(), "deploymentId must be provided");
        var parameters = requireNonNullElse(textGenerationRequest.getParameters(), TextGenerationParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis()));

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textRequest =
            new TextRequest(null, null, null, input, parameters.toSanitized(), moderation);

        return client.generate(parameters.getTransactionId(), deploymentId, timeout, textRequest);
    }

    @Override
    public CompletableFuture<Void> generateStreaming(TextGenerationRequest textGenerationRequest, TextGenerationHandler handler) {
        requireNonNull(textGenerationRequest, "textGenerationRequest cannot be null");

        // The input parameter can be null, if the prompt_template is used.

        var input = textGenerationRequest.getInput();
        var deploymentId = requireNonNull(textGenerationRequest.getDeploymentId(), "deploymentId must be provided");
        var parameters = requireNonNullElse(textGenerationRequest.getParameters(), TextGenerationParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis()));

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textGenRequest =
            new TextRequest(null, null, null, input, parameters.toSanitized(), null);

        return client.generateStreaming(parameters.getTransactionId(), deploymentId, timeout, textGenRequest, handler);
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");

        var deploymentId = requireNonNull(chatRequest.getDeploymentId(), "deploymentId must be provided");
        var messages = chatRequest.getMessages();
        var tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        var parameters = requireNonNullElse(chatRequest.getParameters(), ChatParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis()));

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textChatRequest = TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .timeLimit(timeout.toMillis())
            .build();

        var chatResponse = client.chat(
            parameters.getTransactionId(),
            deploymentId,
            timeout,
            textChatRequest);

        if (nonNull(messageInterceptor))
            chatResponse.setChoices(messageInterceptor.intercept(chatRequest, chatResponse));

        if (nonNull(toolInterceptor))
            chatResponse.setChoices(toolInterceptor.intercept(chatRequest, chatResponse));

        // Watsonx doesn't return "tool_calls" when the tool-choice-option is set to REQUIRED.
        if (nonNull(parameters.getToolChoiceOption()) && parameters.getToolChoiceOption().equals(ToolChoiceOption.REQUIRED.type())) {
            var assistantMessage = chatResponse.toAssistantMessage();
            if (nonNull(assistantMessage.toolCalls()) && !assistantMessage.toolCalls().isEmpty())
                chatResponse.getChoices().get(0).setFinishReason("tool_calls");
        }

        return chatResponse;
    }

    @Override
    public CompletableFuture<Void> chatStreaming(ChatRequest chatRequest, ChatHandler handler) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        requireNonNull(handler, "The chatHandler parameter can not be null");

        var deploymentId = requireNonNull(chatRequest.getDeploymentId(), "deploymentId must be provided");
        var messages = chatRequest.getMessages();
        var tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        var parameters = requireNonNullElse(chatRequest.getParameters(), ChatParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis()));

        Boolean includeReasoning = null;
        String thinkingEffort = null;
        ExtractionTags extractionTags = null;
        Map<String, Object> chatTemplateKwargs = null;
        if (nonNull(chatRequest.getThinking())) {
            var thinking = chatRequest.getThinking();
            chatTemplateKwargs = Map.of("thinking", true);
            extractionTags = thinking.getExtractionTags();
            includeReasoning = thinking.getIncludeReasoning();
            thinkingEffort = nonNull(thinking.getThinkingEffort()) ? thinking.getThinkingEffort().getValue() : null;
        }

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textChatRequest = TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .includeReasoning(includeReasoning)
            .reasoningEffort(thinkingEffort)
            .chatTemplateKwargs(chatTemplateKwargs)
            .timeLimit(timeout.toMillis())
            .build();

        return client.chatStreaming(parameters.getTransactionId(), deploymentId, timeout, extractionTags, textChatRequest, new ChatHandler() {

            @Override
            public void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse) {
                handler.onPartialResponse(partialResponse, partialChatResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (nonNull(toolInterceptor))
                    completeResponse.setChoices(toolInterceptor.intercept(chatRequest, completeResponse));
                handler.onCompleteResponse(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                handler.onError(error);
            }

            @Override
            public void onCompleteToolCall(CompletedToolCall completeToolCall) {
                if (nonNull(toolInterceptor))
                    completeToolCall = toolInterceptor.intercept(completeToolCall);
                handler.onCompleteToolCall(completeToolCall);
            }

            @Override
            public void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {
                handler.onPartialThinking(partialThinking, partialChatResponse);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                handler.onPartialToolCall(partialToolCall);
            }
        });
    }

    @Override
    public ForecastResponse forecast(TimeSeriesRequest timeSeriesRequest) {
        requireNonNull(timeSeriesRequest, "timeSeriesRequest cannot be null");

        var deploymentId = requireNonNull(timeSeriesRequest.getDeploymentId(), "deploymentId must be provided");
        var inputSchema = timeSeriesRequest.getInputSchema();
        var data = timeSeriesRequest.getData();
        var parameters = timeSeriesRequest.getParameters();

        Parameters requestParameters = null;
        Map<String, List<Object>> futureData = null;
        String transactionId = null;

        if (nonNull(parameters)) {
            logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());
            requestParameters = parameters.toParameters();
            futureData = ofNullable(parameters.getFutureData()).map(p -> p.asMap()).orElse(null);
            transactionId = parameters.getTransactionId();
        }

        var forecastRequest = new ForecastRequest(null, null, null, data.asMap(), inputSchema, futureData, requestParameters);

        return client.forecast(transactionId, deploymentId, timeout, forecastRequest);
    }


    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * DeploymentService deploymentService = DeploymentService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * This method helps notify developers when they set parameters that are not applicable to deployments.
     *
     * @param modelId the model identifier, ignored in deployment context
     * @param projectId the project identifier, ignored in deployment context
     * @param spaceId the space identifier, ignored in deployment context
     */
    private void logIgnoredParameters(String modelId, String projectId, String spaceId) {
        if (nonNull(modelId))
            logger.info("The modelId parameter is ignored for the DeploymentService");

        if (nonNull(projectId))
            logger.info("The projectId parameter is ignored for the DeploymentService");

        if (nonNull(spaceId))
            logger.info("The spaceId parameter is ignored for the DeploymentService");
    }

    /**
     * Builder class for constructing {@link DeploymentService} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxService.Builder<Builder> {
        private MessageInterceptor messageInterceptor;
        private ToolInterceptor toolInterceptor;

        private Builder() {}

        /**
         * Registers a {@link MessageInterceptor} used to modify or sanitize the assistant's textual content before it is returned to the caller.
         * <p>
         * This interceptor is invoked on the final aggregated content (non-streaming responses only), allowing adjustments such as rewriting,
         * filtering, or normalization.
         * <p>
         * <b>Example:</b>
         *
         * <pre>{@code
         * ChatService.builder()
         *     .messageInterceptor((request, content) -> content.replace("error", "issue"));
         * }</pre>
         *
         * @param messageInterceptor the interceptor to apply
         */
        public Builder messageInterceptor(MessageInterceptor messageInterceptor) {
            this.messageInterceptor = messageInterceptor;
            return this;
        }

        /**
         * Registers a {@link ToolInterceptor} to modify or normalize function call arguments before tool execution.
         * <p>
         * This interceptor is applied to every function call appearing in the assistant message. It is invoked both for standard responses and for
         * aggregated tool-call data in streaming mode.
         * <p>
         * <b>Example:</b>
         *
         * <pre>{@code
         * ChatService.builder()
         *     .toolInterceptor((request, fc) -> {
         *         var args = fc.arguments();
         *         return (nonNull(args) && args.startsWith("\""))
         *             ? fc.withArguments(Json.fromJson(args, String.class))
         *             : fc;
         *     });
         * }</pre>
         *
         * @param toolInterceptor the interceptor to apply (may be {@code null})
         */
        public Builder toolInterceptor(ToolInterceptor toolInterceptor) {
            this.toolInterceptor = toolInterceptor;
            return this;
        }

        /**
         * Builds a {@link DeploymentService} instance using the configured parameters.
         *
         * @return a new instance of {@link DeploymentService}
         */
        public DeploymentService build() {
            return new DeploymentService(this);
        }
    }
}
