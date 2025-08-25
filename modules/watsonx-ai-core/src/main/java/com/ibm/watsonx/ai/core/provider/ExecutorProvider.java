/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.provider;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
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

/**
 * A utility class that provides a shared instance of Executors for the SDK's internal operations.
 * <p>
 * Provides separate executors for CPU-bound tasks and I/O-bound tasks.
 */
public class ExecutorProvider {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorProvider.class);
    private static final CpuExecutorProvider cpuExecutorProvider = loadCpuExecutorProvider();
    private static final IOExecutorProvider ioExecutorProvider = loadIoExecutorProvider();
    private static volatile Executor cpuExecutor;
    private static volatile Executor ioExecutor;

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
            ExecutorService defaultExecutor = Executors.newFixedThreadPool(nThreads, r -> new Thread(r, "io-thread-" + counter.getAndIncrement()));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                defaultExecutor.shutdown();
            }));
            ioExecutor = defaultExecutor;
        }
        return ioExecutor;
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
}
