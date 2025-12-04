/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model;

/**
 * Represents usage statistics for a chat completion request.
 * <p>
 * This record contains information about the number of tokens used during the processing of a chat request, including prompt tokens, completion
 * tokens, and the total number of tokens consumed.
 */
public record ChatUsage(Integer completionTokens, Integer promptTokens, Integer totalTokens) {}