/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

/**
 * Represents a request to delete a submitted improve schema job.
 *
 * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
 * @param requestId the unique identifier of the improve schema job to delete.
 * @param parameters additional parameters controlling the delete operation.
 */
public record DeleteRequest(String requestTrackingId, String requestId, ImproveSchemaDeleteParameters parameters) {}
