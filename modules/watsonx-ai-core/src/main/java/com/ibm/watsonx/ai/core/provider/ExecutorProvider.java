/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.provider;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import java.lang.reflect.Method;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.core.spi.executor.CpuExecutorProvider;
import com.ibm.watsonx.ai.core.spi.executor.IOExecutorProvider;
import com.ibm.watsonx.ai.core.spi.executor.InterceptorExecutorProvider;

/**
 * A utility class that provides a shared instance of Executors for the SDK's internal operations.
 * <p>
 * Provides separate executors for CPU-bound tasks and I/O-bound tasks.
 */
public final class ExecutorProvider {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorProvider.class);
    private static final CpuExecutorProvider cpuExecutorProvider = loadCpuExecutorProvider();
    private static final IOExecutorProvider ioExecutorProvider = loadIoExecutorProvider();
    private static final InterceptorExecutorProvider interceptorExecutorProvider = loadInterceptorExecutorProvider();
    private static volatile Executor cpuExecutor;
    private static volatile Executor ioExecutor;
    private static volatile Executor interceptorExecutor;

    private ExecutorProvider() {}

    /**
     * Retrieves the shared executor for CPU-bound tasks.
     * <p>
     * This executor is intended for CPU-intensive operations such as JSON parsing, computation, or other processing tasks that should not block I/O
     * threads.
     *
     * @return The shared {@link ExecutorService} for CPU-bound tasks.
     */
    public static synchronized Executor cpuExecutor() {
        if (isNull(cpuExecutor)) {

            if (nonNull(cpuExecutorProvider)) {
                logger.trace("Loaded CPU executor from SPI");
                cpuExecutor = cpuExecutorProvider.executor();
                return cpuExecutor;
            }

            cpuExecutor = ForkJoinPool.commonPool();
        }

        return cpuExecutor;
    }

    /**
     * Retrieves the shared executor for I/O-bound tasks.
     * <p>
     * This executor is intended for network calls, HTTP requests, or other I/O operations that may block.
     *
     * @return The shared {@link ExecutorService} for I/O-bound tasks.
     */
    public static synchronized Executor ioExecutor() {
        if (isNull(ioExecutor)) {

            if (nonNull(ioExecutorProvider)) {
                logger.trace("Loaded IO executor from SPI");
                ioExecutor = ioExecutorProvider.executor();
                return ioExecutor;
            }

            var nThreads = ofNullable(System.getenv("IO_CORE_THREADS"))
                .map(Integer::valueOf)
                .orElse(Runtime.getRuntime().availableProcessors());

            AtomicInteger counter = new AtomicInteger(1);
            ExecutorService defaultExecutor = Executors.newFixedThreadPool(nThreads, r -> {
                var thread = new Thread(r, "io-thread-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            });

            ioExecutor = defaultExecutor;
        }
        return ioExecutor;
    }

    /**
     * Retrieves the shared executor for interceptor tasks.
     * <p>
     * This executor is specifically designed for running interceptors that may perform blocking operations (such as synchronous HTTP calls). It uses
     * a separate thread pool to avoid blocking the main I/O executor.
     * <p>
     * On Java 21+, this executor uses virtual threads for optimal scalability with blocking operations. On earlier Java versions, a cached thread
     * pool is used as fallback.
     *
     * @return The shared {@link Executor} for interceptor tasks.
     */
    public static synchronized Executor interceptorExecutor() {
        if (isNull(interceptorExecutor)) {

            if (nonNull(interceptorExecutorProvider)) {
                logger.trace("Loaded Interceptor executor from SPI");
                interceptorExecutor = interceptorExecutorProvider.executor();
                return interceptorExecutor;
            }

            try {
                // Try to create virtual thread executor (Java 21+)
                Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
                logger.debug("Using virtual thread executor for interceptors (Java 21+)");
                interceptorExecutor = (ExecutorService) method.invoke(null);

            } catch (Exception e) {
                // Java < 21, fall back to cached thread pool
                AtomicInteger counter = new AtomicInteger(1);
                logger.debug("Virtual threads not available, using cached thread pool for interceptors");
                interceptorExecutor = Executors.newCachedThreadPool(r -> {
                    var thread = new Thread(r, "interceptor-thread-" + counter.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                });

            }
        }
        return interceptorExecutor;
    }

    /**
     * Attempts to load a {@link CpuExecutorProvider} via {@link ServiceLoader}.
     */
    private static CpuExecutorProvider loadCpuExecutorProvider() {
        return ServiceLoader.load(CpuExecutorProvider.class)
            .findFirst().orElse(null);
    }

    /**
     * Attempts to load a {@link IOExecutorProvider} via {@link ServiceLoader}.
     */
    private static IOExecutorProvider loadIoExecutorProvider() {
        return ServiceLoader.load(IOExecutorProvider.class)
            .findFirst().orElse(null);
    }

    /**
     * Attempts to load a {@link InterceptorExecutorProvider} via {@link ServiceLoader}.
     */
    private static InterceptorExecutorProvider loadInterceptorExecutorProvider() {
        return ServiceLoader.load(InterceptorExecutorProvider.class)
            .findFirst().orElse(null);
    }
}
