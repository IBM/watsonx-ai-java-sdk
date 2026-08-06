/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.cluster;

/**
 * Represents a request to start a new cluster schema job.
 *
 * @param requestTrackingId optional identifier used internally by the SDK to trace requests
 * @param transactionId optional transaction identifier for correlating multiple related operations
 * @param clusterSchemaRequest the request body
 */
public record StartClusterSchemaRequest(String requestTrackingId, String transactionId, ClusterSchemaRequest clusterSchemaRequest) {}
