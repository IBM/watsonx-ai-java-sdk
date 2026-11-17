/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.embedding;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Mockito.when;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.parallel.Isolated;
import com.ibm.watsonx.ai.AbstractWatsonxTest;

@DisabledInNativeImage
@Isolated("Resets the shared HTTP client provider while asserting transport cancellation.")
public class EmbeddingAsyncCancellationTest extends AbstractWatsonxTest {

    @Test
    void should_cancel_embedding_transport_before_response_headers() {
        assertCancelsTransport(this::embedAsync);
    }

    @Test
    void should_not_send_embedding_retry_after_cancellation() {
        assertCancelsRetry(this::embedAsync);
    }

    private CompletableFuture<EmbeddingResponse> embedAsync() {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));
        var client = EmbeddingRestClient.builder()
            .baseUrl("https://test.com")
            .version(API_VERSION)
            .timeout(Duration.ofSeconds(5))
            .authenticator(mockAuthenticator)
            .build();
        return client.embeddingAsync(null, new EmbeddingPayload("model-id", null, "project-id", List.of("Hello"), null, null));
    }
}
