/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatResponse;

/**
 * Subscriber abstraction for consuming raw Server-Sent Events.
 */
public interface ChatSubscriber {

    /**
     * Called when a new SSE message chunk is received from the server.
     *
     * @param partialMessage the raw SSE event payload (e.g., "data: {...}")
     */
    void onNext(String partialMessage);

    /**
     * Called if an error occurs during streaming.
     *
     * @param throwable the error that occurred
     */
    CompletableFuture<Void> onError(Throwable throwable);

    /**
     * Called once the streaming session has completed successfully.
     *
     * @return a CompletableFuture that completes with the final {@link ChatResponse}
     */
    CompletableFuture<ChatResponse> onComplete();
}
