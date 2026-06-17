/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

/**
 * Represents a request for the Merge Schema API.
 *
 * @param projectId the project identifier
 * @param spaceId the space identifier
 * @param parameters parameters
 */
public record MergeSchemaRequest(String projectId, String spaceId, Parameters parameters) {}
