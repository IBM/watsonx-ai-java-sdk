/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.foundationmodel.filter;

/**
 * A lightweight functional contract that turns a filter predicate into its string representation.
 *
 * @see Filter
 */
@FunctionalInterface
public interface FilterExpression {

    /**
     * Serializes the filter expression to a string value.
     *
     * @return A string representation of the filter expression.
     */
    public String expression();
}
