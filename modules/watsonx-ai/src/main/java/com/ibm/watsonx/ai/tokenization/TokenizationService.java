/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

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
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.tokenization.TokenizationRequest.Parameters;

/**
 * Service class to interact with IBM watsonx.ai Text tokenization APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TokenizationService tokenizationService = TokenizationService.builder()
 *     .url("https://...")      // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
 *     .projectId("my-project-id")
 *     .modelId("ibm/granite-3-8b-instruct")
 *     .build();
 *
 * TokenizationResponse response = tokenizationService.tokenize("Tell me a joke");
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}.
 *
 * @see AuthenticationProvider
 */
public final class TokenizationService extends ModelService {

    protected TokenizationService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
    }

    /**
     * Tokenizes the provided input.
     *
     * @param input The input string to tokenize
     * @return The tokenization response.
     */
    public TokenizationResponse tokenize(String input) {
        return tokenize(input, null);
    }

    /**
     * Tokenizes the provided input.
     *
     * @param input The input string to tokenize
     * @return The tokenization response.
     */
    public CompletableFuture<TokenizationResponse> asyncTokenize(String input) {
        return asyncTokenize(input, null);
    }

    /**
     * Tokenizes the input using parameters.
     *
     * @param input The input string to tokenize.
     * @param parameters Tokenization parameters.
     * @return The tokenization response.
     */
    public TokenizationResponse tokenize(String input, TokenizationParameters parameters) {

        try {

            var httpRequest = buildHttpRequest(input, parameters);
            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TokenizationResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tokenizes the input using parameters.
     *
     * @param input The input string to tokenize.
     * @param parameters Tokenization parameters.
     * @return The tokenization response.
     */
    public CompletableFuture<TokenizationResponse> asyncTokenize(String input, TokenizationParameters parameters) {
        var httpRequest = buildHttpRequest(input, parameters);
        return asyncHttpClient.send(httpRequest, BodyHandlers.ofString())
            .thenApplyAsync(r -> Json.fromJson(r.body(), TokenizationResponse.class), ExecutorProvider.cpuExecutor())
            .thenApplyAsync(r -> r, ExecutorProvider.ioExecutor());
    }

    /**
     * Builds an HTTP request for tokenization.
     *
     * @param input The input string to be tokenized.
     * @param parameters Tokenization parameters.
     * @return A built HttpRequest object for tokenization.
     */
    private HttpRequest buildHttpRequest(String input, TokenizationParameters parameters) {
        requireNonNull(input, "Input cannot be null");

        String modelId = this.modelId;
        String projectId = this.projectId;
        String spaceId = this.spaceId;
        String transactionId = null;
        Parameters requestParameters = null;

        if (nonNull(parameters)) {
            modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
            projectId = ofNullable(parameters.getProjectId()).orElse(this.projectId);
            spaceId = ofNullable(parameters.getSpaceId()).orElse(this.spaceId);
            transactionId = parameters.getTransactionId();
            requestParameters = parameters.toTokenizationRequestParameters();
        }

        var tokenizationRequest = new TokenizationRequest(modelId, input, projectId, spaceId, requestParameters);

        var httpRequest = HttpRequest
            .newBuilder(URI.create(url.toString() + "%s/tokenization?version=%s".formatted(ML_API_TEXT_PATH, version)))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(toJson(tokenizationRequest)))
            .timeout(timeout);

        if (nonNull(transactionId))
            httpRequest.header(TRANSACTION_ID_HEADER, transactionId);

        return httpRequest.build();
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TokenizationService tokenizationService = TokenizationService.builder()
     *     .url("https://...")      // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
     *     .projectId("my-project-id")
     *     .modelId("ibm/granite-3-8b-instruct")
     *     .build();
     *
     * TokenizationResponse response = tokenizationService.tokenize("Tell me a joke");
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TokenizationService} instances with configurable parameters.
     */
    public static class Builder extends ModelService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link TokenizationService} instance using the configured parameters.
         *
         * @return a new instance of {@link TokenizationService}
         */
        public TokenizationService build() {
            return new TokenizationService(this);
        }
    }
}
