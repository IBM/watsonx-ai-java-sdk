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
 * Service Provider Interface (SPI) for providing a custom {@link Executor} for {@link HttpClient} callback tasks.
 *
 * <b>Default Behavior</b>
 * <p>
 * If no custom provider is registered:
 * <ul>
 * <li><b>Java 21+:</b> A virtual thread executor is used ({@code Executors.newVirtualThreadPerTaskExecutor()})</li>
 * <li><b>Java 17-20:</b> A cached thread pool is used ({@code Executors.newCachedThreadPool()})</li>
 * </ul>
 *
 * <b>Custom Implementation</b>
 * <p>
 * To provide a custom executor, implement this interface and register it via {@link ServiceLoader}:
 * <ol>
 * <li>Create an implementation class</li>
 * <li>Create a file {@code META-INF/services/com.ibm.watsonx.ai.core.spi.executor.CallbackExecutorProvider}</li>
 * <li>Add the fully qualified class name of your implementation to the file</li>
 * </ol>
 *
 * @see ExecutorProvider#callbackExecutor()
 */
@FunctionalInterface
public interface CallbackExecutorProvider {

    /**
     * Returns the {@link Executor} to be used for callback tasks.
     *
     * @return the executor for callback tasks
     */
    Executor executor();
}