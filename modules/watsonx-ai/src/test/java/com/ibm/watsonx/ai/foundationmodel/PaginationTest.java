/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelResponse.Pagination;

public class PaginationTest {

    @Test
    void test_pagination() {
        var pagination = new Pagination("https://localhost:8080/ml/v1/foundation_model_tasks?version=2025-04-23&limit=10");
        assertEquals(10, pagination.limit().orElseThrow());

        pagination = new Pagination("http://localhost/ml/v1/foundation_model_tasks?version=2025-04-23&limit=10&start=1");
        assertEquals(10, pagination.limit().orElseThrow());
        assertEquals(1, pagination.start().orElseThrow());

        pagination = new Pagination("http://localhost/ml/v1/foundation_model_tasks?version=2025-04-23");
        assertFalse(pagination.limit().isPresent());
        assertFalse(pagination.start().isPresent());
    }
}
