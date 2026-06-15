/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

/**
 * Represents a request to start a new create schema job.
 *
 * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
 * @param transactionId optional transaction identifier for correlating multiple related operations.
 * @param createSchemaRequest the request body.
 */
public record StartCreateSchemaRequest(String requestTrackingId, String transactionId, CreateSchemaRequest createSchemaRequest) {}
