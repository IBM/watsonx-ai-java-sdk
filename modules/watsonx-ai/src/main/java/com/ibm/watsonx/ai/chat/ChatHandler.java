/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import java.util.ServiceLoader;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;
import com.ibm.watsonx.ai.core.spi.executor.CallbackExecutorProvider;

/**
 * Interface for handling streaming chat responses.
 * <p>
 *
 * <b>Required Method</b>
 * <ul>
 * <li>{@link #onPartialResponse} - called as data arrives</li>
 * </ul>
 *
 * <b>Optional Methods</b>
 * <ul>
 * <li>{@link #onCompleteResponse} - called when streaming completes</li>
 * <li>{@link #onError} - handle errors during streaming</li>
 * <li>{@link #onPartialToolCall} - receive partial tool call fragments</li>
 * <li>{@link #onCompleteToolCall} - receive completed tool calls</li>
 * <li>{@link #onPartialThinking} - receive reasoning/thinking content</li>
 * <li>{@link #failOnFirstError} - control error handling behavior</li>
 * </ul>
 *
 * <b>Callback Order</b>
 * <p>
 *
 * Within a single streaming request, callbacks follow this lifecycle:
 * <ol>
 * <li>{@link #onPartialResponse}, {@link #onPartialThinking}, {@link #onPartialToolCall} - invoked zero or more times as data arrives.
 * {@link #onCompleteToolCall} may occur between partial callbacks for different tools, at most once per tool and after that tool's fragments</li>
 * <li>{@link #onCompleteResponse} - invoked on successful completion, after all earlier callbacks have returned</li>
 * </ol>
 * Errors are reported through {@link #onError}, which is terminal only when {@link #failOnFirstError()} returns {@code true}.
 *
 * <b>Error Handling Behavior</b>
 * <ul>
 * <li>If {@link #failOnFirstError()} returns {@code true}: the first error triggers {@link #onError} and immediately cancels the stream. No further
 * callbacks are invoked.</li>
 * <li>If {@link #failOnFirstError()} returns {@code false} (default): errors are reported via {@link #onError} but streaming continues. Multiple
 * errors may be reported, and {@link #onCompleteResponse} is still called at the end.</li>
 * </ul>
 *
 * <b>Threading Model</b>
 * <p>
 * All callbacks are executed on the callback executor:
 * <ul>
 * <li><b>Java 21+:</b> Virtual threads are used by default, allowing blocking operations without performance impact.</li>
 * <li><b>Java 17-20:</b> A cached thread pool is used by default. Blocking operations are allowed but may consume threads from the pool.</li>
 * </ul>
 * <p>
 * <b>Custom Executor:</b> Developers can customize the threading behavior by implementing the {@link CallbackExecutorProvider} SPI and registering it
 * via {@link ServiceLoader}. This allows full control over thread management, pooling strategies, and execution policies.
 * <p>
 * Within a single streaming request, <b>every</b> callback - including {@link #onCompleteToolCall} - is invoked <b>sequentially</b>, in the order the
 * events were emitted, and is never executed concurrently with another one. Consecutive callbacks may run on different threads of the callback
 * executor, but each one is guaranteed to have returned before the next one starts, and the state it wrote is visible to the next one. A handler can
 * therefore accumulate into unsynchronized fields without any locking of its own.
 * <p>
 * Because delivery is serialized, a callback that blocks delays the callbacks that follow it in the same request. This is intentional: it is what
 * keeps the ordering above observable.
 * <p>
 * <b>Important:</b> If the same {@code ChatHandler} instance is shared across multiple concurrent streaming requests, the implementation must handle
 * synchronization internally. The SDK does not serialize callbacks across different requests.
 */
public interface ChatHandler {

    /**
     * Called whenever a partial chat response chunk is received.
     * <p>
     * This method may be invoked multiple times during the lifecycle of a single chat request.
     *
     * @param partialResponse the partial chunk of the response received
     * @param partialChatResponse the partial chat response
     */
    void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse);

    /**
     * Called once the full chat response has been received and the stream is complete.
     * <p>
     * This marks the successful end of the response sequence. After this method is invoked, no other callbacks will be called for this request.
     * <p>
     * <b>Note:</b> This method is not invoked if {@link #failOnFirstError()} returns {@code true} and an error occurs during streaming. In that case,
     * {@link #onError(Throwable)} is called instead.
     *
     * @param completeResponse the full chat response
     */
    default void onCompleteResponse(ChatResponse completeResponse) {}

    /**
     * Called if an error occurs during the chat streaming process.
     * <p>
     * The behavior after this method is called depends on {@link #failOnFirstError()}:
     * <ul>
     * <li>If {@code true}: the stream is cancelled immediately and no further callbacks are invoked, including {@link #onCompleteResponse}.</li>
     * <li>If {@code false} (default): streaming continues, this method may be called multiple times, and {@link #onCompleteResponse} will still be
     * called at the end.</li>
     * </ul>
     *
     * @param error the exception that was thrown
     */
    default void onError(Throwable error) {}

    /**
     * Called each time the model generates a partial tool call fragment.
     * <p>
     * This method is typically invoked multiple times for a single tool call, each time providing a fragment of the tool's arguments. Calls that
     * finish normally are followed by {@link #onCompleteToolCall(CompletedToolCall)}.
     *
     * @param partialToolCall a partial tool call fragment containing index, tool ID, tool name, and partial arguments
     */
    default void onPartialToolCall(PartialToolCall partialToolCall) {}

    /**
     * Called once the model has finished streaming a single tool call and the arguments are fully assembled.
     * <p>
     * Within each choice, a delta for tool index {@code k} completes pending tools with smaller indexes, a known function name and valid JSON object
     * arguments, directly or encoded as a JSON string. Missing or blank arguments are treated as an empty object. These completion callbacks precede
     * the partial callback for index {@code k}, allowing application code to start a tool while the model generates subsequent tools.
     * <p>
     * This early completion assumes the provider does not return to a completed tool index. A later delta for an already completed index is reported
     * as an {@link IllegalStateException}. Incomplete tools can still receive interleaved fragments and complete when the choice finishes with
     * {@code tool_calls} or {@code stop}. When no finish reason was provided, a trailing usage chunk, {@code [DONE]} or the end of the stream
     * completes pending tools only if every tool in the choice has valid JSON object arguments.
     * <p>
     * Limits, cancellation, errors or content filtering prevent pending tools from completing. Earlier completion callbacks are not revoked if the
     * choice is later interrupted. Accumulated tool data remains in any final response.
     * <p>
     * This method never executes a tool automatically. Application code must invoke or schedule tool execution. All callbacks remain serialized, so
     * blocking this method delays subsequent callbacks in the request. The {@link CompletedToolCall#toolCall()} contains an {@code index} field
     * carrying the position of the tool call in the response.
     *
     * @param completeToolCall the completed tool call
     */
    default void onCompleteToolCall(CompletedToolCall completeToolCall) {}

    /**
     * Called whenever a partial segment of the assistant's reasoning process is received during streaming.
     *
     * @param partialThinking the raw partial text of the reasoning content
     * @param partialChatResponse the structured partial chat response
     */
    default void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {}

    /**
     * Determines whether the handler should fail immediately upon the first error encountered.
     * <p>
     * When {@code true}, the first error will trigger {@link #onError(Throwable)} and immediately cancel the stream. No further callbacks will be
     * invoked, including {@link #onCompleteResponse}.
     * <p>
     * When {@code false} (default), errors are reported via {@link #onError(Throwable)} but streaming continues. Multiple errors may be reported, and
     * {@link #onCompleteResponse} will still be called at the end.
     *
     * @return {@code true} if the handler should fail on first error, {@code false} otherwise
     */
    default boolean failOnFirstError() {
        return false;
    }
}
