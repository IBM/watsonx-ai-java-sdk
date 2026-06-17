/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

/**
 * Represents a request to fetch the details and results of a create schema job.
 *
 * @param requestTrackingId optional identifier used internally by the SDK to trace requests.
 * @param requestId the unique identifier of the create schema job to fetch.
 * @param parameters additional parameters specifying the fetch operation.
 */
public record FetchDetailsRequest(String requestTrackingId, String requestId, CreateSchemaFetchParameters parameters) {}
