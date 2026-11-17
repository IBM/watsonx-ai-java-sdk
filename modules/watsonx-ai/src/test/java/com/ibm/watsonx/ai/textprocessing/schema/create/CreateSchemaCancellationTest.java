/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Mockito.when;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.parallel.Isolated;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;

@DisabledInNativeImage
@Isolated("Resets the shared HTTP client provider while asserting transport cancellation.")
public class CreateSchemaCancellationTest extends AbstractWatsonxTest {

    @Test
    void should_cancel_file_deletion_transport_before_response_headers() {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));

        assertCancelsTransport(() -> CreateSchemaRestClient.builder()
            .baseUrl("https://test.com")
            .cosUrl("https://cos.test.com")
            .version(API_VERSION)
            .timeout(Duration.ofSeconds(5))
            .authenticator(mockAuthenticator)
            .build()
            .deleteFileAsync(DeleteFileRequest.of("my-bucket", "document.pdf")));
    }

    @Test
    void should_not_send_file_deletion_retry_after_cancellation() {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));

        assertCancelsRetry(() -> CreateSchemaRestClient.builder()
            .baseUrl("https://test.com")
            .cosUrl("https://cos.test.com")
            .version(API_VERSION)
            .timeout(Duration.ofSeconds(5))
            .authenticator(mockAuthenticator)
            .build()
            .deleteFileAsync(DeleteFileRequest.of("my-bucket", "document.pdf")));
    }
}
