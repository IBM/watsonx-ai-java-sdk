package com.ibm.watsonx.runtime.embedding;

import static com.ibm.watsonx.core.Json.fromJson;
import static com.ibm.watsonx.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.ibm.watsonx.runtime.WatsonxService;
import com.ibm.watsonx.runtime.embedding.EmbeddingRequest.Parameters;

/**
 * Service class for performing embedding requests.
 * <p>
 * <b>Example usage:</b>
 * 
 * <pre>{@code
 * EmbeddingService embeddingService = EmbeddingService.builder()
 *     .url("https://...") // or use CloudRegion
 *     .authenticationProvider(authProvider)
 *     .modelId("ibm/granite-embedding-278m-multilingual")
 *     .build();
 *
 * EmbeddingResponse response = embeddingService.embedding(
 *     "First input",
 *     "Second input"
 * );
 * }</pre>
 */
public final class EmbeddingService extends WatsonxService {

    private static final int MAX_SIZE = 1000;

    public EmbeddingService(Builder builder) {
        super(builder);
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

        String modelId = this.modelId;
        String projectId = this.projectId;
        String spaceId = this.spaceId;
        Parameters requestParameters = null;

        if (nonNull(parameters)) {
            modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
            projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
            spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
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

            var httpRequest = HttpRequest
                .newBuilder(URI.create(url.toString() + "%s/embeddings?version=%s".formatted(ML_API_PATH, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(embeddingRequest)))
                .build();

            try {

                var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
                var response = fromJson(httpReponse.body(), EmbeddingResponse.class);
                results.addAll(response.results());
                inputTokenCount += response.inputTokenCount();
                createdAt = response.createdAt();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new EmbeddingResponse(modelId, createdAt, results, inputTokenCount);
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return {link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link EmbeddingService} instances with configurable parameters.
     */
    public static class Builder extends WatsonxService.Builder<Builder> {

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
