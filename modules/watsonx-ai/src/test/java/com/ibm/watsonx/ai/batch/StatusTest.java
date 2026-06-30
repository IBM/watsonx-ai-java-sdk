/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class StatusTest {

    @Test
    void should_parse_documented_lifecycle_states() {
        assertEquals(Status.VALIDATING, Status.fromValue("validating"));
        assertEquals(Status.IN_PROGRESS, Status.fromValue("in_progress"));
        assertEquals(Status.FINALIZING, Status.fromValue("finalizing"));
        assertEquals(Status.COMPLETED, Status.fromValue("completed"));
        assertEquals(Status.FAILED, Status.fromValue("failed"));
    }
}
