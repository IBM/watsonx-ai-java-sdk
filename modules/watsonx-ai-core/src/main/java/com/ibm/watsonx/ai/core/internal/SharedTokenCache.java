/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.internal;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Coordinates token caching and acquisition without sharing ownership of the acquisition future with callers.
 * <p>
 * This class is not part of the supported public SDK API.
 *
 * @param <T> the cached token response type
 */
public final class SharedTokenCache<T> {
    private final Predicate<T> expired;
    private final Supplier<CompletableFuture<T>> acquire;
    private final AtomicReference<State<T>> state = new AtomicReference<>(new State<>(null, null));

    /**
     * Creates a cache whose synchronous and asynchronous callers share the same asynchronous acquisition.
     *
     * @param expired determines whether the cached token is expired, including an empty cache represented by {@code null}
     * @param acquire starts an asynchronous token acquisition
     */
    public SharedTokenCache(Predicate<T> expired, Supplier<CompletableFuture<T>> acquire) {
        this.expired = requireNonNull(expired);
        this.acquire = requireNonNull(acquire);
    }

    /**
     * Waits for a token without transferring interruption or cancellation to the shared acquisition.
     *
     * @return the cached or acquired token
     */
    public T token() {
        var waiting = tokenAsync();
        try {
            return waiting.get();
        } catch (InterruptedException e) {
            waiting.cancel(false);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            var cause = unwrap(e);
            if (cause instanceof RuntimeException runtimeException)
                throw runtimeException;
            if (cause instanceof Error error)
                throw error;
            throw new RuntimeException(cause);
        }
    }

    /**
     * Returns a caller-owned future without propagating cancellation or completion to the shared acquisition.
     *
     * @return the caller's token future
     */
    public CompletableFuture<T> tokenAsync() {
        while (true) {

            var current = state.get();

            if (nonNull(current.acquisition()))
                return current.acquisition().copy();

            if (!expired.test(current.token()))
                return completedFuture(current.token());

            var acquisition = new State<>(current.token(), new CompletableFuture<T>());

            if (!state.compareAndSet(current, acquisition))
                continue;

            try {
                requireNonNull(acquire.get(), "Token acquisition future must not be null")
                    .whenComplete((token, error) -> complete(acquisition, token, error));

            } catch (Throwable error) {
                complete(acquisition, null, error);
            }

            return acquisition.acquisition().copy();
        }
    }

    private void complete(State<T> acquisition, T token, Throwable error) {

        if (error == null && token == null)
            error = new NullPointerException("Token response must not be null");

        state.compareAndSet(acquisition, new State<>(error == null ? token : acquisition.token(), null));

        if (error == null)
            acquisition.acquisition().complete(token);
        else
            acquisition.acquisition().completeExceptionally(unwrap(error));
    }

    private static Throwable unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException) && error.getCause() != null)
            error = error.getCause();
        return error;
    }

    private record State<T>(T token, CompletableFuture<T> acquisition) {}
}
