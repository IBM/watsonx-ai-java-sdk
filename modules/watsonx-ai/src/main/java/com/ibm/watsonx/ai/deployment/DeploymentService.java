/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatProvider;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.ChatMessage;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoice;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.chat.model.Tool;
import com.ibm.watsonx.ai.chat.util.StreamingStateTracker;
import com.ibm.watsonx.ai.core.SseEventLogger;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.textgeneration.Moderation;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters;
import com.ibm.watsonx.ai.textgeneration.TextGenerationProvider;
import com.ibm.watsonx.ai.textgeneration.TextGenerationRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.timeseries.ForecastData;
import com.ibm.watsonx.ai.timeseries.ForecastRequest;
import com.ibm.watsonx.ai.timeseries.ForecastRequest.Parameters;
import com.ibm.watsonx.ai.timeseries.ForecastResponse;
import com.ibm.watsonx.ai.timeseries.InputSchema;
import com.ibm.watsonx.ai.timeseries.TimeSeriesParameters;
import com.ibm.watsonx.ai.timeseries.TimeSeriesProvider;

/**
 * Service class to interact with IBM watsonx.ai Deployment APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * DeploymentService deploymentService = DeploymentService.builder()
 *     .url("https://...") // or use CloudRegion
 *     .deployment("...")
 *     .authenticationProvider(authProvider)
 *     .build();
 *
 * ChatResponse response = deploymentService.chat(
 *     UserMessage.text("Tell me a joke")
 * );
 * }</pre>
 *
 * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#deployments-text-chat" target="_blank"> official documentation</a>.
 *
 * @see AuthenticationProvider
 */
public class DeploymentService extends WatsonxService implements ChatProvider, TextGenerationProvider, TimeSeriesProvider {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);
    private final String deployment;

    protected DeploymentService(Builder builder) {
        super(builder);
        deployment = requireNonNull(builder.deployment, "deployment cannot be null");
    }

    /**
     * Retrieves deployment details with the specified parameters.
     *
     * @param parameters the {@link FindByIdParameters} specifying a different deployment, project or space ID, and other options
     * @return the {@link DeploymentResource} with the retrieved deployment details
     */
    public DeploymentResource findById(FindByIdParameters parameters) {

        parameters = requireNonNullElse(parameters, FindByIdParameters.builder().build());
        var deploymentId = requireNonNullElse(parameters.getDeployment(), deployment);

        StringJoiner queryParameters = new StringJoiner("&", "?", "");

        if (nonNull(parameters.getProjectId()))
            queryParameters.add("project_id=".concat(parameters.getProjectId()));

        if (nonNull(parameters.getSpaceId()))
            queryParameters.add("space_id=".concat(parameters.getSpaceId()));

        if (queryParameters.length() == 1)
            throw new IllegalArgumentException("Either projectId or spaceId must be provided");

        queryParameters.add("version=".concat(version));

        var httpRequest = HttpRequest
            .newBuilder(URI.create(url.toString() + "/ml/v4/deployments/%s%s".formatted(deploymentId, queryParameters.toString())))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(timeout.toMillis()))
            .GET();

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), DeploymentResource.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TextGenerationResponse generate(String input, Moderation moderation, TextGenerationParameters parameters) {

        // The input parameter can be null, if the prompt_template is used.

        parameters = requireNonNullElse(parameters, TextGenerationParameters.builder().build());
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());
        parameters.setTimeLimit(timeout);

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textGenerationRequest =
            new TextGenerationRequest(null, null, null, input, parameters.toSanitized(), moderation);

        var httpRequest = HttpRequest
            .newBuilder(URI.create(url.toString() + "%s/deployments/%s/text/generation?version=%s".formatted(ML_API_PATH, deployment, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(Duration.ofMillis(timeout))
            .POST(BodyPublishers.ofString(toJson(textGenerationRequest)));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TextGenerationResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> generateStreaming(String input, TextGenerationParameters parameters, TextGenerationHandler handler) {

        // The input parameter can be null, if the prompt_template is used.

        parameters = requireNonNullElse(parameters, TextGenerationParameters.builder().build());

        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());
        parameters.setTimeLimit(timeout);

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textGenerationRequest =
            new TextGenerationRequest(null, null, null, input, parameters.toSanitized(), null);

        var httpRequest = HttpRequest
            .newBuilder(
                URI.create(url.toString() + "%s/deployments/%s/text/generation_stream?version=%s".formatted(ML_API_PATH, deployment, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .timeout(Duration.ofMillis(timeout))
            .POST(BodyPublishers.ofString(toJson(textGenerationRequest)));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        var subscriber = subscriber(handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAcceptAsync(r -> {}, asyncHttpClient.executor())
            .exceptionallyAsync(t -> handlerError(t, handler), asyncHttpClient.executor());
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {

        List<ChatMessage> messages = chatRequest.getMessages();
        List<Tool> tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        ChatParameters parameters = chatRequest.getParameters();

        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textChatRequest = TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .timeLimit(timeout)
            .build();

        var httpRequest =
            HttpRequest.newBuilder(URI.create(url.toString() + "%s/deployments/%s/text/chat?version=%s".formatted(ML_API_PATH, deployment, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(textChatRequest)))
                .timeout(Duration.ofMillis(timeout));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            var chatResponse = fromJson(httpReponse.body(), ChatResponse.class);

            // Watsonx doesn't return "tool_calls" when the tool-choice-option is set to REQUIRED.
            if (nonNull(parameters.getToolChoiceOption()) && parameters.getToolChoiceOption().equals(ToolChoice.REQUIRED.type())) {
                var assistantMessage = chatResponse.toAssistantMessage();
                if (nonNull(assistantMessage.toolCalls()) && !assistantMessage.toolCalls().isEmpty())
                    chatResponse.getChoices().get(0).setFinishReason("tool_calls");
            }

            return chatResponse;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> chatStreaming(ChatRequest chatRequest, ChatHandler handler) {

        var messages = chatRequest.getMessages();
        var tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        var parameters = chatRequest.getParameters();
        var stateTracker = nonNull(chatRequest.getExtractionTags())
            ? new StreamingStateTracker(chatRequest.getExtractionTags())
            : null;

        requireNonNull(handler, "The chatHandler parameter can not be null");
        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textChatRequest = TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .timeLimit(timeout)
            .build();

        var httpRequest =
            HttpRequest
                .newBuilder(URI.create(url.toString() + "%s/deployments/%s/text/chat_stream?version=%s".formatted(ML_API_PATH, deployment, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(BodyPublishers.ofString(toJson(textChatRequest)))
                .timeout(Duration.ofMillis(timeout));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        Map<String, Boolean> toolHasParameters = new HashMap<>();
        if (nonNull(tools))
            tools.stream().forEach(tool -> toolHasParameters.put(tool.function().name(), tool.hasParameters()));

        var subscriber = subscriber(textChatRequest.getToolChoiceOption(), toolHasParameters, stateTracker, handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAcceptAsync(r -> {}, asyncHttpClient.executor())
            .exceptionallyAsync(t -> handlerError(t, handler), asyncHttpClient.executor());
    }

    @Override
    public ForecastResponse forecast(InputSchema inputSchema, ForecastData data, TimeSeriesParameters parameters) {

        requireNonNull(inputSchema, "InputSchema cannot be null");
        requireNonNull(data, "Data cannot be null");

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

        var httpRequest = HttpRequest
            .newBuilder(URI.create(url.toString() + "%s/deployments/%s/time_series/forecast?version=%s".formatted(ML_API_PATH, deployment, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(timeout)
            .POST(BodyPublishers.ofString(toJson(forecastRequest)));

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        try {

            var httpReponse = syncHttpClient.send(httpRequest.build(), BodyHandlers.ofString());
            return fromJson(httpReponse.body(), ForecastResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * DeploymentService deploymentService = DeploymentService.builder()
     *     .url("https://...") // or use CloudRegion
     *     .deployment("...")
     *     .authenticationProvider(authProvider)
     *     .build();
     *
     * ChatResponse response = deploymentService.chat(
     *     UserMessage.text("Tell me a joke")
     * );
     * }</pre>
     *
     * @see AuthenticationProvider
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
            logger.info("The modelId parameter is ignored for the deployment service");

        if (nonNull(projectId))
            logger.info("The projectId parameter is ignored for the deployment service");

        if (nonNull(spaceId))
            logger.info("The spaceId parameter is ignored for the deployment service");
    }

    /**
     * Builder class for constructing {@link DeploymentService} instances with configurable parameters.
     */
    public static class Builder extends WatsonxService.Builder<Builder> {
        private String deployment;

        public Builder deployment(String deployment) {
            this.deployment = deployment;
            return this;
        }

        public DeploymentService build() {
            return new DeploymentService(this);
        }
    }
}
