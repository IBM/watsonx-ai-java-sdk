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
 *
 * @param object The object type.
 * @param data The list of batch job entries.
 * @param firstId The identifier of the first batch job in the list.
 * @param lastId The identifier of the last batch job in the list.
 * @param hasMore Whether more results are available beyond this page.
 */
public record BatchListResponse(String object, List<BatchData> data, String firstId, String lastId, boolean hasMore) {}
