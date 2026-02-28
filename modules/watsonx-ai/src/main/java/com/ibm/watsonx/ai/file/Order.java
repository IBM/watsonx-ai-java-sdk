/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

/**
 * Represents the sort order for the file watsonx.ai List APIs.
 */
public enum Order {

    ASC("asc"),
    DESC("desc");

    private final String value;

    Order(String value) {
        this.value = value;
    }

    /**
     * Returns the string value of this sort order.
     *
     * @return the sort order value
     */
    public String value() {
        return value;
    }
}