/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.http.logging;

/**
 * Callback invoked with the fully-processed data of an HTTP response or SSE event, for custom logging.
 */
@FunctionalInterface
public interface HttpResponseLogger {

    /**
     * Logs the given response entry.
     *
     * @param entry the response data to log
     */
    void log(HttpResponseLog entry);
}
