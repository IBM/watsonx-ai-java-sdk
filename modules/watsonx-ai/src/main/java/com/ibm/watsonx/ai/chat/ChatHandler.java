/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat;

import com.ibm.watsonx.ai.chat.model.PartialChatResponse;
import com.ibm.watsonx.ai.chat.model.StreamingToolFetcher.PartialToolCall;
import com.ibm.watsonx.ai.chat.model.ToolCall;

/**
 * Callback interface used to handle streaming chat responses.
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
}
