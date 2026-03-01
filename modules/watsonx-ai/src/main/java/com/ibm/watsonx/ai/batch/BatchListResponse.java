/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import java.util.List;

/**
 * Represents the response returned by the watsonx.ai Batch list API.
 * <p>
 * Contains a paginated list of batch job entries along with pagination metadata.
 */
public record BatchListResponse(String object, List<BatchData> data, String firstId, String lastId, boolean hasMore) {}
