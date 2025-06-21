/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.embedding.EmbeddingParameters;
import com.ibm.watsonx.ai.rerank.RerankParameters;

/**
 * This is an abstract class that contains the shared parameters for various Builder configurations.
 * <p>
 * It serves as a base class for different types of parameters used in the {@code watsonx.ai} platform.
 *
 * @see ChatParameters
 * @see EmbeddingParameters
 * @see RerankParameters
 */
public abstract class WatsonxParameters {
  protected final String projectId;
  protected final String spaceId;
  protected final String modelId;

  public WatsonxParameters(Builder<?> builder) {
    projectId = builder.projectId;
    spaceId = builder.spaceId;
    modelId = builder.modelId;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getSpaceId() {
    return spaceId;
  }

  public String getModelId() {
    return modelId;
  }

  @SuppressWarnings("unchecked")
  public static abstract class Builder<T extends Builder<T>> {
    private String projectId;
    private String spaceId;
    private String modelId;

    /**
     * Sets the project id.
     *
     * @param spaceId Project id value
     */
    public T projectId(String projectId) {
      this.projectId = projectId;
      return (T) this;
    }

    /**
     * Sets the space id.
     *
     * @param spaceId Space id value
     */
    public T spaceId(String spaceId) {
      this.spaceId = spaceId;
      return (T) this;
    }

    /**
     * Sets the model id.
     *
     * @param modelId the model identifier to use
     */
    public T modelId(String modelId) {
      this.modelId = modelId;
      return (T) this;
    }
  }
}
