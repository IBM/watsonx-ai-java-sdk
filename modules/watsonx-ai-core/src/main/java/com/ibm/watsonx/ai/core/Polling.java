/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static java.util.Objects.requireNonNull;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Polling schedule for asynchronous operations. Each instance owns one monotonic deadline and an exponential delay capped at three seconds. Callers
 * retain responsibility for operation state, cancellation and cleanup. An instance represents one operation and must be used by one thread.
 *
 * @param <E> the operation-specific timeout exception
 */
public final class Polling<E extends Exception> {

    private final long deadlineNanos;
    private final LongSupplier nanoTime;
    private final Sleeper sleeper;
    private final Supplier<E> timeoutFailure;
    private long delayNanos = TimeUnit.MILLISECONDS.toNanos(100);

    /**
     * Creates a schedule with custom clock and wait strategies.
     *
     * @param timeout the polling budget
     * @param timeoutFailure creates the operation-specific timeout exception
     * @param nanoTime supplies monotonic time in nanoseconds
     * @param sleeper performs an interruptible wait measured in nanoseconds
     */
    public Polling(Duration timeout, Supplier<E> timeoutFailure, LongSupplier nanoTime, Sleeper sleeper) {
        this.nanoTime = requireNonNull(nanoTime);
        this.sleeper = requireNonNull(sleeper);
        this.timeoutFailure = requireNonNull(timeoutFailure);
        deadlineNanos = nanoTime.getAsLong() + requireNonNull(timeout).toNanos();
    }

    /**
     * Starts a polling schedule with the supplied timeout and exception factory.
     *
     * @param timeout the polling budget
     * @param timeoutFailure creates the operation-specific timeout exception
     * @param <E> the timeout exception type
     * @return a new schedule for one job
     */
    public static <E extends Exception> Polling<E> start(Duration timeout, Supplier<E> timeoutFailure) {
        return new Polling<>(timeout, timeoutFailure, System::nanoTime, TimeUnit.NANOSECONDS::sleep);
    }

    /**
     * Waits before the next poll and returns its remaining budget. No request starts when the wait consumes the entire budget.
     *
     * @return the positive time remaining for the next request
     * @throws E if the deadline has expired
     * @throws InterruptedException if the wait is interrupted
     */
    public Duration awaitNext() throws E, InterruptedException {
        sleeper.sleep(Math.min(delayNanos, remainingNanos()));
        delayNanos = Math.min(delayNanos * 2, TimeUnit.SECONDS.toNanos(3));
        return Duration.ofNanos(remainingNanos());
    }

    /**
     * Rejects a response received after the deadline. Call this after storing the response so cleanup can use any newly discovered resources.
     *
     * @throws E if the deadline has expired
     */
    public void checkDeadline() throws E {
        remainingNanos();
    }

    /**
     * Converts a failed poll into the operation timeout once its budget has expired, preserving the original failure as its cause.
     *
     * @param failure the failed request
     * @return the original failure if the deadline has not expired
     * @throws E if the deadline has expired
     */
    public RuntimeException translateFailure(RuntimeException failure) throws E {
        if (hasExpired()) {
            var timeout = timeoutFailure.get();
            timeout.initCause(failure);
            throw timeout;
        }
        return failure;
    }

    private boolean hasExpired() {
        return nanoTime.getAsLong() - deadlineNanos >= 0;
    }

    private long remainingNanos() throws E {
        long remaining = deadlineNanos - nanoTime.getAsLong();
        if (remaining <= 0)
            throw timeoutFailure.get();
        return remaining;
    }

    /**
     * Interruptible wait strategy used between polls.
     */
    @FunctionalInterface
    public interface Sleeper {

        /**
         * Waits for the requested duration, propagating interruption to the caller.
         *
         * @param nanos the positive duration to wait in nanoseconds
         * @throws InterruptedException if the wait is interrupted
         */
        void sleep(long nanos) throws InterruptedException;
    }
}
