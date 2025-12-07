/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.embedding.EmbeddingRequest.Parameters;

/**
 * Service class to interact with IBM watsonx.ai Text Embeddings APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * EmbeddingService embeddingService = EmbeddingService.builder()
 *     .baseUrl("https://...")      // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .projectId("my-project-id")
 *     .modelId("ibm/granite-embedding-278m-multilingual")
 *     .build();
 *
 * EmbeddingResponse response = embeddingService.embedding(
 *     "First input",
 *     "Second input"
 * );
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class EmbeddingService extends ModelService {
    private static final int MAX_SIZE = 1000;
    private final EmbeddingRestClient client;

    private EmbeddingService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = EmbeddingRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .build();
    }

    /**
     * Embeds the provided strings into a vector space and returns the embedding results.
     *
     * @param inputs The strings to be embedded.
     * @return An EmbeddingResponse object containing the embedding results.
     */
    public EmbeddingResponse embedding(String... inputs) {
        return embedding(Arrays.asList(inputs));
    }

    /**
     * Embeds the provided strings into a vector space and returns the embedding results.
     *
     * @param inputs The strings to be embedded.
     * @return An EmbeddingResponse object containing the embedding results.
     */
    public EmbeddingResponse embedding(List<String> inputs) {
        return embedding(inputs, null);
    }

    /**
     * Embeds the provided strings into a vector space and returns the embedding results.
     *
     * @param inputs The strings to be embedded.
     * @param parameters Parameters for the embedding request.
     * @return An EmbeddingResponse object containing the embedding results.
     */
    public EmbeddingResponse embedding(List<String> inputs, EmbeddingParameters parameters) {

        requireNonNull(inputs, "Inputs cannot be null");

        ProjectSpace projectSpace = resolveProjectSpace(parameters);
        String projectId = projectSpace.projectId();
        String spaceId = projectSpace.spaceId();
        String modelId = this.modelId;
        String transactionId = null;
        Parameters requestParameters = null;

        if (nonNull(parameters)) {
            modelId = requireNonNullElse(parameters.modelId(), this.modelId);
            transactionId = parameters.transactionId();
            requestParameters = parameters.toEmbeddingRequestParameters();
        }

        int inputTokenCount = 0;
        String createdAt = null;
        List<EmbeddingResponse.Result> results = new ArrayList<>();

        // Watsonx.ai embedding API allows a maximum of 1000 elements per request.
        for (int fromIndex = 0; fromIndex < inputs.size(); fromIndex += MAX_SIZE) {
            int toIndex = Math.min(fromIndex + MAX_SIZE, inputs.size());
            List<String> subList = inputs.subList(fromIndex, toIndex);

            var embeddingRequest =
                new EmbeddingRequest(modelId, spaceId, projectId, subList, requestParameters);

            var response = client.embedding(transactionId, embeddingRequest);
            results.addAll(response.results());
            inputTokenCount += response.inputTokenCount();
            createdAt = response.createdAt();
        }

        return new EmbeddingResponse(modelId, createdAt, results, inputTokenCount);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * EmbeddingService embeddingService = EmbeddingService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .projectId("my-project-id")
     *     .modelId("ibm/granite-embedding-278m-multilingual")
     *     .build();
     *
     * EmbeddingResponse response = embeddingService.embedding(
     *     "First input",
     *     "Second input"
     * );
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link EmbeddingService} instances with configurable parameters.
     */
    public final static class Builder extends ModelService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link EmbeddingService} instance using the configured parameters.
         *
         * @return a new instance of {@link EmbeddingService}
         */
        public EmbeddingService build() {
            return new EmbeddingService(this);
        }
    }
}
