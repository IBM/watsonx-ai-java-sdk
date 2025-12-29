/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

/**
 * Service class to interact with IBM watsonx.ai Foundation Models APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FoundationModelService service = FoundationModelService.builder()
 *     .baseUrl("https://...") // or use CloudRegion
 *     .build();
 *
 * var result =
 *     service.getModelDetails("meta-llama/llama-3-3-70b-instruct").orElseThrow();
 * var maxOutputTokens = result.maxOutputTokens();
 * var maxSequenceLength = result.maxSequenceLength();
 * }</pre>
 *
 * @see Authenticator
 */
public class FoundationModelService extends WatsonxService {
    private final boolean techPreview;
    private final FoundationModelRestClient client;

    private FoundationModelService(Builder builder) {
        super(builder);
        this.techPreview = requireNonNullElse(builder.techPreview, false);
        client = FoundationModelRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .httpClient(httpClient)
            .build();
    }

    /**
     * Retrieves all available foundation models.
     *
     * @return a {@link FoundationModelResponse} containing the full list of foundation models (default limit applies).
     */
    public FoundationModelResponse<FoundationModel> getModels() {
        return getModels(FoundationModelParameters.builder().build());
    }

    /**
     * Retrieves a list of foundation models filtered by the specified {@link Filter}.
     *
     * @param filter the {@link Filter} criteria to apply when fetching models.
     * @return a {@link FoundationModelResponse} containing the filtered list of models (default limit applies).
     */
    public FoundationModelResponse<FoundationModel> getModels(Filter filter) {
        return getModels(FoundationModelParameters.builder().filter(filter).build());
    }

    /**
     * Retrieves a list of foundation models with pagination and filtering.
     *
     * @param parameters Parameters for the get models request.
     * @return a {@link FoundationModelResponse} containing the filtered and/or paginated list of models.
     */
    public FoundationModelResponse<FoundationModel> getModels(FoundationModelParameters parameters) {
        parameters = requireNonNullElse(parameters, FoundationModelParameters.builder().build());
        var techPreview = ofNullable(parameters.techPreview()).orElse(this.techPreview);
        return getModels(parameters.start(), parameters.limit(), parameters.transactionId(), techPreview, parameters.filter());
    }

    /**
     * Retrieves a list of available foundation models from the model catalog.
     *
     * @param start A pagination token for fetching the next set of results (provided by the service).
     * @param limit The number of models to return (1–200). Defaults to 100 if null.
     * @param transactionId Transaction id for tracking calls.
     * @param techPreview Filter to return the models in tech preview.
     * @param filters A string expression for filtering models using logical combinations of filters.
     *
     *            <p>
     *            <b>Filter Syntax:</b>
     *
     *            <pre>
     *                pattern: tfilter[,tfilter][:(or|and)]
     *                tfilter: filter | !filter
     *            </pre>
     *
     *            <p>
     *            <b>Explanation:</b>
     *            <ul>
     *            <li><code>filter</code> requires the presence of a specific attribute.</li>
     *            <li><code>!filter</code> requires the absence of a specific attribute.</li>
     *            <li>Multiple filters can be combined with <code>,</code>.</li>
     *            <li>Logical operations can be added using <code>:or</code> or <code>:and</code>.</li>
     *            </ul>
     *
     *            <p>
     *            <b>Supported filter types (prefixes):</b>
     *            <ul>
     *            <li><code>modelid_*</code>: Filter by specific model ID</li>
     *            <li><code>provider_*</code>: Filter by model provider (e.g., IBM, Meta)</li>
     *            <li><code>source_*</code>: Filter by model source</li>
     *            <li><code>input_tier_*</code>: Filter by input tier</li>
     *            <li><code>output_tier_*</code>: Filter by output tier</li>
     *            <li><code>tier_*</code>: Filter by input or output tier</li>
     *            <li><code>task_*</code>: Filter by supported task ID (e.g., summarization, classification)</li>
     *            <li><code>lifecycle_*</code>: Filter by lifecycle state (e.g., active, deprecated)</li>
     *            <li><code>function_*</code>: Filter by supported functions</li>
     *            </ul>
     * @return List of foundation models.
     */
    protected FoundationModelResponse<FoundationModel> getModels(Integer start, Integer limit, String transactionId, Boolean techPreview,
        String filters) {
        return client.getModels(start, limit, transactionId, techPreview, filters);
    }

    /**
     * Retrieves the list of tasks.
     *
     * @return a {@link FoundationModelResponse} containing foundation model tasks (default limit applies).
     */
    public FoundationModelResponse<FoundationModelTask> getTasks() {
        return getTasks(FoundationModelParameters.builder().build());
    }

    /**
     * Retrieves a paginated list of tasks.
     *
     * @param parameters Parameters to customize the get tasks operation.
     * @return a {@link FoundationModelResponse} containing the list of tasks.
     */
    public FoundationModelResponse<FoundationModelTask> getTasks(FoundationModelParameters parameters) {
        return client.getTasks(parameters);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * FoundationModelService service = FoundationModelService.builder()
     *     .baseUrl("https://...") // or use CloudRegion
     *     .build();
     *
     * var result =
     *     service.getModelDetails("meta-llama/llama-3-3-70b-instruct").orElseThrow();
     * var maxOutputTokens = result.maxOutputTokens();
     * var maxSequenceLength = result.maxSequenceLength();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link FoundationModelService} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxService.Builder<Builder> {
        private Boolean techPreview;

        private Builder() {}

        /**
         * See all the Tech Preview models if entitled.
         *
         * @param techPreview {@code true} to see all the tech preview models, {@code false} otherwise
         */
        public Builder techPreview(boolean techPreview) {
            this.techPreview = techPreview;
            return this;
        }

        /**
         * Builds a {@link FoundationModelService} instance using the configured parameters.
         *
         * @return a new instance of {@link FoundationModelService}
         */
        public FoundationModelService build() {
            return new FoundationModelService(this);
        }
    }
}
