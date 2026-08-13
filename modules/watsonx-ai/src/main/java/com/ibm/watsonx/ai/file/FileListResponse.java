/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import static java.util.Objects.isNull;
import java.util.List;

/**
 * Represents the response returned by the watsonx.ai File list API.
 * <p>
 * Contains a paginated list of batch file entries along with pagination metadata.
 *
 * @param object the object type of the response
 * @param data the list of file entries
 * @param firstId the identifier of the first entry in the page
 * @param lastId the identifier of the last entry in the page
 * @param hasMore whether more entries are available beyond this page
 */
public record FileListResponse(String object, List<FileData> data, String firstId, String lastId, boolean hasMore) {

    public FileListResponse {
        data = isNull(data) ? null : List.copyOf(data);
    }
}
