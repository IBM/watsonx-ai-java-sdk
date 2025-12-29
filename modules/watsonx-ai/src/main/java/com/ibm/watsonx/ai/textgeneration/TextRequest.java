/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

/**
 * Represents a request for generating text using a specified model and input.
 *
 * @param modelId The identifier of the model to use for text generation.
 * @param spaceId The deployment space identifier.
 * @param projectId The project identifier.
 * @param input The input text prompt for generation.
 * @param parameters The parameters controlling text generation behavior.
 * @param moderations The moderation settings to apply.
 */
public record TextRequest(
    String modelId,
    String spaceId,
    String projectId,
    String input,
    TextGenerationParameters parameters,
    Moderation moderations) {}
