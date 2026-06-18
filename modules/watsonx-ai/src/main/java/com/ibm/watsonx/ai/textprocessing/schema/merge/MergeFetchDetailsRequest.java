/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.merge;

/**
 * Represents a request to fetch the details and results of an merge schema job.
 *
 * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
 * @param requestId the unique identifier of the merge schema job to fetch.
 * @param parameters additional parameters specifying the fetch operation.
 */
public record MergeFetchDetailsRequest(String requestTrackingId, String requestId, MergeSchemaFetchParameters parameters) {}
