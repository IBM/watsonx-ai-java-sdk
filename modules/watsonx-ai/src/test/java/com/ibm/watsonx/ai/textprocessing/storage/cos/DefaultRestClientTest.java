/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage.cos;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;

@DisabledInNativeImage
@Isolated("Resets the shared HTTP client provider while asserting transport cancellation.")
class DefaultRestClientTest extends AbstractWatsonxTest {

    private static final String CONTENT = "document content";
    private DefaultRestClient client;

    @TempDir
    Path uploadDirectory;

    @BeforeEach
    void createClient() {
        when(mockAuthenticator.token()).thenReturn("token");
        withWatsonxServiceMock(() -> client = new DefaultRestClient(DefaultRestClient.builder()
            .cosUrl("https://cos.example.com")
            .bucket("bucket")
            .timeout(Duration.ofSeconds(10))
            .authenticator(mockAuthenticator)
            .httpClient(mockSecureHttpClient), uploadDirectory));
    }

    @Test
    void should_cancel_file_deletion_transport_before_response_headers() {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));

        assertCancelsTransport(() -> client.deleteFileAsync(DeleteFileRequest.of("bucket", "document.pdf")));
    }

    @Test
    void should_not_send_file_deletion_retry_after_cancellation() {
        when(mockAuthenticator.tokenAsync()).thenReturn(completedFuture("token"));

        assertCancelsRetry(() -> CosStorageRestClient.builder()
            .cosUrl("https://cos.example.com")
            .bucket("bucket")
            .timeout(Duration.ofSeconds(10))
            .authenticator(mockAuthenticator)
            .httpClient(mockSecureHttpClient)
            .build()
            .deleteFileAsync(DeleteFileRequest.of("bucket", "document.pdf")));
    }

    @Test
    void should_close_input_before_upload_and_remove_temporary_file_after_success() throws Exception {
        var input = spy(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)));
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockSecureHttpClient.send(any(), any())).thenAnswer(invocation -> {
            verify(input).close();
            assertEquals(CONTENT, Files.readString(stagedFile()));
            return mockHttpResponse;
        });

        assertTrue(client.upload("request", input, "document.pdf"));

        assertNoTemporaryFiles();
    }

    @Test
    void should_close_input_and_remove_temporary_file_when_copy_fails() throws Exception {
        var failure = new IOException("Cannot read input");
        var input = spy(new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }
        });

        var error = assertThrows(RuntimeException.class, () -> client.upload("request", input, "document.pdf"));

        assertSame(failure, error.getCause());
        verify(input).close();
        verifyNoInteractions(mockSecureHttpClient);
        assertNoTemporaryFiles();
    }

    @Test
    void should_remove_temporary_file_when_closing_input_fails() throws Exception {
        var failure = new IOException("Cannot close input");
        var input = spy(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                throw failure;
            }
        });

        var error = assertThrows(RuntimeException.class, () -> client.upload("request", input, "document.pdf"));

        assertSame(failure, error.getCause());
        verify(input).close();
        verifyNoInteractions(mockSecureHttpClient);
        assertNoTemporaryFiles();
    }

    @Test
    void should_close_input_and_remove_temporary_file_when_upload_fails() throws Exception {
        var input = spy(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)));
        var failure = new IOException("Cannot upload document");
        when(mockSecureHttpClient.send(any(), any())).thenAnswer(invocation -> {
            assertEquals(CONTENT, Files.readString(stagedFile()));
            throw failure;
        });

        var error = assertThrows(RuntimeException.class, () -> client.upload("request", input, "document.pdf"));

        assertSame(failure, error.getCause());
        verify(input).close();
        assertNoTemporaryFiles();
    }

    @Test
    void should_preserve_success_when_removing_temporary_file_fails() throws Exception {
        var input = spy(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)));
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockSecureHttpClient.send(any(), any())).thenAnswer(invocation -> {
            var file = stagedFile();
            Files.delete(file);
            Files.createDirectory(file);
            Files.writeString(file.resolve("child"), CONTENT);
            return mockHttpResponse;
        });

        assertTrue(client.upload("request", input, "document.pdf"));

        verify(input).close();
    }

    @Test
    void should_preserve_upload_error_when_removing_temporary_file_also_fails() throws Exception {
        var input = spy(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)));
        var failure = new IOException("Cannot upload document");
        when(mockSecureHttpClient.send(any(), any())).thenAnswer(invocation -> {
            var file = stagedFile();
            Files.delete(file);
            Files.createDirectory(file);
            Files.writeString(file.resolve("child"), CONTENT);
            throw failure;
        });

        var error = assertThrows(RuntimeException.class, () -> client.upload("request", input, "document.pdf"));

        assertSame(failure, error.getCause());
        assertEquals(1, error.getSuppressed().length);
        assertInstanceOf(DirectoryNotEmptyException.class, error.getSuppressed()[0]);
        verify(input).close();
    }

    private Path stagedFile() throws IOException {
        try (var files = Files.list(uploadDirectory)) {
            var paths = files.toList();
            assertEquals(1, paths.size());
            return paths.get(0);
        }
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(uploadDirectory)) {
            assertEquals(List.of(), files.toList());
        }
    }
}
