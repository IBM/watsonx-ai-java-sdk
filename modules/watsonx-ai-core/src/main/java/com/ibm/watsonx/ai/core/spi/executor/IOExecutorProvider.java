/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.executor;


import java.util.concurrent.Executor;

/**
 * Service Provider Interface (SPI) for providing a custom Executor used by the SDK's HTTP client to execute asynchronous I/O-bound operations.
 * <p>
 * The executor is intended for network calls, HTTP requests, or other blocking I/O operations.
 */
@FunctionalInterface
public interface IOExecutorProvider {

    /**
     * Returns the Executor instance to be used for I/O-bound tasks.
     * <p>
     * Implementations may configure corePoolSize, maxPoolSize, and keepAlive time as needed. The SDK may provide a default bounded executor if none
     * is supplied.
     *
     * @return the Executor for I/O-bound tasks
     */
    Executor executor();
}

