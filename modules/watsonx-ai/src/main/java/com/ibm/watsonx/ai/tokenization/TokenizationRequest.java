/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

import com.ibm.watsonx.ai.Crypto;

/**
 * Represents a request for tokenizing a given text input.
 */
public record TokenizationRequest(String modelId, String input, String projectId,
    String spaceId, Parameters parameters, Crypto crypto) {

    public record Parameters(Boolean returnTokens) {}
}
