/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PollingScheduleTest {

    @Test
    void should_double_poll_delays_up_to_three_seconds() throws Exception {
        var nanos = new AtomicLong();
        var delays = new ArrayList<Duration>();
        var polling = new Polling<>(Duration.ofSeconds(30), () -> new IOException("timeout"), nanos::get, delay -> {
            delays.add(Duration.ofNanos(delay));
            nanos.addAndGet(delay);
        });

        for (int i = 0; i < 8; i++)
            polling.awaitNext();

        assertEquals(List.of(100L, 200L, 400L, 800L, 1600L, 3000L, 3000L, 3000L), delays.stream().map(Duration::toMillis).toList());
    }

    @Test
    void should_expire_during_the_wait_without_starting_a_poll() {
        var nanos = new AtomicLong();
        var timeout = new IOException("timeout");
        var polling = new Polling<>(Duration.ofMillis(20), () -> timeout, nanos::get, nanos::addAndGet);

        assertSame(timeout, assertThrows(IOException.class, polling::awaitNext));
        assertEquals(Duration.ofMillis(20).toNanos(), nanos.get());
    }

    @Test
    void should_return_remaining_budget_and_reject_a_late_response() throws Exception {
        var nanos = new AtomicLong();
        var timeout = new IOException("timeout");
        var polling = new Polling<>(Duration.ofSeconds(1), () -> timeout, nanos::get, nanos::addAndGet);

        assertEquals(Duration.ofMillis(900), polling.awaitNext());
        nanos.set(Duration.ofSeconds(1).toNanos());

        assertSame(timeout, assertThrows(IOException.class, polling::checkDeadline));
    }

    @Test
    void should_preserve_the_fetch_failure_as_timeout_cause_only_after_expiry() throws Exception {
        var nanos = new AtomicLong();
        var failure = new IllegalStateException("fetch failed");
        var polling = new Polling<>(Duration.ofSeconds(1), () -> new IOException("timeout"), nanos::get, nanos::addAndGet);

        assertSame(failure, polling.translateFailure(failure));
        nanos.set(Duration.ofSeconds(1).toNanos());

        assertSame(failure, assertThrows(IOException.class, () -> polling.translateFailure(failure)).getCause());
    }

    @Test
    void should_handle_monotonic_clock_wraparound() throws Exception {
        var nanos = new AtomicLong(Long.MAX_VALUE - Duration.ofMillis(10).toNanos());
        var polling = new Polling<>(Duration.ofSeconds(1), () -> new IOException("timeout"), nanos::get, nanos::addAndGet);

        assertEquals(Duration.ofMillis(900), polling.awaitNext());
    }

    @Test
    void should_propagate_wait_interruption() {
        var interrupted = new InterruptedException("interrupted");
        var polling = new Polling<>(Duration.ofSeconds(1), () -> new IOException("timeout"), () -> 0L, delay -> {
            throw interrupted;
        });

        assertSame(interrupted, assertThrows(InterruptedException.class, polling::awaitNext));
    }

    @Test
    void should_reject_an_exhausted_budget_without_waiting() {
        var polling = new Polling<>(Duration.ZERO, () -> new IOException("timeout"), () -> 0L, delay -> fail("Must not wait"));

        assertThrows(IOException.class, polling::awaitNext);
    }
}
