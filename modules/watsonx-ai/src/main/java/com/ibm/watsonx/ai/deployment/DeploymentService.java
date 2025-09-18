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
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoice;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.core.SseEventLogger;
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
 *     .url("https://...")      // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
 *     .build();
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}. *
 *
 * @see AuthenticationProvider
 */
public class DeploymentService extends WatsonxService implements ChatProvider, TextGenerationProvider, TimeSeriesProvider {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);

    protected DeploymentService(Builder builder) {
        super(builder);
    }

    /**
     * Retrieves deployment details with the specified parameters.
     *
     * @param parameters the {@link FindByIdRequest} specifying a different deployment, project or space ID, and other options
     * @return the {@link DeploymentResource} with the retrieved deployment details
     */
    public DeploymentResource findById(FindByIdRequest parameters) {

        parameters = requireNonNullElse(parameters, FindByIdRequest.builder().build());
        var deploymentId = requireNonNull(parameters.getDeploymentId(), "deploymentId must be provided");

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
    public TextGenerationResponse generate(TextGenerationRequest textGenerationRequest) {
        requireNonNull(textGenerationRequest, "textGenerationRequest cannot be null");

        // The input parameter can be null, if the prompt_template is used.

        var input = textGenerationRequest.getInput();
        var moderation = textGenerationRequest.getModeration();
        var deploymentId = requireNonNull(textGenerationRequest.getDeploymentId(), "deploymentId must be provided");
        var parameters = requireNonNullElse(textGenerationRequest.getParameters(), TextGenerationParameters.builder().build());
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var request =
            new TextRequest(null, null, null, input, parameters.toSanitized(), moderation);

        var httpRequest = HttpRequest
            .newBuilder(URI.create(url.toString() + "%s/deployments/%s/text/generation?version=%s".formatted(ML_API_PATH, deploymentId, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(Duration.ofMillis(timeout))
            .POST(BodyPublishers.ofString(toJson(request)));

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
    public CompletableFuture<Void> generateStreaming(TextGenerationRequest textGenerationRequest, TextGenerationHandler handler) {
        requireNonNull(textGenerationRequest, "textGenerationRequest cannot be null");

        // The input parameter can be null, if the prompt_template is used.

        var input = textGenerationRequest.getInput();
        var deploymentId = requireNonNull(textGenerationRequest.getDeploymentId(), "deploymentId must be provided");
        var parameters = requireNonNullElse(textGenerationRequest.getParameters(), TextGenerationParameters.builder().build());
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textGenRequest =
            new TextRequest(null, null, null, input, parameters.toSanitized(), null);

        var httpRequest = HttpRequest
            .newBuilder(
                URI.create(url.toString() + "%s/deployments/%s/text/generation_stream?version=%s".formatted(ML_API_PATH, deploymentId, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .timeout(Duration.ofMillis(timeout))
            .POST(BodyPublishers.ofString(toJson(textGenRequest)));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        var subscriber = subscriber(handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> handlerError(t, handler));
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");

        var deploymentId = requireNonNull(chatRequest.getDeploymentId(), "deploymentId must be provided");
        var messages = chatRequest.getMessages();
        var tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        var parameters = chatRequest.getParameters();

        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textChatRequest = TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .build();

        var httpRequest =
            HttpRequest
                .newBuilder(URI.create(url.toString() + "%s/deployments/%s/text/chat?version=%s".formatted(ML_API_PATH, deploymentId, version)))
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
        requireNonNull(chatRequest, "chatRequest cannot be null");

        var deploymentId = requireNonNull(chatRequest.getDeploymentId(), "deploymentId must be provided");
        var messages = chatRequest.getMessages();
        var tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        var parameters = chatRequest.getParameters();

        requireNonNull(handler, "The chatHandler parameter can not be null");
        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());
        var timeout = requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis());

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textChatRequest = TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .build();

        var httpRequest =
            HttpRequest
                .newBuilder(
                    URI.create(url.toString() + "%s/deployments/%s/text/chat_stream?version=%s".formatted(ML_API_PATH, deploymentId, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(BodyPublishers.ofString(toJson(textChatRequest)))
                .timeout(Duration.ofMillis(timeout));

        if (nonNull(parameters.getTransactionId()))
            httpRequest.header(TRANSACTION_ID_HEADER, parameters.getTransactionId());

        Map<String, Boolean> toolHasParameters = new HashMap<>();
        if (nonNull(tools))
            tools.stream().forEach(tool -> toolHasParameters.put(tool.function().name(), tool.hasParameters()));

        var subscriber = subscriber(textChatRequest.getToolChoiceOption(), toolHasParameters, chatRequest.getExtractionTags(), handler);
        return asyncHttpClient.send(httpRequest.build(), responseInfo -> logResponses
            ? BodySubscribers.fromLineSubscriber(new SseEventLogger(subscriber, responseInfo.statusCode(), responseInfo.headers()))
            : BodySubscribers.fromLineSubscriber(subscriber))
            .thenAccept(r -> {})
            .exceptionally(t -> handlerError(t, handler));
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

        var httpRequest = HttpRequest
            .newBuilder(
                URI.create(url.toString() + "%s/deployments/%s/time_series/forecast?version=%s".formatted(ML_API_PATH, deploymentId, version)))
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
     *     .url("https://...")      // or use CloudRegion
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
    public static class Builder extends WatsonxService.Builder<Builder> {

        private Builder() {}

        public DeploymentService build() {
            return new DeploymentService(this);
        }
    }
}
