/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.rerank;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.rerank.RerankRequest.Parameters;
import com.ibm.watsonx.ai.rerank.RerankRequest.RerankInput;


/**
 * Service class for performing rerank requests.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * RerankService rerankService = RerankService.builder()
 *   .url("https://...") // or use CloudRegion
 *   .authenticationProvider(authProvider)
 *   .projectId("my-project-id")
 *   .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
 *   .build();
 *
 * RerankResponse response = rerankService.rerank(
 *   "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
 *   List.of(
 *     "In my younger years, I often reveled in the excitement...",
 *     "As a young man, I frequently sought out exhilarating..."
 *   )
 * );
 * }</pre>
 *
 */
public final class RerankService extends WatsonxService {

  public RerankService(Builder builder) {
    super(builder);
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

    String modelId = this.modelId;
    String projectId = this.projectId;
    String spaceId = this.spaceId;
    Parameters requestParameters = null;

    if (nonNull(parameters)) {
      modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
      projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
      spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
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

    var httpRequest = HttpRequest
      .newBuilder(URI.create(url.toString() + "%s/rerank?version=%s".formatted(ML_API_PATH, version)))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .POST(BodyPublishers.ofString(toJson(rerankRequest)))
      .build();

    try {

      var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
      return fromJson(httpReponse.body(), RerankResponse.class);

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
   * RerankService rerankService = RerankService.builder()
   *   .url("https://...") // or use CloudRegion
   *   .authenticationProvider(authProvider)
   *   .projectId("my-project-id")
   *   .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
   *   .build();
   *
   * RerankResponse response = rerankService.rerank(
   *   "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
   *   List.of(
   *     "In my younger years, I often reveled in the excitement...",
   *     "As a young man, I frequently sought out exhilarating..."
   *   )
   * );
   * }</pre>
   *
   * @return {link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for constructing {@link RerankService} instances with configurable parameters.
   */
  public static class Builder extends WatsonxService.Builder<Builder> {

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
