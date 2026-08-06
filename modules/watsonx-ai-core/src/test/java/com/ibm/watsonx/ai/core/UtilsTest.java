/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class UtilsTest {

    @Test
    void should_return_value_when_non_null() {
        assertEquals("hello", Utils.getOrDefault("hello", "default"));
    }

    @Test
    void should_return_default_when_value_is_null() {
        assertEquals("default", Utils.getOrDefault(null, "default"));
    }
}
