/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.improve;

/**
 * Represents a request to fetch the details and results of an improve schema job.
 *
 * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
 * @param requestId the unique identifier of the improve schema job to fetch.
 * @param parameters additional parameters specifying the fetch operation.
 */
public record ImproveFetchDetailsRequest(String requestTrackingId, String requestId, ImproveSchemaFetchParameters parameters) {}
