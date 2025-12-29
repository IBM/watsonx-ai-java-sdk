/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
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
        resetExecutor("ioExecutor");
        resetExecutor("callbackExecutor");
    }

    @Test
    void should_return_forkjoinpool_for_cpu_executor() throws Exception {
        var instance = ExecutorProvider.cpuExecutor();
        assertEquals(instance, ExecutorProvider.cpuExecutor());
        assertInstanceOf(ForkJoinPool.class, instance);
    }

    @Test
    void should_return_single_thread_executor_for_io_executor() throws Exception {
        environmentVariables.remove("WATSONX_IO_EXECUTOR_THREADS");
        var instance = ExecutorProvider.ioExecutor();
        assertEquals(instance, ExecutorProvider.ioExecutor());
        var threadPoolExecutor = (ThreadPoolExecutor) instance;
        assertEquals(1, threadPoolExecutor.getCorePoolSize());
        assertEquals(1, threadPoolExecutor.getMaximumPoolSize());
    }

    @Test
    void should_customize_the_thread_numbers_executor_for_io_executor() throws Exception {
        environmentVariables.set("WATSONX_IO_EXECUTOR_THREADS", 3);
        var instance = ExecutorProvider.ioExecutor();
        assertEquals(instance, ExecutorProvider.ioExecutor());
        var threadPoolExecutor = (ThreadPoolExecutor) instance;
        assertEquals(3, threadPoolExecutor.getCorePoolSize());
        assertEquals(3, threadPoolExecutor.getMaximumPoolSize());
    }

    @Test
    @EnabledForJreRange(max = JRE.JAVA_20)
    void should_use_callback_executor_with_correct_thread_name_pattern() throws Exception {
        var instance = ExecutorProvider.callbackExecutor();
        assertEquals(instance, ExecutorProvider.callbackExecutor());
        CompletableFuture<String> future = new CompletableFuture<>();
        ExecutorProvider.callbackExecutor().execute(() -> {
            future.complete(Thread.currentThread().getName());
        });
        assertTrue(future.get(3, TimeUnit.SECONDS).startsWith("thread"));
    }

    @Test
    @EnabledForJreRange(max = JRE.JAVA_20)
    void should_use_callback_executor_with_correct_virtual_thread_name_pattern() throws Exception {
        var instance = ExecutorProvider.callbackExecutor();
        assertEquals(instance, ExecutorProvider.callbackExecutor());
        CompletableFuture<String> future = new CompletableFuture<>();
        ExecutorProvider.callbackExecutor().execute(() -> {
            future.complete(Thread.currentThread().getName());
        });
        assertTrue(future.get(3, TimeUnit.SECONDS).startsWith("thread"));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void should_provide_shared_io_executor_that_uses_virtual_threads() throws Exception {
        var instance = ExecutorProvider.callbackExecutor();
        assertEquals(instance, ExecutorProvider.callbackExecutor());
        CompletableFuture<String> future = new CompletableFuture<>();
        ExecutorProvider.callbackExecutor().execute(() -> {
            try {
                var method = Thread.class.getMethod("isVirtual");
                assertTrue((boolean) method.invoke(Thread.currentThread()));
                assertTrue(Thread.currentThread().getName().startsWith("virtual-thread-"));
                future.complete("Done");
            } catch (Exception e) {
                fail(e);
            }
        });
        assertEquals("Done", future.get(5, TimeUnit.SECONDS));
    }

    private void resetExecutor(String fieldName) {
        try {
            Field field = ExecutorProvider.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            fail(e);
        }
    }
}
