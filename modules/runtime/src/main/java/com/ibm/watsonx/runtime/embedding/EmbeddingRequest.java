/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.runtime.embedding;

import java.util.List;

/**
 * Represents a request to generate embeddings from a given model.
 */
public record EmbeddingRequest(String modelId, String spaceId, String projectId,
  List<String> inputs, Parameters parameters) {

  public record Parameters(Integer truncateInputTokens, ReturnOptions returnOptions) {
  }
  public record ReturnOptions(boolean inputText) {
  }
}
