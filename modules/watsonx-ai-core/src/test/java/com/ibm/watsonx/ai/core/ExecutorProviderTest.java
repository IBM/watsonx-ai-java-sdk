/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class ExecutorProviderTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    void setUp() {
        environmentVariables.set("IO_CORE_THREADS", 3);
        Stream.of("cpuExecutor", "ioExecutor").forEach(v -> {
            try {
                var field = ExecutorProvider.class.getDeclaredField(v);
                field.setAccessible(true);
                field.set(null, null);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_create_io_executor_with_custom_core_threads_from_environment() throws Exception {
        var instance = (ThreadPoolExecutor) ExecutorProvider.ioExecutor();
        assertEquals(3, instance.getCorePoolSize());
    }

    @Test
    void should_return_singleton_forkjoinpool_for_cpu_executor() throws Exception {
        var instance = ExecutorProvider.cpuExecutor();
        assertEquals(instance, ExecutorProvider.cpuExecutor());
        assertInstanceOf(ForkJoinPool.class, instance);
    }


    @Test
    void should_provide_shared_io_executor_with_correct_thread_name_pattern() throws Exception {
        var instance = ExecutorProvider.ioExecutor();
        assertEquals(instance, ExecutorProvider.ioExecutor());
        CompletableFuture<String> future = new CompletableFuture<>();
        ExecutorProvider.ioExecutor().execute(() -> {
            future.complete(Thread.currentThread().getName());
        });
        assertTrue(future.get(3, TimeUnit.SECONDS).startsWith("io-thread"));
    }
}
