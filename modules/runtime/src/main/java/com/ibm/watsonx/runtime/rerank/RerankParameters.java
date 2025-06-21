/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.runtime.rerank;

import static java.util.Objects.nonNull;
import com.ibm.watsonx.runtime.WatsonxParameters;
import com.ibm.watsonx.runtime.rerank.RerankRequest.Parameters;
import com.ibm.watsonx.runtime.rerank.RerankRequest.ReturnOptions;

/**
 * Represents a set of parameters used to control the behavior of a rerank operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * RerankParameters.builder()
 *   .truncateInputTokens(512)
 *   .build();
 * }</pre>
 *
 */
public final class RerankParameters extends WatsonxParameters {
  private final Integer truncateInputTokens;
  private final Integer topN;
  private final Boolean inputs;
  private final Boolean query;

  public RerankParameters(Builder builder) {
    super(builder);
    truncateInputTokens = builder.truncateInputTokens;
    topN = builder.topN;
    inputs = builder.inputs;
    query = builder.query;
  }

  public Integer getTruncateInputTokens() {
    return truncateInputTokens;
  }

  public Integer getTopN() {
    return topN;
  }

  public Boolean getInputs() {
    return inputs;
  }

  public Boolean getQuery() {
    return query;
  }

  Parameters toRerankRequestParameters() {

    Parameters parameters = null;
    ReturnOptions returnOptions = null;

    if (nonNull(topN) || nonNull(inputs) || nonNull(query))
      returnOptions = new ReturnOptions(topN, inputs, query);

    if (nonNull(truncateInputTokens) || nonNull(returnOptions))
      parameters = new Parameters(truncateInputTokens, returnOptions);

    return parameters;
  }

  /**
   * Returns a new {@link Builder} instance.
   * <p>
   * <b>Example usage:</b>
   *
   * <pre>{@code
   * RerankParameters.builder()
   *   .truncateInputTokens(512)
   *   .build();
   * }</pre>
   *
   * @return {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for constructing {@link RerankParameters} instances.
   */
  public static final class Builder extends WatsonxParameters.Builder<Builder> {
    private Integer truncateInputTokens;
    private Integer topN;
    private Boolean inputs;
    private Boolean query;

    /**
     * The maximum number of tokens allowed per input.
     * <p>
     * If the input exceeds this limit, it will be truncated from the end (right side).
     * <p>
     * Must be > 1.
     *
     * @param truncateInputTokens Integer value.
     */
    public Builder truncateInputTokens(Integer truncateInputTokens) {
      this.truncateInputTokens = truncateInputTokens;
      return this;
    }

    /**
     * If set, only the top {@code N} ranked results are returned.
     * <p>
     * Must be > 1.
     *
     * @param topN Number of ranked results
     */
    public Builder topN(Integer topN) {
      this.topN = topN;
      return this;
    }

    /**
     * Whether to return the input strings in the response.
     *
     * @param inputs Boolean value.
     */
    public Builder inputs(Boolean inputs) {
      this.inputs = inputs;
      return this;
    }

    /**
     * Whether to return the query in the response.
     *
     * @param query Boolean value.
     */
    public Builder query(Boolean query) {
      this.query = query;
      return this;
    }

    /**
     * Builds a {@link RerankParameters} instance.
     *
     * @return a new instance of {@link RerankParameters}
     */
    public RerankParameters build() {
      return new RerankParameters(this);
    }
  }
}
