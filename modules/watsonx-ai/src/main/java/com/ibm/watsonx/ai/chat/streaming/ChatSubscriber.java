/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.streaming;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.ai.chat.ChatHandler;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.Tool;

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

    /**
     * Builds a map indicating whether each tool has parameters.
     *
     * @param tools the list of available {@link Tool}s
     * @return a map where keys are tool names and values indicate if the tool has parameters
     */
    static Map<String, Boolean> toolHasParameters(List<Tool> tools) {
        if (isNull(tools) || tools.size() == 0)
            return Map.of();

        return tools.stream().collect(toMap(
            tool -> tool.function().name(),
            Tool::hasParameters
        ));
    }

    /**
     * Handles an error by invoking the {@link ChatHandler}'s {@code onError} callback if the given throwable is non-null.
     *
     * @param t the {@link Throwable} to handle
     * @param handler the {@link ChatHandler} that should be notified of the error
     * @return always {@code null}, enabling direct use in async exception handlers
     */
    static Throwable handleError(Throwable t, ChatHandler handler) {
        t = nonNull(t.getCause()) ? t.getCause() : t;
        handler.onError(t);
        return t;
    }
}
