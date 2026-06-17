/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

/**
 * Represents a request to start a new improve schema job.
 *
 * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
 * @param transactionId optional transaction identifier for correlating multiple related operations.
 * @param improveSchemaRequest the request body.
 */
public record StartImproveSchemaRequest(String requestTrackingId, String transactionId, ImproveSchemaRequest improveSchemaRequest) {}
