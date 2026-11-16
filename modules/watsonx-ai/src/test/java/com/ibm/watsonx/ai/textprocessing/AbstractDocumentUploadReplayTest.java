/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ibm.watsonx.ai.AbstractWatsonxTest;

public abstract class AbstractDocumentUploadReplayTest extends AbstractWatsonxTest {

    private static final String CONTENT = "complete document content";
    private static final String UPLOAD_PATH = "/bucket/folder/document.pdf";

    @TempDir
    Path inputDirectory;

    @BeforeEach
    protected void authenticate() {
        when(mockAuthenticator.token()).thenReturn("token");
    }

    @Test
    protected void should_replay_legacy_upload_after_transient_failure() throws Exception {
        stubRetry(503, "text/plain", "Unavailable");
        var inputFile = Files.writeString(inputDirectory.resolve("document.pdf"), CONTENT);

        try (var input = Files.newInputStream(inputFile)) {
            assertFalse(input.markSupported());
            assertTrue(upload(UploadRequest.of("request", "bucket", input, "folder/document.pdf")));
        }

        wireMock.verify(2, putRequestedFor(urlEqualTo(UPLOAD_PATH)).withRequestBody(equalTo(CONTENT))
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("request")));
    }

    @Test
    protected void should_replay_legacy_upload_after_token_expiration() throws Exception {
        when(mockAuthenticator.token()).thenReturn("expired", "fresh");
        stubRetry(401, "application/json", """
            {"errors":[{"code":"authentication_token_expired","message":"Token expired"}]}
            """);
        var inputFile = Files.writeString(inputDirectory.resolve("document.pdf"), CONTENT);

        try (var input = Files.newInputStream(inputFile)) {
            assertTrue(upload(UploadRequest.of("request", "bucket", input, "folder/document.pdf")));
        }

        wireMock.verify(2, putRequestedFor(urlEqualTo(UPLOAD_PATH)).withRequestBody(equalTo(CONTENT)));
        wireMock.verify(1, putRequestedFor(urlEqualTo(UPLOAD_PATH)).withHeader("Authorization", equalTo("Bearer fresh")));
    }

    @Test
    protected void should_keep_uploads_scoped_to_each_bucket() {
        wireMock.stubFor(put("/first/document.pdf").willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(put("/second/document.pdf").willReturn(aResponse().withStatus(200)));

        assertTrue(upload(request("first", "first content")));
        assertTrue(upload(request("second", "second content")));
        assertTrue(upload(request("first", "third content")));

        wireMock.verify(1, putRequestedFor(urlEqualTo("/first/document.pdf")).withRequestBody(equalTo("first content")));
        wireMock.verify(1, putRequestedFor(urlEqualTo("/second/document.pdf")).withRequestBody(equalTo("second content")));
        wireMock.verify(1, putRequestedFor(urlEqualTo("/first/document.pdf")).withRequestBody(equalTo("third content")));
    }

    private UploadRequest request(String bucket, String content) {
        return UploadRequest.of("request", bucket, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "document.pdf");
    }

    protected abstract boolean upload(UploadRequest request);

    private void stubRetry(int status, String contentType, String body) {
        wireMock.stubFor(put(UPLOAD_PATH).inScenario("upload-retry").whenScenarioStateIs(Scenario.STARTED).willSetStateTo("retry")
            .willReturn(aResponse().withStatus(status).withHeader("Content-Type", contentType).withBody(body)));
        wireMock.stubFor(put(UPLOAD_PATH).inScenario("upload-retry").whenScenarioStateIs("retry")
            .willReturn(aResponse().withStatus(200)));
    }
}
