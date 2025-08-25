/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.util.StreamingToolFetcher.PartialToolCall;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Callback interface used to handle streaming chat responses.
 * <p>
 * <b>Thread Safety Considerations:</b>
 * <p>
 * Implementations of this interface must be thread-safe if the same instance is shared across multiple streaming requests. The subscriber
 * implementation guarantees serialized invocation within a single stream, but if the same handler instance processes multiple concurrent streams, its
 * methods may be called from different threads simultaneously.
 * <p>
 * This is particularly important when the I/O executor is configured with multiple threads (which is the default configuration using
 * {@link ExecutorProvider#ioExecutor()}).
 */
public interface ChatHandler {

    /**
     * Called whenever a partial chat response chunk is received. This method may be invoked multiple times during the lifecycle of a single chat
     * request.
     * <p>
     * <b>Thread Safety:</b>
     * <p>
     * This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple streaming requests.
     *
     * @param partialResponse the partial chunk of the response received
     * @param partialChatResponse the partial chat response
     */
    void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse);

    /**
     * Called once the full chat response has been received and the stream is complete. This marks the end of the response sequence.
     * <p>
     * <b>Thread Safety:</b>
     * <p>
     * This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple streaming requests.
     *
     * @param completeResponse the full chat response
     */
    void onCompleteResponse(ChatResponse completeResponse);

    /**
     * Called if an error occurs during the chat streaming process. This terminates the stream and no further responses will be delivered.
     * <p>
     * <b>Thread Safety:</b>
     * <p>
     * This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple streaming requests.
     *
     * @param error the exception that was thrown
     */
    void onError(Throwable error);

    /**
     * Called whenever a partial tool call is detected during the chat streaming process. This method may be invoked multiple times if the model
     * streams tool call arguments or metadata in chunks.
     * <p>
     * <b>Thread Safety:</b>
     * <p>
     * This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple streaming requests.
     *
     * @param partialToolCall the partial chunk of the tool call received
     */
    default void onPartialToolCall(PartialToolCall partialToolCall) {
        // Invoked whenever a partial tool call is detected during the streaming process
    }

    /**
     * Called once a tool call has been fully received.
     * <p>
     * <b>Thread Safety:</b>
     * <p>
     * This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple streaming requests.
     *
     * @param completeToolCall the fully constructed tool call
     */
    default void onCompleteToolCall(ToolCall completeToolCall) {
        // Allows triggering actions as soon as the complete tool call is available, without necessarily waiting for the full assistant response to
        // finish streaming.
    }

    /**
     * Called whenever a partial segment of the assistant's reasoning process is received during streaming.
     * <p>
     * This method may be invoked multiple times if the reasoning content is streamed in multiple chunks.
     * <p>
     * <b>Note:</b> For this handler to work, {@link ExtractionTags} must be configured in the {@code ChatService} so that reasoning tags can be
     * detected in the stream.
     * <p>
     * <b>Thread Safety:</b> This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple
     * streaming requests.
     * <p>
     *
     * @param partialThinking the raw partial text of the reasoning content
     * @param partialChatResponse the structured partial chat response
     * @see ExtractionTags
     */
    default void onPartialThinking(String partialThinking, PartialChatResponse partialChatResponse) {}

    /**
     * Determines whether the handler should fail immediately upon the first error encountered.
     *
     * @return {@code true} if the handler should fail on first error, {@code false} otherwise
     */
    default boolean failOnFirstError() {
        return false;
    }
}