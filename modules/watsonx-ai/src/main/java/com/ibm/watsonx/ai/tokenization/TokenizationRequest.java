/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

/**
 * Represents a request for tokenizing a given text input.
 */
public record TokenizationRequest(String modelId, String input, String projectId,
    String spaceId, Parameters parameters) {

    public record Parameters(Boolean returnTokens) {}
}
