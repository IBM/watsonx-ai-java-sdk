package com.ibm.watsonx.runtime.tokenization;

/**
 * Represents a request for tokenizing a given text input.
 */
public record TokenizationRequest(String modelId, String input, String projectId,
    String spaceId, Parameters parameters) {

    public record Parameters(Boolean returnTokens) {}
}
