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
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.tokenization.TokenizationRequest.Parameters;

/**
 * Service class for performing text tokenization requests.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TokenizationService rerankService = TokenizationService.builder()
 *   .url("https://...") // or use CloudRegion
 *   .authenticationProvider(authProvider)
 *   .projectId("my-project-id")
 *   .modelId("ibm/granite-3-8b-instruct")
 *   .build();
 *
 * TokenizationResponse response = TokenizationService.tokenize("Tell me a joke");
 * }</pre>
 *
 */
public class TokenizationService extends WatsonxService {

  public TokenizationService(Builder builder) {
    super(builder);
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
   * Tokenizes the input using parameters.
   *
   * @param input The input string to tokenize.
   * @param parameters Tokenization parameters.
   * @return The tokenization response.
   */
  public TokenizationResponse tokenize(String input, TokenizationParameters parameters) {

    requireNonNull(input, "Input cannot be null");

    String modelId = this.modelId;
    String projectId = this.projectId;
    String spaceId = this.spaceId;
    Parameters requestParameters = null;

    if (nonNull(parameters)) {
      modelId = requireNonNullElse(parameters.getModelId(), this.modelId);
      projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
      spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
      requestParameters = parameters.toTokenizationRequestParameters();
    }

    var tokenizationRequest = new TokenizationRequest(modelId, input, projectId,
      spaceId, requestParameters);

    var httpRequest = HttpRequest
      .newBuilder(URI.create(url.toString() + "%s/tokenization?version=%s".formatted(ML_API_PATH, version)))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .POST(BodyPublishers.ofString(toJson(tokenizationRequest)))
      .build();

    try {

      var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
      return fromJson(httpReponse.body(), TokenizationResponse.class);

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
   * TokenizationService rerankService = TokenizationService.builder()
   *   .url("https://...") // or use CloudRegion
   *   .authenticationProvider(authProvider)
   *   .projectId("my-project-id")
   *   .modelId("ibm/granite-3-8b-instruct")
   *   .build();
   *
   * TokenizationResponse response = TokenizationService.tokenize("Tell me a joke");
   * }</pre>
   *
   * @return {link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for constructing {@link TokenizationService} instances with configurable parameters.
   */
  public static class Builder extends WatsonxService.Builder<Builder> {

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
