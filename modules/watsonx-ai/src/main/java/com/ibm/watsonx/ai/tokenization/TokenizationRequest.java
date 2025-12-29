/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

import com.ibm.watsonx.ai.Crypto;

/**
 * Represents a request for tokenizing a given text input.
 *
 * @param modelId The identifier of the tokenization model to use.
 * @param input The text input to be tokenized.
 * @param projectId The project identifier.
 * @param spaceId The deployment space identifier.
 * @param parameters Additional parameters for the tokenization operation.
 * @param crypto Encryption configuration for sensitive data.
 */
public record TokenizationRequest(String modelId, String input, String projectId,
    String spaceId, Parameters parameters, Crypto crypto) {

    /**
     * Additional parameters for controlling the tokenization behavior.
     *
     * @param returnTokens Whether to include the actual tokens in the response.
     */
    public record Parameters(Boolean returnTokens) {}
}
