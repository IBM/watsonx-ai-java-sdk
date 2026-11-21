/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.isNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.internal.SharedTokenCache;

class SharedTokenCacheTest {

    @Test
    void should_isolate_caller_completion_and_cancellation_from_the_cache() throws Exception {
        var source = new CompletableFuture<String>();
        var calls = new AtomicInteger();
        var cache = new SharedTokenCache<String>(Objects::isNull, () -> {
            calls.incrementAndGet();
            return source;
        });

        var completedByCaller = cache.tokenAsync();
        var cancelledByCaller = cache.tokenAsync();
        var waiting = cache.tokenAsync();
        assertTrue(completedByCaller.complete("forged-token"));
        assertTrue(cancelledByCaller.cancel(true));
        assertFalse(source.isDone());

        source.complete("server-token");

        assertEquals("server-token", waiting.get(3, TimeUnit.SECONDS));
        assertEquals("server-token", cache.token());
        assertEquals("forged-token", completedByCaller.get(3, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
    }

    @Test
    void should_release_all_waiters_after_a_synchronous_startup_rejection_and_allow_retry() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var rejected = new RejectedExecutionException("Token executor rejected the acquisition");
        var cache = new SharedTokenCache<String>(Objects::isNull, () -> {
            if (calls.incrementAndGet() != 1)
                return completedFuture("recovered");
            started.countDown();
            try {
                assertTrue(release.await(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            throw rejected;
        });
        var worker = Executors.newSingleThreadExecutor();

        try {
            var initiatingCall = worker.submit(cache::tokenAsync);
            assertTrue(started.await(3, TimeUnit.SECONDS));
            var waiting = new ArrayList<CompletableFuture<String>>();
            for (int i = 0; i < 20; i++)
                waiting.add(cache.tokenAsync());

            release.countDown();
            waiting.add(initiatingCall.get(3, TimeUnit.SECONDS));
            for (var waiter : waiting)
                assertSame(rejected, assertThrows(ExecutionException.class, () -> waiter.get(3, TimeUnit.SECONDS)).getCause());

            assertEquals("recovered", cache.token());
            assertEquals(2, calls.get());
        } finally {
            release.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    void should_allow_retry_after_the_acquisition_source_is_cancelled() throws Exception {
        var source = new CompletableFuture<String>();
        var calls = new AtomicInteger();
        var cache = new SharedTokenCache<String>(Objects::isNull,
            () -> calls.incrementAndGet() == 1 ? source : completedFuture("recovered"));
        var first = cache.tokenAsync();
        var second = cache.tokenAsync();

        source.cancel(true);

        assertInstanceOf(CancellationException.class,
            assertThrows(ExecutionException.class, () -> first.get(3, TimeUnit.SECONDS)).getCause());
        assertInstanceOf(CancellationException.class,
            assertThrows(ExecutionException.class, () -> second.get(3, TimeUnit.SECONDS)).getCause());
        assertEquals("recovered", cache.token());
        assertEquals(2, calls.get());
    }

    @Test
    void should_preserve_checked_failures_for_synchronous_callers_and_allow_retry() {
        var failure = new IOException("Token transport failed");
        var calls = new AtomicInteger();
        var cache = new SharedTokenCache<String>(Objects::isNull,
            () -> calls.incrementAndGet() == 1 ? failedFuture(failure) : completedFuture("recovered"));

        assertSame(failure, assertThrows(RuntimeException.class, cache::token).getCause());
        assertEquals("recovered", cache.token());
    }

    @Test
    void should_preserve_runtime_failures_for_synchronous_callers() {
        var failure = new IllegalArgumentException("Invalid token credentials");
        var cache = new SharedTokenCache<String>(Objects::isNull, () -> failedFuture(failure));

        assertSame(failure, assertThrows(IllegalArgumentException.class, cache::token));
    }

    @Test
    void should_recover_after_a_provider_returns_a_null_future() throws Exception {
        var calls = new AtomicInteger();
        var cache = new SharedTokenCache<String>(Objects::isNull,
            () -> calls.incrementAndGet() == 1 ? null : completedFuture("recovered"));

        assertInstanceOf(NullPointerException.class,
            assertThrows(ExecutionException.class, () -> cache.tokenAsync().get(3, TimeUnit.SECONDS)).getCause());
        assertEquals("recovered", cache.token());
    }

    @Test
    void should_recover_after_a_provider_returns_a_null_response() throws Exception {
        var calls = new AtomicInteger();
        var cache = new SharedTokenCache<String>(Objects::isNull,
            () -> completedFuture(calls.incrementAndGet() == 1 ? null : "recovered"));

        assertInstanceOf(NullPointerException.class,
            assertThrows(ExecutionException.class, () -> cache.tokenAsync().get(3, TimeUnit.SECONDS)).getCause());
        assertEquals("recovered", cache.token());
    }

    @Test
    void should_allow_reentrant_refresh_without_clearing_a_newer_acquisition() throws Exception {
        var first = new CompletableFuture<String>();
        var second = new CompletableFuture<String>();
        var calls = new AtomicInteger();
        var cache = new SharedTokenCache<String>(token -> isNull(token) || "expired".equals(token),
            () -> calls.incrementAndGet() == 1 ? first : second);
        var next = new AtomicReference<CompletableFuture<String>>();
        var callback = cache.tokenAsync().thenRun(() -> next.set(cache.tokenAsync()));

        first.complete("expired");
        callback.get(3, TimeUnit.SECONDS);
        assertFalse(next.get().isDone());
        var concurrent = cache.tokenAsync();
        assertEquals(2, calls.get());

        second.complete("fresh");

        assertEquals("fresh", next.get().get(3, TimeUnit.SECONDS));
        assertEquals("fresh", concurrent.get(3, TimeUnit.SECONDS));
        assertEquals("fresh", cache.token());
        assertEquals(2, calls.get());
    }

    @Test
    void should_allow_reentrant_retry_from_a_failure_callback() throws Exception {
        var first = new CompletableFuture<String>();
        var second = new CompletableFuture<String>();
        var calls = new AtomicInteger();
        var cache = new SharedTokenCache<String>(Objects::isNull,
            () -> calls.incrementAndGet() == 1 ? first : second);
        var next = new AtomicReference<CompletableFuture<String>>();
        var waiting = cache.tokenAsync();
        var callback = waiting.whenComplete((token, error) -> next.set(cache.tokenAsync()));
        var failure = new IOException("First login failed");

        first.completeExceptionally(failure);

        assertSame(failure, assertThrows(ExecutionException.class, () -> callback.get(3, TimeUnit.SECONDS)).getCause());
        assertFalse(next.get().isDone());
        assertEquals(2, calls.get());
        second.complete("recovered");
        assertEquals("recovered", next.get().get(3, TimeUnit.SECONDS));
        assertEquals("recovered", cache.token());
    }
}
