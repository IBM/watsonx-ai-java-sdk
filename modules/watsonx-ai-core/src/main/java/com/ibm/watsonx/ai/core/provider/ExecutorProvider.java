/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.provider;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.lang.reflect.Method;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.core.spi.executor.CallbackExecutorProvider;
import com.ibm.watsonx.ai.core.spi.executor.CpuExecutorProvider;
import com.ibm.watsonx.ai.core.spi.executor.IOExecutorProvider;

/**
 * Provides the three shared {@link Executor} instances used across the SDK. Each is created lazily on first use and then reused for the lifetime of
 * the JVM, so that work is dispatched onto a well-defined pool instead of ad-hoc threads.
 *
 * <p>
 * The executors are separated by the kind of work they run:
 * <ul>
 * <li><b>{@link #cpuExecutor()}</b> - CPU-bound work such as JSON (de)serialization. Defaults to the {@link ForkJoinPool#commonPool() common
 * pool}.</li>
 * <li><b>{@link #ioExecutor()}</b> - the executor of the SDK's singleton HTTP client (response delivery, SSE chunk parsing) and of the SDK's other
 * internal asynchronous tasks (token refresh, retry scheduling, request/response logging). Defaults to virtual threads on Java 21+ and to a cached
 * thread pool on Java 17-20; the size can be capped with the {@code WATSONX_IO_EXECUTOR_THREADS} environment variable.</li>
 * <li><b>{@link #callbackExecutor()}</b> - runs user-supplied streaming callbacks ({@code ChatHandler} / {@code TextGenerationHandler}) so they never
 * block the I/O threads. Defaults to virtual threads on Java 21+ and to a cached thread pool on Java 17-20.</li>
 * </ul>
 *
 * <p>
 * Every executor can be replaced by registering the matching SPI provider ({@link CpuExecutorProvider}, {@link IOExecutorProvider},
 * {@link CallbackExecutorProvider}) via {@link ServiceLoader}; when present, the SPI takes precedence over the defaults described above.
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
     * Shared executor backing the SDK's singleton {@link java.net.http.HttpClient} and the SDK's other internal asynchronous tasks (authentication
     * token refresh, retry scheduling, request/response logging and the {@code thenApplyAsync} continuations of the non-streaming services). For
     * streaming requests it is therefore also where each SSE chunk is parsed, before user callbacks are dispatched on {@link #callbackExecutor()}.
     * <p>
     * <b>Default behavior:</b>
     * <ul>
     * <li><b>Java 21+:</b> a virtual-thread-per-task executor.</li>
     * <li><b>Java 17-20:</b> a cached thread pool.</li>
     * </ul>
     * The signals of a single streaming response stay sequential regardless of the pool size, so a larger pool only improves throughput when many
     * requests run concurrently.
     * <p>
     * Setting the {@code WATSONX_IO_EXECUTOR_THREADS} environment variable to a positive integer overrides the default with a fixed-size thread pool
     * of that size
     *
     * @return the shared I/O executor
     */
    public static synchronized Executor ioExecutor() {
        if (isNull(ioExecutor)) {

            if (nonNull(ioExecutorProvider)) {
                logger.trace("Loaded IO executor from SPI");
                ioExecutor = ioExecutorProvider.executor();
                return ioExecutor;
            }

            Integer override = parseIoThreadsOverride();
            if (nonNull(override)) {
                ioExecutor = Executors.newFixedThreadPool(override, namedDaemonThreadFactory("http-io-thread-"));
                logger.debug("Using fixed IO executor with {} threads (WATSONX_IO_EXECUTOR_THREADS)", override);
                return ioExecutor;
            }

            var virtual = newVirtualThreadPerTaskExecutorOrNull("http-io-virtual-thread-");
            if (nonNull(virtual)) {
                ioExecutor = virtual;
                logger.debug("Using virtual thread IO executor (Java 21+)");
            } else {
                logger.debug("Virtual threads not available, using cached IO thread pool");
                ioExecutor = Executors.newCachedThreadPool(namedDaemonThreadFactory("http-io-thread-"));
            }
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
     * <li><b>Java 21+:</b> Virtual threads</li>
     * <li><b>Java 17-20:</b> Cached thread pool</li>
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

            var virtual = newVirtualThreadPerTaskExecutorOrNull("virtual-thread-");
            if (nonNull(virtual)) {
                callbackExecutor = virtual;
                logger.debug("Using virtual thread executor for callbacks (Java 21+)");
            } else {
                // Java < 21: cached thread pool
                logger.debug("Virtual threads not available, using cached thread pool");
                AtomicInteger counter = new AtomicInteger(1);
                callbackExecutor = new ThreadPoolExecutor(
                    1,
                    Integer.MAX_VALUE,
                    60L,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    r -> {
                        var thread = new Thread(r, "thread-" + counter.getAndIncrement());
                        thread.setDaemon(true);
                        return thread;
                    }
                );
            }
        }
        return callbackExecutor;
    }

    /**
     * Creates a virtual-thread-per-task executor whose threads are named {@code <namePrefix><n>}, or returns {@code null} when virtual threads are
     * not available (Java &lt; 21). Reflection is used so the SDK keeps compiling against Java 17.
     *
     * @param namePrefix the prefix used to name the virtual threads
     * @return a virtual-thread-per-task {@link ExecutorService}, or {@code null} on Java &lt; 21
     */
    private static ExecutorService newVirtualThreadPerTaskExecutorOrNull(String namePrefix) {
        try {
            Method ofVirtual = Thread.class.getMethod("ofVirtual");
            Object builder = ofVirtual.invoke(null);
            Class<?> builderInterface = Class.forName("java.lang.Thread$Builder");
            Class<?> ofVirtualInterface = Class.forName("java.lang.Thread$Builder$OfVirtual");
            Method nameMethod = ofVirtualInterface.getMethod("name", String.class, long.class);
            builder = nameMethod.invoke(builder, namePrefix, 1L);
            Method factoryMethod = builderInterface.getMethod("factory");
            ThreadFactory factory = (ThreadFactory) factoryMethod.invoke(builder);
            Method newThreadPerTaskExecutor = Executors.class.getMethod("newThreadPerTaskExecutor", ThreadFactory.class);
            return (ExecutorService) newThreadPerTaskExecutor.invoke(null, factory);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns a {@link ThreadFactory} producing daemon threads named {@code <prefix><n>}.
     *
     * @param prefix the thread name prefix
     * @return a daemon thread factory
     */
    private static ThreadFactory namedDaemonThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return r -> {
            var thread = new Thread(r, prefix + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * Parses the {@code WATSONX_IO_EXECUTOR_THREADS} override.
     *
     * @return the requested fixed pool size, or {@code null} when the variable is unset or invalid (non-numeric or {@code < 1}), in which case the
     *         default executor is used
     */
    private static Integer parseIoThreadsOverride() {
        var raw = System.getenv("WATSONX_IO_EXECUTOR_THREADS");
        if (isNull(raw) || raw.isBlank())
            return null;
        try {
            int n = Integer.parseInt(raw.trim());
            if (n < 1) {
                logger.warn("Ignoring WATSONX_IO_EXECUTOR_THREADS=\"{}\": value must be >= 1. Using the default IO executor.", raw);
                return null;
            }
            return n;
        } catch (NumberFormatException e) {
            logger.warn("Ignoring WATSONX_IO_EXECUTOR_THREADS=\"{}\": not a valid integer. Using the default IO executor.", raw);
            return null;
        }
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
