/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

/**
 * Callback interface used to handle streaming in text generation requests.
 * <p>
 * This interface defines a callback-based mechanism to process data as it is streamed from the model. A {@code TextGenerationHandler} guarantees that
 * all method calls are delivered in a <b>sequential and ordered</b> manner.
 */
public interface TextGenerationHandler {

    /**
     * Called whenever a partial response chunk is received. This method may be invoked multiple times during the lifecycle of a single text
     * generation request.
     *
     * @param partialResponse the partial chunk of the response received
     */
    void onPartialResponse(String partialResponse);

    /**
     * Called once the full response has been received and the stream is complete. This marks the end of the response sequence.
     *
     * @param completeResponse the full assembled text generation response
     */
    void onCompleteResponse(TextGenerationResponse completeResponse);

    /**
     * Called if an error occurs during the streaming process. This terminates the stream and no further responses will be delivered.
     *
     * @param error the exception that was thrown
     */
    void onError(Throwable error);

    /**
     * Determines whether the handler should fail immediately upon the first error encountered.
     *
     * @return {@code true} if the handler should fail on first error, {@code false} otherwise
     */
    default boolean failOnFirstError() {
        return false;
    }
}
