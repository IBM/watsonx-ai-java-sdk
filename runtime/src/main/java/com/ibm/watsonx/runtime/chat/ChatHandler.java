package com.ibm.watsonx.runtime.chat;

import com.ibm.watsonx.runtime.chat.model.PartialChatResponse;

/**
 * Callback interface used to handle streaming chat responses.
 */
public interface ChatHandler {

    /**
     * Called whenever a partial chat response chunk is received.
     * This method may be invoked multiple times during the lifecycle
     * of a single chat request.
     *
     * @param partialResponse the partial chunk of the response received
     * @param partialChatResponse the partial chat response
     */
    void onPartialResponse(String partialResponse, PartialChatResponse partialChatResponse);

    /**
     * Called once the full chat response has been received and the stream is complete.
     * This marks the end of the response sequence.
     *
     * @param completeResponse the full assembled chat response
     */
    void onCompleteResponse(ChatResponse completeResponse);

    /**
     * Called if an error occurs during the chat streaming process.
     * This terminates the stream and no further responses will be delivered.
     *
     * @param error the exception that was thrown
     */
    void onError(Throwable error);
}
