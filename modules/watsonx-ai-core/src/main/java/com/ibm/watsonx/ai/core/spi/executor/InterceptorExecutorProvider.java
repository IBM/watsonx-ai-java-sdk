/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.spi.executor;

import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Service Provider Interface (SPI) for providing a custom {@link Executor} for interceptor tasks.
 * <p>
 * Interceptors may perform blocking operations (such as synchronous HTTP calls via {@code InterceptorContext.invoke()}). To avoid blocking the main
 * I/O executor, interceptors run on a separate executor provided by this interface.
 *
 * <h2>Default Behavior</h2>
 * <p>
 * If no custom provider is registered:
 * <ul>
 * <li><b>Java 21+:</b> A virtual thread executor is used ({@code Executors.newVirtualThreadPerTaskExecutor()})</li>
 * <li><b>Java 17-20:</b> A cached thread pool is used ({@code Executors.newCachedThreadPool()})</li>
 * </ul>
 *
 * <h2>Custom Implementation</h2>
 * <p>
 * To provide a custom executor, implement this interface and register it via {@link ServiceLoader}:
 * <ol>
 * <li>Create an implementation class</li>
 * <li>Create a file {@code META-INF/services/com.ibm.watsonx.ai.core.spi.executor.InterceptorExecutorProvider}</li>
 * <li>Add the fully qualified class name of your implementation to the file</li>
 * </ol>
 *
 * @see ExecutorProvider#interceptorExecutor()
 */
@FunctionalInterface
public interface InterceptorExecutorProvider {

    /**
     * Returns the {@link Executor} to be used for interceptor tasks.
     * <p>
     * The returned executor should be capable of handling blocking operations without causing thread starvation.
     *
     * @return the executor for interceptor tasks
     */
    Executor executor();
}