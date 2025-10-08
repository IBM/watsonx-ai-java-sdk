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
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.ChatParameters.ToolChoiceOption;
import com.ibm.watsonx.ai.chat.model.TextChatRequest;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.deployment.DeploymentRestClient.ChatStreamingRequest;
import com.ibm.watsonx.ai.deployment.DeploymentRestClient.GenerateRequest;
import com.ibm.watsonx.ai.deployment.DeploymentRestClient.GenerateStreamingRequest;
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
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}. *
 *
 * @see AuthenticationProvider
 */
public class DeploymentService extends WatsonxService implements ChatProvider, TextGenerationProvider, TimeSeriesProvider {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);
    private final DeploymentRestClient client;

    protected DeploymentService(Builder builder) {
        super(builder);
        client = DeploymentRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticationProvider(builder.getAuthenticationProvider())
            .build();
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

        var request = new GenerateRequest(parameters.getTransactionId(), deploymentId, timeout, textRequest);
        return client.generate(request);
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

        var request = new GenerateStreamingRequest(parameters.getTransactionId(), deploymentId, timeout, textGenRequest, handler);
        return client.generateStreaming(request);
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        requireNonNull(chatRequest, "chatRequest cannot be null");

        var deploymentId = requireNonNull(chatRequest.getDeploymentId(), "deploymentId must be provided");
        var messages = chatRequest.getMessages();
        var tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        var parameters = chatRequest.getParameters();

        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis()));

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textChatRequest = TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .build();

        var request =
            new com.ibm.watsonx.ai.deployment.DeploymentRestClient.ChatRequest(
                parameters.getTransactionId(),
                deploymentId,
                timeout,
                textChatRequest
            );

        var chatResponse = client.chat(request);

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

        var deploymentId = requireNonNull(chatRequest.getDeploymentId(), "deploymentId must be provided");
        var messages = chatRequest.getMessages();
        var tools = nonNull(chatRequest.getTools()) && !chatRequest.getTools().isEmpty() ? chatRequest.getTools() : null;
        var parameters = chatRequest.getParameters();
        var extractionTags = chatRequest.getExtractionTags();

        requireNonNull(handler, "The chatHandler parameter can not be null");
        parameters = requireNonNullElse(parameters, ChatParameters.builder().build());
        var timeout = Duration.ofMillis(requireNonNullElse(parameters.getTimeLimit(), this.timeout.toMillis()));

        logIgnoredParameters(parameters.getModelId(), parameters.getProjectId(), parameters.getSpaceId());

        var textChatRequest = TextChatRequest.builder()
            .messages(messages)
            .tools(tools)
            .parameters(parameters)
            .build();

        var request = new ChatStreamingRequest(parameters.getTransactionId(), deploymentId, timeout, extractionTags, textChatRequest, handler);
        return client.chatStreaming(request);
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

        var request = new com.ibm.watsonx.ai.deployment.DeploymentRestClient.ForecastRequest(transactionId, deploymentId, timeout, forecastRequest);
        return client.forecast(request);
    }


    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * DeploymentService deploymentService = DeploymentService.builder()
     *     .baseUrl("https://...")      // or use CloudRegion
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
