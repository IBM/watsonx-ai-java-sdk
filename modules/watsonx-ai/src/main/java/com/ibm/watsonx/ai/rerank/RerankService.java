/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.rerank;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.rerank.RerankRequest.Parameters;
import com.ibm.watsonx.ai.rerank.RerankRequest.RerankInput;


/**
 * Service class to interact with IBM watsonx.ai Text Rerank APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * RerankService rerankService = RerankService.builder()
 *     .baseUrl("https://...")      // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud AuthenticationProvider
 *     .projectId("my-project-id")
 *     .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
 *     .build();
 *
 * RerankResponse response = rerankService.rerank(
 *     "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
 *     List.of(
 *         "In my younger years, I often reveled in the excitement...",
 *         "As a young man, I frequently sought out exhilarating..."
 *     )
 * );
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}.
 *
 * @see AuthenticationProvider
 */
public class RerankService extends ModelService {
    private final RerankRestClient client;

    private RerankService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticationProvider(), "authenticationProvider cannot be null");
        client = RerankRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticationProvider(builder.authenticationProvider())
            .build();
    }

    /**
     * Performs a reranking operation using the specified query and input texts.
     *
     * @param query The query to rank the input texts against.
     * @param inputs The list of input texts to be reranked.
     * @return The {@link RerankResponse} containing the reranked results.
     */
    public RerankResponse rerank(String query, List<String> inputs) {
        return rerank(query, inputs, null);
    }

    /**
     * Performs a reranking operation using the specified query and input texts.
     *
     * @param query The query to rank the input texts against.
     * @param inputs The list of input texts to be reranked.
     * @param parameters Set of parameters used to control the behavior of a rerank operation
     * @return The {@link RerankResponse} containing the reranked results.
     */
    public RerankResponse rerank(String query, List<String> inputs, RerankParameters parameters) {

        requireNonNull(query, "Query cannot be null");
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
            requestParameters = parameters.toRerankRequestParameters();
        }

        var rerankRequest = new RerankRequest(
            modelId,
            inputs.stream().map(RerankInput::new).toList(),
            query,
            spaceId,
            projectId,
            requestParameters
        );

        return client.rerank(transactionId, rerankRequest);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * RerankService rerankService = RerankService.builder()
     *     .baseUrl("https://...")      // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud AuthenticationProvider
     *     .projectId("my-project-id")
     *     .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
     *     .build();
     *
     * RerankResponse response = rerankService.rerank(
     *     "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
     *     List.of(
     *         "In my younger years, I often reveled in the excitement...",
     *         "As a young man, I frequently sought out exhilarating..."
     *     )
     * );
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link RerankService} instances with configurable parameters.
     */
    public final static class Builder extends ModelService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link RerankService} instance using the configured parameters.
         *
         * @return a new instance of {@link RerankService}
         */
        public RerankService build() {
            return new RerankService(this);
        }
    }
}
