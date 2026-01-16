/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static java.util.Objects.nonNull;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatHandler;

/**
 * Utility methods for streaming operations.
 */
public class StreamingUtils {

    /**
     * Handles an error by unwrapping the root cause and notifying the {@link ChatHandler}.
     * <p>
     * This method extracts the underlying cause from the throwable (if present) and invokes the handler's {@code onError} callback. It is designed to
     * be used in {@link CompletableFuture} exception handlers, particularly with {@code exceptionally} or {@code completeExceptionally}.
     *
     * @param t the {@link Throwable} to handle (typically a {@link java.util.concurrent.CompletionException})
     * @param handler the {@link ChatHandler} that should be notified of the error
     * @return the unwrapped {@link Throwable} (the cause if present, otherwise the original throwable)
     */
    public static Throwable handleError(Throwable t, ChatHandler handler) {
        t = nonNull(t.getCause()) ? t.getCause() : t;
        handler.onError(t);
        return t;
    }
}
