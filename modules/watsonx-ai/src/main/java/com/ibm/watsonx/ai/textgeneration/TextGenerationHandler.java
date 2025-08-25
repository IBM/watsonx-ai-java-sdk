/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

/**
 * Callback interface used to handle streaming in text generation requests.
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
public interface TextGenerationHandler {

    /**
     * Called whenever a partial response chunk is received. This method may be invoked multiple times during the lifecycle of a single text
     * generation request.
     * <p>
     * <b>Thread Safety:</b>
     * <p>
     * This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple streaming requests.
     *
     * @param partialResponse the partial chunk of the response received
     */
    void onPartialResponse(String partialResponse);

    /**
     * Called once the full response has been received and the stream is complete. This marks the end of the response sequence.
     * <p>
     * <b>Thread Safety:</b>
     * <p>
     * This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple streaming requests.
     *
     * @param completeResponse the full assembled chat response
     */
    void onCompleteResponse(TextGenerationResponse completeResponse);

    /**
     * Called if an error occurs during the streaming process. This terminates the stream and no further responses will be delivered.
     * <p>
     * <b>Thread Safety:</b>
     * <p>
     * This method may be called concurrently from different threads if the {@code handler} instance is shared across multiple streaming requests.
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
