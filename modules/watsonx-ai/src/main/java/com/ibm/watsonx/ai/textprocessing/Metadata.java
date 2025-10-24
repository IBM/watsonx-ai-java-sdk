/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Common metadata for a text processing resource.
 *
 * @param id The unique identifier of the resource.
 * @param createdAt The ISO 8601 timestamp indicating when the resource was created.
 * @param modifiedAt The ISO 8601 timestamp indicating when the resource was modified.
 * @param spaceId The id of the space containing the resource.
 * @param projectId The id of the project containing the resource.
 */
public record Metadata(String id, String createdAt, String modifiedAt, String spaceId, String projectId) {}
