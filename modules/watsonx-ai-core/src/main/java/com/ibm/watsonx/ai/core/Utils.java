/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.nonNull;

/**
 * A utility class that provides helper methods for common operations.
 */
public class Utils {

    /**
     * Returns the provided value if it is non-null, otherwise, returns the specified default value.
     *
     * @param <T> The type of the value and default value
     * @param value The value to check
     * @param defaultValue The default value to return if the value is null
     * @return The provided value if it is non-null, otherwise the default value
     */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return nonNull(value) ? value : defaultValue;
    }
}
