/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import java.util.Optional;

/**
 * Represents a set of parameters used to control the behavior of a text extraction delete operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextExtractionDeleteParameters.builder()
 *   .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
 *   .hardDelete(true)
 *   .build();
 * }</pre>
 *
 */
public class TextExtractionDeleteParameters {
  private final String projectId;
  private final String spaceId;
  private final Optional<Boolean> hardDelete;

  public TextExtractionDeleteParameters(Builder builder) {
    this.projectId = builder.projectId;
    this.spaceId = builder.spaceId;
    this.hardDelete = Optional.ofNullable(builder.hardDelete);
  }

  public String getProjectId() {
    return projectId;
  }

  public String getSpaceId() {
    return spaceId;
  }

  public Optional<Boolean> getHardDelete() {
    return hardDelete;
  }

  /**
   * Returns a new {@link Builder} instance.
   * <p>
   * <b>Example usage:</b>
   *
   * <pre>{@code
   * TextExtractionDeleteParameters.builder()
   *   .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
   *   .hardDelete(true)
   *   .build();
   * }</pre>
   *
   * @return {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for constructing {@link TextExtractionDeleteParameters} instances.
   */
  public static class Builder {
    private String projectId;
    private String spaceId;
    private Boolean hardDelete;

    /**
     * Sets the project id.
     *
     * @param projectId the project id.
     */
    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    /**
     * Sets the space id.
     *
     * @param spaceId the space id.
     */
    public Builder spaceId(String spaceId) {
      this.spaceId = spaceId;
      return this;
    }

    /**
     * Sets the hard delete option.
     *
     * @param hardDelete {@code true} to also delete job metadata.
     */
    public Builder hardDelete(Boolean hardDelete) {
      this.hardDelete = hardDelete;
      return this;
    }

    /**
     * Builds a {@link TextExtractionDeleteParameters} instance using the configured parameters.
     *
     * @return a new instance of {@link TextExtractionDeleteParameters}
     */
    public TextExtractionDeleteParameters build() {
      return new TextExtractionDeleteParameters(this);
    }
  }
}
