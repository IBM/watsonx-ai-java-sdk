/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.file.FileListRequest;
import com.ibm.watsonx.ai.file.FileRetrieveRequest;
import com.ibm.watsonx.ai.file.FileService;
import com.ibm.watsonx.ai.file.FileUploadRequest;
import com.ibm.watsonx.ai.file.Order;
import com.ibm.watsonx.ai.file.Purpose;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest extends AbstractWatsonxTest {

    private final String PROJECT_ID = "project_id";
    private final String SPACE_ID = "space_id";
    private String BASE_URL;

    @BeforeEach
    void setUp() {
        when(mockAuthenticator.token()).thenReturn("token");
        BASE_URL = "http://localhost:%s".formatted(wireMock.getPort());
    }

    @Test
    void should_upload_a_file() throws Exception {

        var RESPONSE = """
            {
                "id" : "file-AQIDkP4L79L9Wyuo--GR5E26LgIzQVFNSGNBTlB6OTRiNm5GTl9KeFY2ajVfOE9PUUF5c2ktMWxzZ1VKUHZYT1J4blM2WlZF",
                "object" : "file",
                "bytes" : 858,
                "created_at" : 1772228448309,
                "expires_at" : 1774820448309,
                "filename" : "@file_to_upload.jsonl",
                "purpose" : "batch"
            }""";

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            wireMock.stubFor(post("/ml/v1/files?version=%s".formatted(API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .withMultipartRequestBody(
                    aMultipart()
                        .withName("purpose")
                        .withBody(equalTo("batch"))
                )
                .withMultipartRequestBody(
                    aMultipart()
                        .withName("file")
                        .withHeader("Content-Type", containing("application/octet-stream"))
                        .withBody(equalTo(
                            """
                                {"custom_id": "a", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of Italy"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}
                                {"custom_id": "b", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of French"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}
                                {"custom_id": "c", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of Germany"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}"""))
                )
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(RESPONSE)));

            var path = assertDoesNotThrow(() -> Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
            var builder = FileService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(BASE_URL);

            if (header.getKey().equals("X-IBM-Project-ID"))
                builder.projectId(PROJECT_ID);
            else
                builder.spaceId(SPACE_ID);

            var fileService = builder.build();
            var response = fileService.upload(path);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_upload_a_file_and_override_parameters() throws Exception {

        var RESPONSE = """
            {
                "id" : "file-AQIDkP4L79L9Wyuo--GR5E26LgIzQVFNSGNBTlB6OTRiNm5GTl9KeFY2ajVfOE9PUUF5c2ktMWxzZ1VKUHZYT1J4blM2WlZF",
                "object" : "file",
                "bytes" : 858,
                "created_at" : 1772228448309,
                "expires_at" : 1774820448309,
                "filename" : "@file_to_upload.jsonl",
                "purpose" : "batch"
            }""";

        wireMock.stubFor(post("/ml/v1/files?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo("override-project-id"))
            .withHeader("X-IBM-Space-ID", equalTo("override-space-id"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .withMultipartRequestBody(
                aMultipart()
                    .withName("purpose")
                    .withBody(equalTo("batch"))
            )
            .withMultipartRequestBody(
                aMultipart()
                    .withName("file")
                    .withHeader("Content-Type", containing("application/octet-stream"))
                    .withBody(equalTo(
                        """
                            {"custom_id": "a", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of Italy"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}
                            {"custom_id": "b", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of French"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}
                            {"custom_id": "c", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of Germany"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}"""))
            )
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)));

        var is = assertDoesNotThrow(() -> new FileInputStream(new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI())));
        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .baseUrl(BASE_URL)
            .build();

        var response = fileService.upload(
            FileUploadRequest.builder()
                .inputStream(is)
                .fileName("@file_to_upload.jsonl")
                .purpose(Purpose.BATCH)
                .projectId("override-project-id")
                .spaceId("override-space-id")
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_file_does_not_exist() {
        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .baseUrl(BASE_URL)
            .build();

        var ex = assertThrows(RuntimeException.class, () -> fileService.upload(Path.of("non-existent-file")));
        assertInstanceOf(FileNotFoundException.class, ex.getCause());
    }

    @Test
    void should_list_all_files() {

        var RESPONSE = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_response_list.json").toURI())));

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            wireMock.stubFor(get("/ml/v1/files?version=%s".formatted(API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(RESPONSE)));

            var builder = FileService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(BASE_URL);

            if (header.getKey().equals("X-IBM-Project-ID"))
                builder.projectId(PROJECT_ID);
            else
                builder.spaceId(SPACE_ID);

            var fileService = builder.build();
            var response = fileService.list();
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_list_files_with_filters() {

        var RESPONSE = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_response_list.json").toURI())));

        String queryParams = """
            &after=file-AQIDkP4L79L9Wyuo--GR5E26LgIzQVFNSGNBTlB6OTRiNm5GTl9KeFY2ajVfOE9PUUEwMFVpRzBzZTBjbGpLYWN4T0tWTUxj\
            &limit=10\
            &order=asc\
            &purpose=batch""";

        wireMock.stubFor(get("/ml/v1/files?version=%s%s".formatted(API_VERSION, queryParams))
            .withHeader("X-IBM-Project-ID", equalTo("new-project-id"))
            .withHeader("X-IBM-Space-ID", equalTo("new-space-id"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .baseUrl(BASE_URL)
            .build();

        var response = fileService.list(
            FileListRequest.builder()
                .after("file-AQIDkP4L79L9Wyuo--GR5E26LgIzQVFNSGNBTlB6OTRiNm5GTl9KeFY2ajVfOE9PUUEwMFVpRzBzZTBjbGpLYWN4T0tWTUxj")
                .limit(10)
                .order(Order.ASC)
                .purpose(Purpose.BATCH)
                .projectId("new-project-id")
                .spaceId("new-space-id")
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_retrieve_the_content_of_a_file() {

        var RESPONSE = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));
        var FILE = "file-AQIDkP4L79L9Wyuo--GR5E26LgIzQVFNSGNBTlB6OTRiNm5GTl9KeFY2ajVfOE9PUUEzbzhTa01XaGtlWmczN0dHbHZGTFFZ";

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(FILE, API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(RESPONSE)));

            var builder = FileService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(BASE_URL);

            if (header.getKey().equals("X-IBM-Project-ID"))
                builder.projectId(PROJECT_ID);
            else
                builder.spaceId(SPACE_ID);

            var fileService = builder.build();
            var response = fileService.retrieve(FILE);
            assertEquals(RESPONSE, response);
        });
    }

    @Test
    void should_retrieve_the_content_of_a_file_with_overridden_parameters() {

        var RESPONSE = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));
        var FILE = "file-AQIDkP4L79L9Wyuo--GR5E26LgIzQVFNSGNBTlB6OTRiNm5GTl9KeFY2ajVfOE9PUUEzbzhTa01XaGtlWmczN0dHbHZGTFFZ";

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(FILE, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo("my-project-id"))
            .withHeader("X-IBM-Space-ID", equalTo("my-space-id"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .baseUrl(BASE_URL)
            .build();

        var response = fileService.retrieve(
            FileRetrieveRequest.builder()
                .fileId(FILE)
                .projectId("my-project-id")
                .spaceId("my-space-id")
                .transactionId("transaction-id")
                .build());

        assertEquals(RESPONSE, response);
    }

    @Test
    void should_throw_exception_when_file_service_returns_authentication_error() throws Exception {

        var ERROR = """
            {
                "message": "401 Failed to authenticate the request due to an expired token",
                "status": 401,
                "error": {
                    "message": "Failed to authenticate the request due to an expired token",
                    "type": "authentication_error",
                    "param": null,
                    "code": "authentication_token_expired"
                },
                "code": "authentication_token_expired",
                "param": null,
                "type": "authentication_error",
                "more_info": "https://cloud.ibm.com/apidocs/watsonx-ai#upload_file",
                "trace": "c8d777a8fe6551b6069a8cef52c52956"
            }""";

        wireMock.stubFor(post("/ml/v1/files?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .withMultipartRequestBody(
                aMultipart()
                    .withName("purpose")
                    .withBody(equalTo("batch"))
            )
            .withMultipartRequestBody(
                aMultipart()
                    .withName("file")
                    .withHeader("Content-Type", containing("application/octet-stream"))
                    .withBody(equalTo(
                        """
                            {"custom_id": "a", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of Italy"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}
                            {"custom_id": "b", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of French"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}
                            {"custom_id": "c", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small","messages":[{"role":"user","content":[{"type":"text","text":"Capital of Germany"}]}],"max_completion_tokens":0,"time_limit":30000,"temperature":0}}"""))
            )
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(401)
                .withBody(ERROR)));

        var path = assertDoesNotThrow(() -> Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var ex = assertThrows(RuntimeException.class, () -> fileService.upload(path));
        assertTrue(ex.getMessage().startsWith("Max retries reached for request"));

        var watsonxException = (WatsonxException) ex.getCause();
        assertTrue(watsonxException.details().isPresent());
        assertEquals(401, watsonxException.details().get().statusCode());
        assertNotNull(401, watsonxException.details().get().trace());
        assertEquals("authentication_token_expired", watsonxException.details().get().errors().get(0).code());
        assertEquals("Failed to authenticate the request due to an expired token", watsonxException.details().get().errors().get(0).message());
        assertEquals(401, watsonxException.statusCode());

        wireMock.verify(2, postRequestedFor(urlPathEqualTo("/ml/v1/files"))
            .withQueryParam("version", equalTo(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
        );
        JSONAssert.assertEquals(ERROR, ex.getCause().getMessage(), true);
    }

    @Test
    void should_throw_runtime_exception_on_http_client_error() throws Exception {

        var path = assertDoesNotThrow(() -> Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        when(mockSecureHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));
        withWatsonxServiceMock(() -> {
            var fileService = FileService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .projectId("project-id")
                .build();

            assertThrows(RuntimeException.class, () -> fileService.upload(path), "IOException");
            assertThrows(RuntimeException.class, () -> fileService.list(), "IOException");
            assertThrows(RuntimeException.class, () -> fileService.retrieve("file-id"), "IOException");
        });
    }
}
