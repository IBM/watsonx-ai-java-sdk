/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.executor;

import java.util.concurrent.Executor;

/**
 * Service Provider Interface (SPI) for providing a custom {@link Executor} used by the SDK's HTTP client to execute asynchronous CPU-bound
 * operations.
 */
@FunctionalInterface
public interface CpuExecutorProvider {

    /**
     * Returns the {@link Executor} instance to be used for CPU-bound tasks.
     *
     * @return the Executor for CPU-bound tasks
     */
    Executor executor();
}
