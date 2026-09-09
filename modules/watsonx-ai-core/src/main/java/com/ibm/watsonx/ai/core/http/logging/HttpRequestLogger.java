/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.logging;

/**
 * Callback invoked with the fully-processed data of an outgoing HTTP request, for custom logging.
 */
@FunctionalInterface
public interface HttpRequestLogger {

    /**
     * Logs the given request entry.
     *
     * @param entry the request data to log
     */
    void log(HttpRequestLog entry);
}
