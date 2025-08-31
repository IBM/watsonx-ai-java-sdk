/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import com.ibm.watsonx.ai.chat.model.ExtractionTags;
import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.ToolCall;
import com.ibm.watsonx.ai.chat.util.StreamingToolFetcher.PartialToolCall;

/**
 * Interface for handling streaming chat responses.
 * <p>
 * This interface defines a callback-based mechanism to process data as it is streamed from the model. A {@code ChatHandler} guarantees that all
 * method calls are delivered in a <b>sequential and ordered</b> manner.
 */
public interface ChatHandler {

    /**
     * Called whenever a partial chat response chunk is received. This method may be invoked multiple times during the lifecycle of a single chat
     * request.
     *
     * @param partialResponse the partial chunk of the response received
     * @param partialChatResponse the partial chat response
     */
    void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse);

    /**
     * Called once the full chat response has been received and the stream is complete. This marks the end of the response sequence.
     *
     * @param completeResponse the full chat response
     */
    void onCompleteResponse(ChatResponse completeResponse);

    /**
     * Called if an error occurs during the chat streaming process. This terminates the stream and no further responses will be delivered.
     *
     * @param error the exception that was thrown
     */
    void onError(Throwable error);

    /**
     * Called whenever a partial tool call is detected during the chat streaming process. This method may be invoked multiple times if the model
     * streams tool call arguments or metadata in chunks.
     *
     * @param partialToolCall the partial chunk of the tool call received
     */
    default void onPartialToolCall(PartialToolCall partialToolCall) {
        // Invoked whenever a partial tool call is detected during the streaming process
    }

    /**
     * Called once a tool call has been fully received.
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