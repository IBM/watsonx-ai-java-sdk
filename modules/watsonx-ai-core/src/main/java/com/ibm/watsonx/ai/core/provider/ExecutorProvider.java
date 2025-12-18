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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.core.spi.executor.CallbackExecutorProvider;
import com.ibm.watsonx.ai.core.spi.executor.CpuExecutorProvider;
import com.ibm.watsonx.ai.core.spi.executor.IOExecutorProvider;

/**
 * A utility class that provides shared Executor instances for the SDK's internal operations.
 * <p>
 * Provides three separate executors:
 * <ul>
 * <li>{@link #cpuExecutor()} - for CPU-intensive tasks (JSON parsing, computation)</li>
 * <li>{@link #ioExecutor()} - for SSE stream parsing (single-threaded by default)</li>
 * <li>{@link #callbackExecutor()} - for user callbacks (virtual threads on Java 21+)</li>
 * </ul>
 */
public final class ExecutorProvider {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorProvider.class);
    private static final CpuExecutorProvider cpuExecutorProvider = loadCpuExecutorProvider();
    private static final IOExecutorProvider ioExecutorProvider = loadIOExecutorProvider();
    private static final CallbackExecutorProvider callbackExecutorProvider = loadCallbackExecutorProvider();
    private static volatile Executor cpuExecutor;
    private static volatile Executor ioExecutor;
    private static volatile Executor callbackExecutor;

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
     * Executor for SSE stream parsing and HTTP response processing.
     * <p>
     * By default, uses a single thread to ensure sequential processing of SSE events. Can be configured to use multiple threads via the
     * {@code WATSONX_IO_EXECUTOR_THREADS} environment variable.
     * <p>
     * <b>Note:</b> User callbacks are executed on {@link #callbackExecutor()}, not this executor.
     *
     * @return the executor for SSE parsing
     */
    public static synchronized Executor ioExecutor() {
        if (isNull(ioExecutor)) {

            if (nonNull(ioExecutorProvider)) {
                logger.trace("Loaded IO executor from SPI");
                ioExecutor = ioExecutorProvider.executor();
                return ioExecutor;
            }

            var nThreads = ofNullable(System.getenv("WATSONX_IO_EXECUTOR_THREADS"))
                .map(Integer::valueOf)
                .orElse(1);

            if (nThreads > 1) {
                AtomicInteger counter = new AtomicInteger(1);
                ioExecutor = Executors.newFixedThreadPool(nThreads, r -> {
                    var thread = new Thread(r, "http-io-thread-" + counter.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                });
            } else
                ioExecutor = Executors.newFixedThreadPool(nThreads, r -> {
                    var thread = new Thread(r, "http-io-thread");
                    thread.setDaemon(true);
                    return thread;
                });
        }
        return ioExecutor;
    }

    /**
     * Executor for user-defined callbacks in streaming operations.
     * <p>
     * This executor is used to run {@code ChatHandler} and {@code TextGenerationHandler} callbacks, ensuring they don't block the SSE parsing thread.
     * <p>
     * <b>Default Behavior:</b>
     * <ul>
     * <li><b>Java 21+:</b> Virtual threads (lightweight, non-blocking)</li>
     * <li><b>Java 17-20:</b> Cached thread pool (reuses threads)</li>
     * </ul>
     *
     * @return the executor for user callbacks
     */
    public static synchronized Executor callbackExecutor() {
        if (isNull(callbackExecutor)) {

            if (nonNull(callbackExecutorProvider)) {
                logger.trace("Loaded callback executor from SPI");
                callbackExecutor = callbackExecutorProvider.executor();
                return callbackExecutor;
            }

            try {
                // Java 21+: virtual threads
                Method ofVirtual = Thread.class.getMethod("ofVirtual");
                Object builder = ofVirtual.invoke(null);
                Class<?> builderInterface = Class.forName("java.lang.Thread$Builder");
                Class<?> ofVirtualInterface = Class.forName("java.lang.Thread$Builder$OfVirtual");
                Method nameMethod = ofVirtualInterface.getMethod("name", String.class, long.class);
                builder = nameMethod.invoke(builder, "virtual-thread-", 1L);
                Method factoryMethod = builderInterface.getMethod("factory");
                ThreadFactory factory = (ThreadFactory) factoryMethod.invoke(builder);
                Method newThreadPerTaskExecutor = Executors.class.getMethod("newThreadPerTaskExecutor", ThreadFactory.class);
                callbackExecutor = (ExecutorService) newThreadPerTaskExecutor.invoke(null, factory);
                logger.debug("Using virtual thread executor for callbacks (Java 21+)");

            } catch (Exception e) {
                // Java < 21: cached thread pool
                logger.debug("Virtual threads not available, using cached thread pool for callbacks");
                AtomicInteger counter = new AtomicInteger(1);
                callbackExecutor = Executors.newCachedThreadPool(r -> {
                    var thread = new Thread(r, "thread-" + counter.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                });
            }
        }
        return callbackExecutor;
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
    private static IOExecutorProvider loadIOExecutorProvider() {
        return ServiceLoader.load(IOExecutorProvider.class)
            .findFirst().orElse(null);
    }

    /**
     * Attempts to load a {@link CallbackExecutorProvider} via {@link ServiceLoader}.
     */
    private static CallbackExecutorProvider loadCallbackExecutorProvider() {
        return ServiceLoader.load(CallbackExecutorProvider.class)
            .findFirst().orElse(null);
    }
}
