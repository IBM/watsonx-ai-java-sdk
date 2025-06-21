/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

import java.util.List;

/**
 * Represents the response returned from a text tokenization request.
 */
public record TokenizationResponse(String modelId, Result result) {

  public record Result(int tokenCount, List<String> tokens) {
  }
}
