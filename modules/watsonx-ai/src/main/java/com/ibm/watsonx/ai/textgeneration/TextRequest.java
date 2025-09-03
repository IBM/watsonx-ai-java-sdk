/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

/**
 * Represents a request for generating text using a specified model and input.
 */
public record TextRequest(
    String modelId,
    String spaceId,
    String projectId,
    String input,
    TextGenerationParameters parameters,
    Moderation moderations) {}
