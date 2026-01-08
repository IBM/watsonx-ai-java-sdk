/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import static com.ibm.watsonx.ai.core.Utils.getOrDefault;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.chat.ChatClientContext;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatProvider;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.ExecutableTool;
import com.ibm.watsonx.ai.chat.interceptor.InterceptorContext;
import com.ibm.watsonx.ai.chat.interceptor.MessageInterceptor;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.core.auth.Authenticator;
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
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .build();
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class DeploymentService extends WatsonxService implements ChatProvider, TextGenerationProvider, TimeSeriesProvider {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);
    private final DeploymentRestClient client;
    private final MessageInterceptor messageInterceptor;
    private final ToolInterceptor toolInterceptor;
    private final ChatProvider chatProvider;
    private final ChatParameters defaultParameters;
    private final List<Tool> defaultTools;

    private DeploymentService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        messageInterceptor = builder.messageInterceptor;
        toolInterceptor = builder.toolInterceptor;
        defaultTools = builder.defaultTools;
        defaultParameters = requireNonNullElse(builder.defaultParameters, ChatParameters.builder().build());

        client = DeploymentRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();

        if (nonNull(messageInterceptor) || nonNull(toolInterceptor)) {
            chatProvider = new Builder()
                .authenticator(builder.authenticator())
                .baseUrl(baseUrl)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(timeout)
                .version(version)
                .parameters(defaultParameters)
                .httpClient(httpClient)
                .verifySsl(verifySsl)
                .build();
        } else
            chatProvider = null;
    }

    /**
     * Retrieves deployment details with the specified parameters.
     *
     * @param parameters the {@link FindByIdRequest} specifying a different deployment, project or space ID, and other options
     * @return the {@link DeploymentResource} with the retrieved deployment details
     */
    public DeploymentResource findById(FindByIdRequest parameters) {
        requireNonNull(parameters, "FindByIdRequest cannot be null");
        requireNonNull(parameters.deploymentId(), "deploymentId must be provided");

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            throw new IllegalArgumentException("Either projectId or spaceId must be provided");

        return client.findById(parameters);
    }

    @Override
    public TextGenerationResponse generate(TextGenerationRequest textGenerationRequest) {
        requireNonNull(textGenerationRequest, "textGenerationRequest cannot be null");

        // The input parameter can be null, if the prompt_template is used.

        var input = textGenerationRequest.input();
        var moderation = textGenerationRequest.moderation();
        var deploymentId = requireNonNull(textGenerationRequest.deploymentId(), "deploymentId must be provided");
        var parameters = requireNonNullElse(textGenerationRequest.parameters(), TextGenerationParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(parameters.timeLimit(), this.timeout.toMillis()));

        logIgnoredParameters(parameters.modelId(), parameters.projectId(), parameters.spaceId());

        var textRequest =
            new TextRequest(null, null, null, input, parameters.toSanitized(), moderation);

        return client.generate(parameters.transactionId(), deploymentId, timeout, textRequest);
    }

    @Override
    public CompletableFuture<Void> generateStreaming(TextGenerationRequest textGenerationRequest, TextGenerationHandler handler) {
        requireNonNull(textGenerationRequest, "textGenerationRequest cannot be null");

        // The input parameter can be null, if the prompt_template is used.

        var input = textGenerationRequest.input();
        var deploymentId = requireNonNull(textGenerationRequest.deploymentId(), "deploymentId must be provided");
        var parameters = requireNonNullElse(textGenerationRequest.parameters(), TextGenerationParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(parameters.timeLimit(), this.timeout.toMillis()));

        logIgnoredParameters(parameters.modelId(), parameters.projectId(), parameters.spaceId());

        var textGenRequest =
            new TextRequest(null, null, null, input, parameters.toSanitized(), null);

        return client.generateStreaming(parameters.transactionId(), deploymentId, timeout, textGenRequest, handler);
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");

        var deploymentId = requireNonNull(chatRequest.deploymentId(), "deploymentId must be provided");
        var textChatRequest = buildTextChatRequest(chatRequest);
        var transactionId = nonNull(chatRequest.parameters()) ? chatRequest.parameters().transactionId() : null;
        var timeout = nonNull(chatRequest.parameters()) && nonNull(chatRequest.parameters().timeLimit())
            ? chatRequest.parameters().timeLimit()
            : this.timeout.toMillis();

        var chatResponse = client.chat(transactionId, deploymentId, Duration.ofMillis(timeout), textChatRequest);

        if (nonNull(messageInterceptor)) {
            var newChoices = messageInterceptor.intercept(new InterceptorContext(chatProvider, chatRequest, chatResponse));
            chatResponse = chatResponse.toBuilder()
                .choices(newChoices)
                .build();
        }

        if (nonNull(toolInterceptor)) {
            var newChoices = toolInterceptor.intercept(new InterceptorContext(chatProvider, chatRequest, chatResponse));
            chatResponse = chatResponse.toBuilder()
                .choices(newChoices)
                .build();
        }

        return chatResponse;
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(ChatRequest chatRequest, ChatHandler handler) {
        requireNonNull(chatRequest, "chatRequest cannot be null");
        requireNonNull(handler, "The chatHandler parameter can not be null");

        var deploymentId = requireNonNull(chatRequest.deploymentId(), "deploymentId must be provided");
        var textChatRequest = buildTextChatRequest(chatRequest);
        var extractionTags = nonNull(chatRequest.thinking()) ? chatRequest.thinking().extractionTags() : null;
        var transactionId = nonNull(chatRequest.parameters()) ? chatRequest.parameters().transactionId() : null;
        var context = ChatClientContext.builder()
            .chatProvider(chatProvider)
            .chatRequest(chatRequest)
            .toolInterceptor(toolInterceptor)
            .extractionTags(extractionTags)
            .build();

        return client.chatStreaming(transactionId, deploymentId, textChatRequest, context, handler);
    }

    @Override
    public ForecastResponse forecast(TimeSeriesRequest timeSeriesRequest) {
        requireNonNull(timeSeriesRequest, "timeSeriesRequest cannot be null");

        var deploymentId = requireNonNull(timeSeriesRequest.deploymentId(), "deploymentId must be provided");
        var inputSchema = timeSeriesRequest.inputSchema();
        var data = timeSeriesRequest.data();
        var parameters = timeSeriesRequest.parameters();

        Parameters requestParameters = null;
        Map<String, List<Object>> futureData = null;
        String transactionId = null;

        if (nonNull(parameters)) {
            logIgnoredParameters(parameters.modelId(), parameters.projectId(), parameters.spaceId());
            requestParameters = parameters.toParameters();
            futureData = ofNullable(parameters.futureData()).map(p -> p.asMap()).orElse(null);
            transactionId = parameters.transactionId();
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
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link TextChatRequest} from the provided {@link ChatRequest}.
     *
     * @param chatRequest the {@link ChatRequest} object
     * @return a fully constructed {@link TextChatRequest} object
     */
    private TextChatRequest buildTextChatRequest(ChatRequest chatRequest) {
        var messages = chatRequest.messages();
        var tools = isNull(chatRequest.tools()) ? defaultTools : chatRequest.tools();
        tools = nonNull(tools) && !tools.isEmpty() ? tools : null;
        var parameters = requireNonNullElse(chatRequest.parameters(), ChatParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(defaultParameters.timeLimit(), this.timeout.toMillis()));

        Boolean includeReasoning = null;
        String thinkingEffort = null;
        Map<String, Object> chatTemplateKwargs = null;
        if (nonNull(chatRequest.thinking())) {
            var thinking = chatRequest.thinking();
            chatTemplateKwargs = Map.of("thinking", true);
            includeReasoning = thinking.includeReasoning();
            thinkingEffort = nonNull(thinking.thinkingEffort()) ? thinking.thinkingEffort().getValue() : null;
        }

        logIgnoredParameters(parameters.modelId(), parameters.projectId(), parameters.spaceId());

        return TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .toolChoiceOption(getOrDefault(parameters.toolChoiceOption(), defaultParameters.toolChoiceOption()))
            .toolChoice(getOrDefault(parameters.toolChoice(), defaultParameters.toolChoice()))
            .frequencyPenalty(getOrDefault(parameters.frequencyPenalty(), defaultParameters.frequencyPenalty()))
            .logitBias(getOrDefault(parameters.logitBias(), defaultParameters.logitBias()))
            .logprobs(getOrDefault(parameters.logprobs(), defaultParameters.logprobs()))
            .topLogprobs(getOrDefault(parameters.topLogprobs(), defaultParameters.topLogprobs()))
            .maxCompletionTokens(getOrDefault(parameters.maxCompletionTokens(), defaultParameters.maxCompletionTokens()))
            .n(getOrDefault(parameters.n(), defaultParameters.n()))
            .presencePenalty(getOrDefault(parameters.presencePenalty(), defaultParameters.presencePenalty()))
            .seed(getOrDefault(parameters.seed(), defaultParameters.seed()))
            .stop(getOrDefault(parameters.stop(), defaultParameters.stop()))
            .temperature(getOrDefault(parameters.temperature(), defaultParameters.temperature()))
            .topP(getOrDefault(parameters.topP(), defaultParameters.topP()))
            .responseFormat(getOrDefault(parameters.responseFormat(), defaultParameters.responseFormat()))
            .jsonSchema(getOrDefault(parameters.jsonSchema(), defaultParameters.jsonSchema()))
            .context(getOrDefault(parameters.context(), defaultParameters.context()))
            .timeLimit(getOrDefault(parameters.timeLimit(), timeout.toMillis()))
            .guidedChoice(getOrDefault(parameters.guidedChoice(), defaultParameters.guidedChoice()))
            .guidedRegex(getOrDefault(parameters.guidedRegex(), defaultParameters.guidedRegex()))
            .guidedGrammar(getOrDefault(parameters.guidedGrammar(), defaultParameters.guidedGrammar()))
            .repetitionPenalty(getOrDefault(parameters.repetitionPenalty(), defaultParameters.repetitionPenalty()))
            .lengthPenalty(getOrDefault(parameters.lengthPenalty(), defaultParameters.lengthPenalty()))
            .includeReasoning(includeReasoning)
            .reasoningEffort(thinkingEffort)
            .chatTemplateKwargs(chatTemplateKwargs)
            .build();
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
        private ChatParameters defaultParameters;
        private List<Tool> defaultTools;

        private Builder() {}

        /**
         * Sets the default {@link ChatParameters} that will be applied to all chat requests when no specific parameters are provided.
         * <p>
         * These default values serve as fallbacks for any parameter not explicitly set. When parameters are provided in the chat method call, they
         * will take precedence over these default parameters.
         *
         * @param parameters the default chat parameters to use
         */
        public Builder parameters(ChatParameters parameters) {
            this.defaultParameters = parameters;
            return this;
        }

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
         * @param toolInterceptor the interceptor to apply
         */
        public Builder toolInterceptor(ToolInterceptor toolInterceptor) {
            this.toolInterceptor = toolInterceptor;
            return this;
        }

        /**
         * Sets the default tools that will be available to the model during chat interactions.
         *
         * @param executableTools list of {@link ExecutableTool} objects to set as defaults
         */
        public Builder tools(ExecutableTool... executableTools) {
            return tools(Arrays.stream(executableTools).map(ExecutableTool::schema).toList());
        }

        /**
         * Sets the default tools that will be available to the model during chat interactions.
         * <p>
         * These tools will be used in all chat requests unless explicitly overridden by passing tools directly to the chat methods. If tools are
         * provided in the chat method call, they will take precedence over these default tools.
         *
         * @param tools list of {@link Tool} objects to set as defaults
         */
        public Builder tools(Tool... tools) {
            return tools(Arrays.asList(tools));
        }

        /**
         * Sets the default tools that will be available to the model during chat interactions.
         * <p>
         * These tools will be used in all chat requests unless explicitly overridden by passing tools directly to the chat methods. If tools are
         * provided in the chat method call, they will take precedence over these default tools.
         *
         * @param tools list of {@link Tool} objects to set as defaults
         */
        public Builder tools(List<Tool> tools) {
            this.defaultTools = tools;
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
