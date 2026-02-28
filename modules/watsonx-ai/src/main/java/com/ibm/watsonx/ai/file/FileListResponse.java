/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import java.util.List;

/**
 * Represents the response returned by the watsonx.ai File list API.
 * <p>
 * Contains a paginated list of batch file entries along with pagination metadata.
 */
public record FileListResponse(String object, List<FileData> data, String firstId, String lastId, boolean hasMore) {}
