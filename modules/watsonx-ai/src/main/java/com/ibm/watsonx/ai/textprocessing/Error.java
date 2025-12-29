/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Represents an error that occurred during text processing.
 *
 * @param code A simple code representing the error type.
 * @param message A human-readable message describing the error.
 * @param moreInfo Optional URL pointing to more detailed information.
 */
public record Error(String code, String message, String moreInfo) {}
