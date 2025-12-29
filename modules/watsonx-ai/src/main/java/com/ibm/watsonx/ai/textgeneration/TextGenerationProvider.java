/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.deployment.DeploymentService;

/**
 * A provider interface for text generation functionality.
 *
 * @see TextGenerationService
 * @see DeploymentService
 */
public interface TextGenerationProvider {

    /**
     * Generates text based on the provided {@link TextGenerationRequest}.
     *
     * @param request the {@link TextGenerationRequest} containing input, moderation, parameters.
     * @return a {@link TextGenerationResponse} containing the generated text and associated metadata
     */
    public TextGenerationResponse generate(TextGenerationRequest request);

    /**
     * Sends a streaming text generation request based on the provided {@link TextGenerationRequest}.
     * <p>
     * This method initiates an asynchronous text generation operation where partial responses are delivered incrementally through the provided
     * {@link TextGenerationHandler}.
     *
     * @param request the {@link TextGenerationRequest} containing input, moderation, parameters, and optional deployment ID
     * @param handler the handler that will receive streamed generation events
     * @return a {@link CompletableFuture} that completes when the streaming generation is finished
     */
    public CompletableFuture<Void> generateStreaming(TextGenerationRequest request, TextGenerationHandler handler);
}
