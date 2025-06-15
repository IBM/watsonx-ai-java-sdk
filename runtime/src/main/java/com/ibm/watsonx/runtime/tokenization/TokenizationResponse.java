package com.ibm.watsonx.runtime.tokenization;

import java.util.List;

/**
 * Represents the response returned from a text tokenization request.
 */
public record TokenizationResponse(String modelId, Result result) {

    public record Result(int tokenCount, List<String> tokens) {}
}
