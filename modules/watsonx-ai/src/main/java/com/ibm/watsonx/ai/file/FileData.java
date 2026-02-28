/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

/**
 * Represents a File returned by the watsonx.ai Files APIs.
 */
public record FileData(
    String id,
    String object,
    Integer bytes,
    Long createdAt,
    Long expiresAt,
    String filename,
    String purpose) {}
