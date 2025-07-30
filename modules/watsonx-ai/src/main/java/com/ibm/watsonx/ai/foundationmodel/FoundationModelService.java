/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.StringJoiner;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.foundationmodel.filter.Filter;

/**
 * Service class to interact with IBM watsonx.ai Foundation Models APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FoundationModelService service = FoundationModelService.builder()
 *     .url("https://...") // or use CloudRegion
 *     .build();
 *
 * var result =
 *     service.getModelDetails("meta-llama/llama-3-3-70b-instruct").orElseThrow();
 * var maxOutputTokens = result.maxOutputTokens();
 * var maxSequenceLength = result.maxSequenceLength();
 * }</pre>
 *
 * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#list-foundation-model-specs" target="_blank"> official
 * documentation</a>.
 *
 * @see AuthenticationProvider
 */
public final class FoundationModelService extends WatsonxService {
    private final boolean techPreview;

    protected FoundationModelService(Builder builder) {
        super(builder);
        this.techPreview = requireNonNullElse(builder.techPreview, false);
    }

    /**
     * Retrieves the details of a model.
     *
     * @param modelId Unique identifier of the model.
     * @return an {@link Optional} containing the {@link FoundationModel} details if found, or an empty {@code Optional} if the model is not
     *         available.
     */
    public Optional<FoundationModel> getModelDetails(String modelId) {
        var result = getModels(Filter.of(modelId(modelId)));
        if (result.totalCount() == 1)
            return Optional.of(result.resources().get(0));
        else
            return Optional.empty();
    }

    /**
     * Retrieves all available foundation models.
     *
     * @return a {@link FoundationModelResponse} containing the full list of foundation models (default limit applies).
     */
    public FoundationModelResponse<FoundationModel> getModels() {
        return getModels(null);
    }

    /**
     * Retrieves a list of foundation models filtered by the specified {@link Filter}.
     *
     * @param filter the {@link Filter} criteria to apply when fetching models.
     * @return a {@link FoundationModelResponse} containing the filtered list of models (default limit applies).
     */
    public FoundationModelResponse<FoundationModel> getModels(Filter filter) {
        return getModels(null, null, filter);
    }

    /**
     * Retrieves a list of foundation models with pagination and filtering.
     *
     * @param start A pagination token indicating where to start fetching results.
     * @param limit The maximum number of models to return.
     * @param filter The {@link Filter} criteria used to narrow down model results.
     * @return a {@link FoundationModelResponse} containing the filtered and/or paginated list of models.
     */
    public FoundationModelResponse<FoundationModel> getModels(Integer start, Integer limit, Filter filter) {
        return getModels(start, limit, nonNull(filter) ? filter.toString() : null);
    }

    /**
     * Retrieves a list of available foundation models from the model catalog.
     *
     * @param start A pagination token for fetching the next set of results (provided by the service).
     * @param limit The number of models to return (1–200). Defaults to 100 if null.
     * @param filters A string expression for filtering models using logical combinations of filters.
     *
     *            <p>
     *            <b>Filter Syntax:</b>
     *            </p>
     *
     *            <pre>
     *                pattern: tfilter[,tfilter][:(or|and)]
     *                tfilter: filter | !filter
     *            </pre>
     *
     *            <p>
     *            <b>Explanation:</b>
     *            </p>
     *            <ul>
     *            <li><code>filter</code> requires the presence of a specific attribute.</li>
     *            <li><code>!filter</code> requires the absence of a specific attribute.</li>
     *            <li>Multiple filters can be combined with <code>,</code>.</li>
     *            <li>Logical operations can be added using <code>:or</code> or <code>:and</code>.</li>
     *            </ul>
     *
     *            <p>
     *            <b>Supported filter types (prefixes):</b>
     *            </p>
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
    protected FoundationModelResponse<FoundationModel> getModels(Integer start, Integer limit, String filters) {
        try {

            StringJoiner queryParameters = new StringJoiner("&", "", "");
            queryParameters.add("version=" + version);

            if (nonNull(start))
                queryParameters.add("start=" + start);

            if (nonNull(limit))
                queryParameters.add("limit=" + limit);

            if (techPreview)
                queryParameters.add("tech_preview=" + techPreview);

            if (nonNull(filters))
                queryParameters.add("filters=" + URLEncoder.encode(filters.toString(), StandardCharsets.UTF_8));

            var uri =
                URI.create(url.toString() + "%s/foundation_model_specs?%s".formatted(ML_API_PATH, queryParameters));

            var httpRequest = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET().build();

            var response = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(response.body(), new TypeToken<FoundationModelResponse<FoundationModel>>() {});

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the list of tasks.
     *
     * @return a {@link FoundationModelResponse} containing foundation model tasks (default limit applies).
     */
    public FoundationModelResponse<FoundationModelTask> getTasks() {
        return getTasks(null, null);
    }

    /**
     * Retrieves a paginated list of tasks.
     *
     * @param start A pagination token indicating where to start fetching results.
     * @param limit The maximum number of models to return.
     * @return a {@link FoundationModelResponse} containing the list of tasks.
     */
    public FoundationModelResponse<FoundationModelTask> getTasks(Integer start, Integer limit) {

        StringJoiner queryParameters = new StringJoiner("&", "", "");
        queryParameters.add("version=" + version);

        if (nonNull(start))
            queryParameters.add("start=" + start);

        if (nonNull(limit))
            queryParameters.add("limit=" + limit);

        var uri =
            URI.create(url.toString() + "%s/foundation_model_tasks?%s".formatted(ML_API_PATH, queryParameters));

        var httpRequest = HttpRequest.newBuilder(uri).GET().build();

        try {

            var response = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(response.body(), new TypeToken<FoundationModelResponse<FoundationModelTask>>() {});

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
     * FoundationModelService service = FoundationModelService.builder()
     *     .url("https://...") // or use CloudRegion
     *     .build();
     *
     * var result =
     *     service.getModelDetails("meta-llama/llama-3-3-70b-instruct").orElseThrow();
     * var maxOutputTokens = result.maxOutputTokens();
     * var maxSequenceLength = result.maxSequenceLength();
     * }</pre>
     *
     * @see AuthenticationProvider
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link FoundationModelService} instances with configurable parameters.
     */
    public static class Builder extends WatsonxService.Builder<Builder> {
        private Boolean techPreview;

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
