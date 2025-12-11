/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import java.util.concurrent.ExecutorService;
import com.ibm.watsonx.ai.chat.interceptor.ToolInterceptor;
import com.ibm.watsonx.ai.chat.model.CompletedToolCall;
import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.PartialToolCall;

/**
 * Interface for handling streaming chat responses.
 * <p>
 * This interface defines a callback-based mechanism to process data as it is streamed from the model.
 * <p>
 * Within a single streaming request, callbacks are invoked in the following order:
 * <ol>
 * <li>{@link #onPartialResponse}, {@link #onPartialThinking}, {@link #onPartialToolCall} - invoked zero or more times as data arrives</li>
 * <li>{@link #onCompleteToolCall} - invoked zero or more times (once per completed tool call)</li>
 * <li>Terminal callback:
 * <ul>
 * <li>{@link #onCompleteResponse} - invoked on successful completion</li>
 * <li>{@link #onError} - invoked when error occurs</li>
 * </ul>
 * </li>
 * </ol>
 * <p>
 * <b>Error handling behavior:</b>
 * <ul>
 * <li>If {@link #failOnFirstError()} returns {@code true}: the first error triggers {@link #onError} and immediately cancels the stream. No further
 * callbacks are invoked.</li>
 * <li>If {@link #failOnFirstError()} returns {@code false} (default): errors are reported via {@link #onError} but streaming continues. Multiple
 * errors may be reported, and {@link #onCompleteResponse} is still called at the end.</li>
 * </ul>
 *
 * <b>Thread Safety</b>
 * <p>
 * Within a single streaming request, all callbacks are guaranteed to be invoked <b>sequentially</b> and <b>never concurrently</b>, following the
 * Reactive Streams specification.
 * <p>
 * <b>Important:</b> If the same {@code ChatHandler} instance is shared across multiple concurrent streaming requests, the implementation must handle
 * synchronization internally. The SDK does not serialize callbacks across different requests.
 */
public interface ChatHandler {

    /**
     * Called whenever a partial chat response chunk is received.
     * <p>
     * This method may be invoked multiple times during the lifecycle of a single chat request.
     * <p>
     * <b>Threading:</b> This method is executed on the streaming thread. If it performs blocking operations (e.g., I/O, heavy computation), delegate
     * those tasks to a separate thread using an {@link ExecutorService} to avoid blocking the stream.
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
     * <p>
     * <b>Threading:</b> This method is executed on the streaming thread. If it performs blocking operations (e.g., I/O, heavy computation), delegate
     * those tasks to a separate thread using an {@link ExecutorService} to avoid blocking the stream.
     *
     * @param completeResponse the full chat response
     */
    void onCompleteResponse(ChatResponse completeResponse);

    /**
     * Called if an error occurs during the chat streaming process.
     * <p>
     * The behavior after this method is called depends on {@link #failOnFirstError()}:
     * <ul>
     * <li>If {@code true}: the stream is cancelled immediately and no further callbacks are invoked, including {@link #onCompleteResponse}.</li>
     * <li>If {@code false} (default): streaming continues, this method may be called multiple times, and {@link #onCompleteResponse} will still be
     * called at the end.</li>
     * </ul>
     * <p>
     * <b>Threading:</b> This method is executed on the streaming thread. If it performs blocking operations (e.g., I/O, heavy computation), delegate
     * those tasks to a separate thread using an {@link ExecutorService} to avoid blocking the stream.
     *
     * @param error the exception that was thrown
     */
    void onError(Throwable error);

    /**
     * Called each time the model generates a partial tool call fragment.
     * <p>
     * This method is typically invoked multiple times for a single tool call, each time providing a fragment of the tool's arguments, until
     * {@link #onCompleteToolCall(CompletedToolCall)} is invoked indicating that the tool call is fully assembled.
     * <p>
     * <b>Threading:</b> This method is executed on the streaming thread. If it performs blocking operations (e.g., I/O, heavy computation), delegate
     * those tasks to a separate thread using an {@link ExecutorService} to avoid blocking the stream.
     *
     * @param partialToolCall a partial tool call fragment containing index, tool ID, tool name, and partial arguments
     */
    default void onPartialToolCall(PartialToolCall partialToolCall) {}

    /**
     * Called once the model has finished streaming a single tool call and the arguments are fully assembled.
     * <p>
     * When multiple tools are called in the same response, this method is invoked once per tool. The {@link CompletedToolCall#toolCall()} contains an
     * {@code index} field that can be used to determine the original order of tool calls if needed.
     * <p>
     * <b>Note:</b> When a {@link ToolInterceptor} is configured, tool calls are processed asynchronously. In this case, the order of
     * {@code onCompleteToolCall} invocations may not match the original order of tool calls. Use the {@code index} field to reorder if necessary.
     * <p>
     * <b>Threading:</b> This method is executed on the streaming thread, or on an executor thread if a {@link ToolInterceptor} is configured. If it
     * performs blocking operations (e.g., I/O, heavy computation), delegate those tasks to a separate thread using an {@link ExecutorService}.
     *
     * @param completeToolCall the completed tool call
     */
    default void onCompleteToolCall(CompletedToolCall completeToolCall) {}

    /**
     * Called whenever a partial segment of the assistant's reasoning process is received during streaming.
     * <p>
     * <b>Threading:</b> This method is executed on the streaming thread. If it performs blocking operations (e.g., I/O, heavy computation), delegate
     * those tasks to a separate thread using an {@link ExecutorService} to avoid blocking the stream.
     *
     * @param partialThinking the raw partial text of the reasoning content
     * @param partialChatResponse the structured partial chat response
     * @see ExtractionTags
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