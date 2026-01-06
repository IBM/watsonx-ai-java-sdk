/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.Crypto;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.tokenization.TokenizationRequest.Parameters;

/**
 * Service class to interact with IBM watsonx.ai Text tokenization APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TokenizationService tokenizationService = TokenizationService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .projectId("my-project-id")
 *     .modelId("ibm/granite-4-h-small")
 *     .build();
 *
 * TokenizationResponse response = tokenizationService.tokenize("Tell me a joke");
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class TokenizationService extends ModelService {
    private final TokenizationRestClient client;

    private TokenizationService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = TokenizationRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
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
        var tokenizationRequest = buildTokenizationRequest(input, parameters);
        var transactionId = nonNull(parameters) ? parameters.transactionId() : null;
        return client.tokenize(transactionId, tokenizationRequest);
    }

    /**
     * Tokenizes the input using parameters.
     *
     * @param input The input string to tokenize.
     * @param parameters Tokenization parameters.
     * @return The tokenization response.
     */
    public CompletableFuture<TokenizationResponse> asyncTokenize(String input, TokenizationParameters parameters) {
        var tokenizationRequest = buildTokenizationRequest(input, parameters);
        var transactionId = nonNull(parameters) ? parameters.transactionId() : null;
        return client.asyncTokenize(transactionId, tokenizationRequest);
    }

    /**
     * Builds the TokenizationRequest.
     *
     * @param input The input string to be tokenized.
     * @param parameters Tokenization parameters.
     * @return A built TokenizationRequest.
     */
    private TokenizationRequest buildTokenizationRequest(String input, TokenizationParameters parameters) {
        requireNonNull(input, "Input cannot be null");

        ProjectSpace projectSpace = resolveProjectSpace(parameters);
        String projectId = projectSpace.projectId();
        String spaceId = projectSpace.spaceId();
        String modelId = this.modelId;
        Crypto crypto = null;
        Parameters requestParameters = null;

        if (nonNull(parameters)) {
            modelId = requireNonNullElse(parameters.modelId(), this.modelId);
            requestParameters = parameters.toTokenizationRequestParameters();
            crypto = nonNull(parameters.crypto()) ? new Crypto(parameters.crypto()) : null;
        }

        return new TokenizationRequest(modelId, input, projectId, spaceId, requestParameters, crypto);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TokenizationService tokenizationService = TokenizationService.builder()
     *     .baseUrl("https://...")      // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .projectId("my-project-id")
     *     .modelId("ibm/granite-4-h-small")
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
    public final static class Builder extends ModelService.Builder<Builder> {

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
