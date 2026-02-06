/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.ibm.watsonx.ai.core.Language;
import com.ibm.watsonx.ai.transcription.TranscriptionRequest;
import com.ibm.watsonx.ai.transcription.TranscriptionService;

@ExtendWith(MockitoExtension.class)
public class TranscriptionServiceTest extends AbstractWatsonxTest {

    @BeforeEach
    void setup() {
        when(mockAuthenticator.token()).thenReturn("token");
    }

    @Test
    void should_transcribe_audio_successfully() throws Exception {

        var file = Files.createTempFile("", "");
        file.toFile().deleteOnExit();
        Files.writeString(file, "the ending was terrific.");

        wireMock.stubFor(post("/ml/v1/audio/transcriptions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", containing("multipart/form-data; boundary=----watsonx-ai-sdk"))
            .withRequestBody(containing("name=\"model\""))
            .withRequestBody(containing("openai/whisper-tiny"))
            .withRequestBody(containing("name=\"language\""))
            .withRequestBody(containing("it"))
            .withRequestBody(containing("name=\"project_id\""))
            .withRequestBody(containing("pid"))
            .withRequestBody(containing("the ending was terrific."))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                          "model": "openai/whisper-tiny",
                          "text": "the ending was terrific.",
                          "created_at": "2023-07-21T16:52:32.190Z",
                          "token_count": 8
                    }""")));

        TranscriptionService service = TranscriptionService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl("http://localhost:".concat(String.valueOf(wireMock.getPort())))
            .projectId("pid")
            .modelId("openai/whisper-tiny")
            .build();

        var result = service.transcribe(file.toAbsolutePath().toString(), Language.ITALIAN);
        assertEquals("openai/whisper-tiny", result.model());
        assertEquals("the ending was terrific.", result.text());
        assertEquals(8, result.tokenCount());
        assertEquals("2023-07-21T16:52:32.190Z", result.createdAt());
    }

    @Test
    void should_transcribe_audio_successfully_overriding_parameters() throws Exception {

        var file = Files.createTempFile("", "");
        file.toFile().deleteOnExit();
        Files.writeString(file, "the ending was terrific.");

        wireMock.stubFor(post("/ml/v1/audio/transcriptions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", containing("multipart/form-data; boundary=----watsonx-ai-sdk"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .withRequestBody(containing("name=\"model\""))
            .withRequestBody(containing("my-openai/whisper-tiny"))
            .withRequestBody(containing("name=\"language\""))
            .withRequestBody(containing("en"))
            .withRequestBody(containing("name=\"project_id\""))
            .withRequestBody(containing("mid"))
            .withRequestBody(containing("name=\"space_id\""))
            .withRequestBody(containing("sid"))
            .withRequestBody(containing("the ending was terrific."))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                          "model": "openai/whisper-tiny",
                          "text": "the ending was terrific.",
                          "created_at": "2023-07-21T16:52:32.190Z",
                          "token_count": 8
                    }""")));

        TranscriptionService service = TranscriptionService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl("http://localhost:".concat(String.valueOf(wireMock.getPort())))
            .projectId("pid")
            .modelId("openai/whisper-tiny")
            .build();

        var result = service.transcribe(
            TranscriptionRequest.builder()
                .projectId("mid")
                .spaceId("sid")
                .modelId("my-openai/whisper-tiny")
                .file(file.toAbsolutePath().toString())
                .transactionId("transaction-id")
                .build());

        assertEquals("openai/whisper-tiny", result.model());
        assertEquals("the ending was terrific.", result.text());
        assertEquals(8, result.tokenCount());
        assertEquals("2023-07-21T16:52:32.190Z", result.createdAt());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_file_not_found_exception() {
        var ex = assertThrowsExactly(RuntimeException.class, () -> TranscriptionRequest.builder().file("/fileNotFound/file.txt").build());
        assertEquals(FileNotFoundException.class, ex.getCause().getClass());

        TranscriptionService service = TranscriptionService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl("http://localhost:".concat(String.valueOf(wireMock.getPort())))
            .projectId("pid")
            .modelId("openai/whisper-tiny")
            .build();

        ex = assertThrowsExactly(RuntimeException.class, () -> service.transcribe(new File("/fileNotFound/file.txt"), Language.ENGLISH));
        assertEquals(FileNotFoundException.class, ex.getCause().getClass());
    }

    @Test
    void should_throw_io_exception() throws Exception {

        when(mockSecureHttpClient.send(any(), any())).thenThrow(IOException.class);

        withWatsonxServiceMock(() -> {

            TranscriptionService service = TranscriptionService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl("http://localhost:".concat(String.valueOf(wireMock.getPort())))
                .projectId("pid")
                .modelId("openai/whisper-tiny")
                .build();

            var ex = assertThrows(RuntimeException.class, () -> service.transcribe(new ByteArrayInputStream("Hello".getBytes()), Language.ITALIAN));
            assertEquals(IOException.class, ex.getCause().getClass());
        });
    }
}
