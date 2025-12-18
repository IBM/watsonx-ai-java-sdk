/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.executor;

import java.net.http.HttpClient;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Service Provider Interface (SPI) for providing a custom {@link Executor} for {@link HttpClient} response processing.
 * <p>
 * This executor is used internally by the SDK to process HTTP responses and parse SSE (Server-Sent Events) streams.
 * <p>
 * User callbacks are executed on a separate executor (see {@link CallbackExecutorProvider}).
 *
 * <p>
 * <b>Default Behavior</b>
 * <p>
 * If no custom provider is registered, a single-threaded executor is used by default (configurable via {@code WATSONX_IO_EXECUTOR_THREADS}
 * environment variable).
 *
 * <p>
 * <b>Custom Implementation</b>
 * <p>
 * To provide a custom executor, implement this interface and register it via {@link ServiceLoader}:
 * <ol>
 * <li>Create an implementation class</li>
 * <li>Create a file {@code META-INF/services/com.ibm.watsonx.ai.core.spi.executor.IOExecutorProvider}</li>
 * <li>Add the fully qualified class name of your implementation to the file</li>
 * </ol>
 *
 * @see ExecutorProvider#ioExecutor()
 * @see CallbackExecutorProvider
 */
@FunctionalInterface
public interface IOExecutorProvider {

    /**
     * Returns the {@link Executor} to be used for HTTP response processing.
     *
     * @return the executor for HTTP response processing
     */
    Executor executor();
}