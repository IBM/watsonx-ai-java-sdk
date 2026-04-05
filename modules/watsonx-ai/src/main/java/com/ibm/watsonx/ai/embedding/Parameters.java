/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

/**
 * Parameters for embedding generation.
 *
 * @param truncateInputTokens the maximum number of tokens accepted per input
 * @param returnOptions the return options
 */
public record Parameters(Integer truncateInputTokens, ReturnOptions returnOptions) {

    /**
     * Return options for embedding generation.
     *
     * @param inputText whether to include the input text in each result document
     */
    public record ReturnOptions(boolean inputText) {}
}