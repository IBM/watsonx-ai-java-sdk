/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Internal future composition for operations that own their source future. Shared source operations must not use cancellation propagation.
 * <p>
 * This class is not part of the supported public SDK API.
 */
public final class CancellableFutures {

    private CancellableFutures() {}

    /**
     * Maps the source future and propagates caller cancellation to it.
     *
     * @param source the owned source future
     * @param mapper the result mapping
     * @param <T> the source result type
     * @param <R> the mapped result type
     * @return the mapped future
     */
    public static <T, R> CompletableFuture<R> thenApply(CompletableFuture<T> source, Function<? super T, ? extends R> mapper) {
        return propagateCancellation(source, source.thenApply(mapper));
    }

    /**
     * Maps the source future on the supplied executor and propagates caller cancellation to the source.
     *
     * @param source the owned source future
     * @param mapper the result mapping
     * @param executor the executor used for the mapping
     * @param <T> the source result type
     * @param <R> the mapped result type
     * @return the mapped future
     */
    public static <T, R> CompletableFuture<R> thenApplyAsync(CompletableFuture<T> source, Function<? super T, ? extends R> mapper,
        Executor executor) {
        return propagateCancellation(source, source.thenApplyAsync(mapper, executor));
    }

    private static <T> CompletableFuture<T> propagateCancellation(CompletableFuture<?> source, CompletableFuture<T> response) {
        response.whenComplete((result, error) -> {
            if (response.isCancelled())
                source.cancel(true);
        });
        return response;
    }
}
