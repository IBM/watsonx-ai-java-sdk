/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.rerank;

import java.util.List;
import com.ibm.watsonx.ai.Crypto;

/**
 * Represents a request to perform text reranking using a specified model.
 */
public record RerankRequest(
    String modelId,
    List<RerankInput> inputs,
    String query,
    String spaceId,
    String projectId,
    Parameters parameters,
    Crypto crypto) {

    public record RerankInput(String text) {}
    public record Parameters(Integer truncateInputTokens, ReturnOptions returnOptions) {}
    public record ReturnOptions(Integer topN, boolean inputs, Boolean query) {}
}
