/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.tokenization;

import java.util.List;

/**
 * Represents the response returned from a text tokenization request.
 *
 * @param modelId The identifier of the model used for tokenization.
 * @param result The tokenization result containing token count and tokens.
 */
public record TokenizationResponse(String modelId, Result result) {

    /**
     * Represents the result of a tokenization operation.
     *
     * @param tokenCount The total number of tokens produced.
     * @param tokens The list of individual tokens (if requested).
     */
    public record Result(int tokenCount, List<String> tokens) {}
}
