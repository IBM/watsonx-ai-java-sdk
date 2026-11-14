/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage.cos;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.slf4j.event.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.storage.StorageFactory;
import com.ibm.watsonx.ai.textprocessing.storage.StorageOperations;

@DisabledInNativeImage
public class CosStorageServiceTest extends AbstractWatsonxTest {

    private static final String BUCKET = "my-bucket";
    private static final String FILE_NAME = "test.pdf";
    private static final String FILE_PATH = "/" + BUCKET + "/" + FILE_NAME;
    private static final String FILE_CONTENT = "hello world";

    private static final String COS_NOT_FOUND_XML = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Error>
            <Code>NoSuchKey</Code>
            <Message>The specified key does not exist.</Message>
            <Resource>/%s/%s</Resource>
            <RequestId>req-id</RequestId>
            <httpStatusCode>404</httpStatusCode>
        </Error>""".formatted(BUCKET, FILE_NAME);

    private static final String COS_GENERIC_404_XML = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Error>
            <Code>NoSuchBucket</Code>
            <Message>The specified bucket does not exist.</Message>
            <Resource>/%s/%s</Resource>
            <RequestId>req-id</RequestId>
            <httpStatusCode>404</httpStatusCode>
        </Error>""".formatted(BUCKET, FILE_NAME);

    @RegisterExtension
    WireMockExtension cosServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
        .build();

    StorageOperations storage;

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUpStorage() {
        when(mockAuthenticator.scheme()).thenReturn("Bearer");
        when(mockAuthenticator.tokenAsync()).thenAnswer(invocation -> completedFuture(mockAuthenticator.token()));
        resetHttpClient();
        storage = buildCosService();
    }

    @Test
    void should_upload_inputstream_successfully() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(put(FILE_PATH).willReturn(aResponse().withStatus(200)));
        boolean result = storage.upload(stream(FILE_CONTENT), FILE_NAME);
        assertTrue(result);
        cosServer.verify(1, putRequestedFor(urlEqualTo(FILE_PATH)));
    }

    @Test
    void should_upload_inputstream_with_tracking_id() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(put(FILE_PATH)
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("req-upload"))
            .willReturn(aResponse().withStatus(200)));
        boolean result = storage.upload("req-upload", stream(FILE_CONTENT), FILE_NAME);
        assertTrue(result);
        cosServer.verify(1, putRequestedFor(urlEqualTo(FILE_PATH))
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("req-upload")));
    }

    @Test
    void should_replay_inputstream_after_transient_upload_failure() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        stubUploadRetry(503, "text/plain", "Service unavailable");
        var file = Files.writeString(temporaryDirectory.resolve(FILE_NAME), FILE_CONTENT);

        try (var input = Files.newInputStream(file)) {
            assertFalse(input.markSupported());
            assertTrue(storage.upload("req-retry", input, FILE_NAME));
        }

        cosServer.verify(2, putRequestedFor(urlEqualTo(FILE_PATH))
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("req-retry"))
            .withRequestBody(equalTo(FILE_CONTENT)));
    }

    @Test
    void should_replay_file_after_transient_upload_failure() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        stubUploadRetry(503, "text/plain", "Service unavailable");
        var file = Files.writeString(temporaryDirectory.resolve(FILE_NAME), FILE_CONTENT);

        assertTrue(storage.upload(file.toFile()));

        cosServer.verify(2, putRequestedFor(urlEqualTo(FILE_PATH)).withRequestBody(equalTo(FILE_CONTENT)));
        assertEquals(FILE_CONTENT, Files.readString(file));
    }

    @Test
    void should_replay_upload_after_token_expiration() {
        when(mockAuthenticator.token()).thenReturn("expired", "fresh");
        stubUploadRetry(401, "application/json", """
            {"errors":[{"code":"authentication_token_expired","message":"Token expired"}]}
            """);

        assertTrue(storage.upload(stream(FILE_CONTENT), FILE_NAME));

        cosServer.verify(2, putRequestedFor(urlEqualTo(FILE_PATH)).withRequestBody(equalTo(FILE_CONTENT)));
        cosServer.verify(1, putRequestedFor(urlEqualTo(FILE_PATH)).withHeader("Authorization", equalTo("Bearer fresh")));
    }

    @Test
    void should_return_false_when_upload_returns_non_200() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(put(FILE_PATH).willReturn(aResponse().withStatus(204)));
        boolean result = storage.upload(stream(FILE_CONTENT), FILE_NAME);
        assertFalse(result);
    }

    @Test
    void should_throw_on_upload_null_inputstream() {
        assertThrows(NullPointerException.class, () -> storage.upload((InputStream) null, FILE_NAME));
    }

    @Test
    void should_throw_on_upload_null_filename() {
        assertThrows(NullPointerException.class, () -> storage.upload(stream("x"), null));
    }

    @Test
    void should_upload_url_encoded_filename() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(put(urlPathEqualTo("/" + BUCKET + "/folder/file.pdf")).willReturn(aResponse().withStatus(200)));
        boolean result = storage.upload(stream("data"), "folder/file.pdf");
        assertTrue(result);
    }

    @Test
    void should_upload_file_successfully() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(put(FILE_PATH).willReturn(aResponse().withStatus(200)));
        var file = File.createTempFile("test", ".pdf");
        file.deleteOnExit();
        Files.writeString(file.toPath(), FILE_CONTENT);
        var namedFile = new File(file.getParent(), FILE_NAME);
        file.renameTo(namedFile);
        namedFile.deleteOnExit();
        boolean result = storage.upload(namedFile);
        assertTrue(result);
        cosServer.verify(1, putRequestedFor(urlEqualTo(FILE_PATH)));
    }

    @Test
    void should_throw_on_upload_null_file() {
        assertThrows(NullPointerException.class, () -> storage.upload((File) null));
    }

    @Test
    void should_reject_upload_directory_before_sending_request() {
        var directory = temporaryDirectory.toFile();

        assertThrows(IllegalArgumentException.class, () -> storage.upload(directory));
        assertThrows(IllegalArgumentException.class, () -> storage.upload("req-directory", directory));

        cosServer.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void should_wrap_ioexception_on_upload_file() {
        var missingFile = temporaryDirectory.resolve("missing.pdf").toFile();

        var failure = assertThrows(RuntimeException.class, () -> storage.upload(missingFile));

        assertInstanceOf(FileNotFoundException.class, failure.getCause());
        cosServer.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void should_upload_and_return_null_id() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(put(FILE_PATH).willReturn(aResponse().withStatus(200)));
        String id = storage.uploadAndGetId(stream(FILE_CONTENT), FILE_NAME);
        assertNull(id);
        cosServer.verify(1, putRequestedFor(urlEqualTo(FILE_PATH)));
    }

    @Test
    void should_read_file_successfully() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(get(FILE_PATH).willReturn(aResponse().withStatus(200).withBody(FILE_CONTENT)));
        String content = storage.readFile(FILE_NAME);
        assertEquals(FILE_CONTENT, content);
        cosServer.verify(1, getRequestedFor(urlEqualTo(FILE_PATH)));
    }

    @Test
    void should_read_file_with_tracking_id() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(get(FILE_PATH)
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("req-read"))
            .willReturn(aResponse().withStatus(200).withBody(FILE_CONTENT)));
        String content = storage.readFile("req-read", FILE_NAME);
        assertEquals(FILE_CONTENT, content);
        cosServer.verify(1, getRequestedFor(urlEqualTo(FILE_PATH))
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("req-read")));
    }

    @Test
    void should_throw_file_not_found_on_read_nosuchkey_404() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(get(FILE_PATH).willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/xml")
            .withBody(COS_NOT_FOUND_XML)));

        assertThrows(FileNotFoundException.class, () -> storage.readFile(FILE_NAME));
    }

    @Test
    void should_preserve_bucket_not_found_error_on_read() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(get(FILE_PATH).willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/xml")
            .withBody(COS_GENERIC_404_XML)));

        var error = assertThrows(WatsonxException.class, () -> storage.readFile(FILE_NAME));
        assertEquals(404, error.statusCode());
        assertEquals("NoSuchBucket", error.details().orElseThrow().errors().get(0).code());
    }

    @Test
    void should_throw_on_read_null_filename() {
        assertThrows(NullPointerException.class, () -> storage.readFile(null));
    }

    @Test
    void should_read_url_encoded_filename() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(get(urlPathEqualTo("/" + BUCKET + "/dir/file.txt")).willReturn(aResponse().withStatus(200).withBody("content")));
        String content = storage.readFile("dir/file.txt");
        assertEquals("content", content);
    }

    @Test
    void should_delete_file_successfully() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse().withStatus(204)));
        boolean result = storage.deleteFile(FILE_NAME);
        assertTrue(result);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo(FILE_PATH)));
    }

    @Test
    void should_delete_file_with_tracking_id() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH)
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("req-delete"))
            .willReturn(aResponse().withStatus(204)));
        boolean result = storage.deleteFile("req-delete", FILE_NAME);
        assertTrue(result);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo(FILE_PATH))
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("req-delete")));
    }

    @Test
    void should_return_false_when_delete_returns_non_204() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse().withStatus(200)));
        boolean result = storage.deleteFile(FILE_NAME);
        assertFalse(result);
    }

    @Test
    void should_throw_file_not_found_on_delete_nosuchkey_404() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/xml")
            .withBody(COS_NOT_FOUND_XML)));

        assertThrows(FileNotFoundException.class, () -> storage.deleteFile(FILE_NAME));
    }

    @Test
    void should_preserve_bucket_not_found_error_on_delete() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/xml")
            .withBody(COS_GENERIC_404_XML)));

        var error = assertThrows(WatsonxException.class, () -> storage.deleteFile(FILE_NAME));
        assertEquals(404, error.statusCode());
        assertEquals("NoSuchBucket", error.details().orElseThrow().errors().get(0).code());
    }

    @Test
    void should_delete_file_async_successfully() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse().withStatus(204)));
        storage.deleteFileAsync(FILE_NAME);
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo(FILE_PATH)), 1);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo(FILE_PATH)));
    }

    @Test
    void should_swallow_error_on_delete_file_async_failure() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/xml")
            .withBody(COS_NOT_FOUND_XML)));

        storage.deleteFileAsync(FILE_NAME);
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo(FILE_PATH)), 1);
    }

    @Test
    void should_delete_file_async_with_tracking_id() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH)
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("txn-123"))
            .willReturn(aResponse().withStatus(204)));

        var req = DeleteFileRequest.of("txn-123", BUCKET, FILE_NAME);
        ((CosStorageService) storage).deleteFileAsync(req);

        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo(FILE_PATH)), 1);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo(FILE_PATH))
            .withHeader("Watsonx-AI-SDK-Request-Id", equalTo("txn-123")));
    }

    @Test
    void should_delete_file_async_without_tracking_id() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse().withStatus(204)));

        var req = DeleteFileRequest.of(null, BUCKET, FILE_NAME);
        ((CosStorageService) storage).deleteFileAsync(req);

        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo(FILE_PATH)), 1);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo(FILE_PATH)));
    }

    @Test
    void should_swallow_error_on_delete_file_async_request_failure() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/xml")
            .withBody(COS_NOT_FOUND_XML)));

        var req = DeleteFileRequest.of("txn-err", BUCKET, FILE_NAME);
        ((CosStorageService) storage).deleteFileAsync(req);
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo(FILE_PATH)), 1);
    }

    @Test
    void should_rethrow_watsonx_exception_on_read_404_without_xml_body() {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(get(FILE_PATH).willReturn(aResponse()
            .withStatus(404)
            .withBody("not found")));

        assertThrows(WatsonxException.class,
            () -> storage.readFile(FILE_NAME));
    }

    @Test
    void should_rethrow_watsonx_exception_on_delete_404_without_xml_body() throws Exception {
        when(mockAuthenticator.token()).thenReturn("tok");
        cosServer.stubFor(delete(FILE_PATH).willReturn(aResponse()
            .withStatus(404)
            .withBody("not found")));

        assertThrows(WatsonxException.class,
            () -> storage.deleteFile(FILE_NAME));
    }

    private StorageOperations buildCosService() {
        var cosUrl = "http://localhost:" + cosServer.getPort();
        return StorageFactory.cos(
            cosUrl,
            BUCKET,
            mockAuthenticator,
            null,
            null,
            Duration.ofSeconds(10),
            false, false, null,
            Level.INFO,
            null,
            Level.INFO,
            true);
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private void stubUploadRetry(int status, String contentType, String body) {
        cosServer.stubFor(put(FILE_PATH)
            .inScenario("upload-retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(status).withHeader("Content-Type", contentType).withBody(body))
            .willSetStateTo("retry"));
        cosServer.stubFor(put(FILE_PATH)
            .inScenario("upload-retry")
            .whenScenarioStateIs("retry")
            .willReturn(aResponse().withStatus(200)));
    }
}
