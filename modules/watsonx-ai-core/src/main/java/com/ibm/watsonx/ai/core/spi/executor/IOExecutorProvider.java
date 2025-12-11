/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.executor;

import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Service Provider Interface (SPI) for providing a custom {@link Executor} for I/O-bound tasks.
 * <p>
 * This executor is used for network calls, HTTP requests, and other operations that may block while waiting for external resources.
 *
 * <h2>Default Behavior</h2>
 * <p>
 * If no custom provider is registered, the SDK uses a fixed thread pool with a size equal to the number of available processors. The pool size can be
 * customized via the {@code IO_CORE_THREADS} environment variable.
 *
 * <h2>Custom Implementation</h2>
 * <p>
 * To provide a custom executor, implement this interface and register it via {@link ServiceLoader}:
 * <ol>
 * <li>Create an implementation class</li>
 * <li>Create a file {@code META-INF/services/com.ibm.watsonx.ai.core.spi.executor.IOExecutorProvider}</li>
 * <li>Add the fully qualified class name of your implementation to the file</li>
 * </ol>
 *
 * @see ExecutorProvider#ioExecutor()
 */
@FunctionalInterface
public interface IOExecutorProvider {

    /**
     * Returns the {@link Executor} to be used for I/O-bound tasks.
     * <p>
     * The returned executor should be capable of handling blocking I/O operations.
     *
     * @return the executor for I/O-bound tasks
     */
    Executor executor();
}