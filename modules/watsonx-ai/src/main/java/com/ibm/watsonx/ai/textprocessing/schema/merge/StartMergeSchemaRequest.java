/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

/**
 * Represents a request to start a new merge schema job.
 *
 * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
 * @param transactionId optional transaction identifier for correlating multiple related operations.
 * @param mergeSchemaRequest the request body.
 */
public record StartMergeSchemaRequest(String requestTrackingId, String transactionId, MergeSchemaRequest mergeSchemaRequest) {}
