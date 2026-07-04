/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

/**
 * Represents a File returned by the watsonx.ai Files APIs.
 *
 * @param id the unique identifier of the file
 * @param object the object type of the entry
 * @param bytes the size of the file in bytes
 * @param createdAt the creation timestamp, in epoch seconds
 * @param expiresAt the expiration timestamp, in epoch seconds
 * @param filename the name of the file
 * @param purpose the purpose of the file
 */
public record FileData(
    String id,
    String object,
    Long bytes,
    Long createdAt,
    Long expiresAt,
    String filename,
    String purpose) {}
