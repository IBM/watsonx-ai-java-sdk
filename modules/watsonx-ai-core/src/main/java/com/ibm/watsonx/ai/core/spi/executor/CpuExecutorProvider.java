/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.executor;

import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Service Provider Interface (SPI) for providing a custom {@link Executor} for CPU-bound tasks.
 * <p>
 * This executor is used for CPU-intensive operations such as JSON parsing, data transformation, or other computational tasks that should not block
 * I/O threads.
 *
 * <h2>Default Behavior</h2>
 * <p>
 * If no custom provider is registered, the SDK uses {@link ForkJoinPool#commonPool()}.
 *
 * <h2>Custom Implementation</h2>
 * <p>
 * To provide a custom executor, implement this interface and register it via {@link ServiceLoader}:
 * <ol>
 * <li>Create an implementation class</li>
 * <li>Create a file {@code META-INF/services/com.ibm.watsonx.ai.core.spi.executor.CpuExecutorProvider}</li>
 * <li>Add the fully qualified class name of your implementation to the file</li>
 * </ol>
 *
 * @see ExecutorProvider#cpuExecutor()
 */
@FunctionalInterface
public interface CpuExecutorProvider {

    /**
     * Returns the {@link Executor} to be used for CPU-bound tasks.
     * <p>
     * The returned executor should be optimized for computational tasks. Avoid using executors designed for blocking I/O operations.
     *
     * @return the executor for CPU-bound tasks
     */
    Executor executor();
}