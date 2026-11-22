/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.batch.BatchCancelRequest;
import com.ibm.watsonx.ai.batch.BatchCreateRequest;
import com.ibm.watsonx.ai.batch.BatchListRequest;
import com.ibm.watsonx.ai.batch.BatchRetrieveRequest;
import com.ibm.watsonx.ai.batch.BatchService;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.Polling;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.file.FileDeleteRequest;
import com.ibm.watsonx.ai.file.FileRetrieveRequest;
import com.ibm.watsonx.ai.file.FileService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisabledInNativeImage
@Isolated("Resets the shared HTTP client provider while exercising real HTTP requests.")
public class BatchServiceTest extends AbstractWatsonxTest {

    @Mock
    private FileService mockFileService;

    private static final String PROJECT_ID = "project_id";
    private static final String SPACE_ID = "space_id";
    private static final String ENDPOINT = "/v1/chat/completions";
    private static final String FILE_ID = "file-AQIDkP4L79L9Wyuo--GR5E26LgIzQVFNSGNBTlB6OTRiNm5GTl9KeFY2ajVfOE9PUUF5c2ktMWxzZ1VKUHZYT1J4blM2WlZF";
    private static final String BATCH_ID = "batch-AQIDkP4L79L9Wyuo--GR5E26LgOtk0emSyJDzrBhZgrBeCdG";
    private static final String OUTPUT_FILE_ID =
        "file-AQIDkP4L79L9Wyuo--GR5E26LgIzQVFNSGNBTlB6OTRiNm5GTl9KeFY2ajVfOE9PUUEzbzhTa01XaGtlWmczN0dHbHZGTFFZ";

    private String BASE_URL;
    private String SUBMIT_RESPONSE =
        assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("batch_submit_response.json").toURI())));
    private String LIST_RESPONSE =
        assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("batch_list_response.json").toURI())));
    private String FILE_UPLOAD_RESPONSE = """
        {
            "id" : "%s",
            "object" : "file",
            "bytes" : 858,
            "created_at" : 1772228448309,
            "expires_at" : 1774820448309,
            "filename" : "file_to_upload.jsonl",
            "purpose" : "batch"
        }""".formatted(FILE_ID);

    private static final String FILE_DELETE_RESPONSE = """
        {
            "id": "my-id",
            "deleted": true,
            "object": "file"
        }""";


    @BeforeEach
    void setUp() {
        super.setUp();
        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.scheme()).thenReturn("Bearer");
        BASE_URL = "http://localhost:%s".formatted(wireMock.getPort());

    }

    @Test
    void should_submit_a_batch_job() {

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(SUBMIT_RESPONSE)));

            var builder = BatchService.builder()
                .authenticator(mockAuthenticator)
                .endpoint(ENDPOINT)
                .fileService(mockFileService)
                .baseUrl(BASE_URL);

            if (header.getKey().equals("X-IBM-Project-ID"))
                builder.projectId(PROJECT_ID);
            else
                builder.spaceId(SPACE_ID);

            var batchService = builder.build();
            var response = batchService.submit(
                BatchCreateRequest.builder()
                    .inputFileId(FILE_ID)
                    .completionWindow("1h")
                    .metadata(Map.of("test", "test"))
                    .build());

            JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_submit_a_batch_job_with_parameters() {

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo("override-project-id"))
            .withHeader("X-IBM-Space-ID", equalTo("override-space-id"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .endpoint(ENDPOINT)
            .fileService(mockFileService)
            .baseUrl(BASE_URL)
            .build();

        var response = batchService.submit(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .projectId("override-project-id")
                .spaceId("override-space-id")
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_submit_a_batch_job_via_path() {

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            stubFileUpload(header.getKey(), header.getValue());
            wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(SUBMIT_RESPONSE)));

            var fileService = buildFileService(header.getKey(), header.getValue());
            var batchService = buildBatchService(header.getKey(), header.getValue(), fileService);

            var path = assertDoesNotThrow(() -> Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
            var response = batchService.submit(path);
            JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_submit_a_batch_job_via_file() {

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            stubFileUpload(header.getKey(), header.getValue());
            wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(SUBMIT_RESPONSE)));

            var fileService = buildFileService(header.getKey(), header.getValue());
            var batchService = buildBatchService(header.getKey(), header.getValue(), fileService);

            var file = assertDoesNotThrow(() -> new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
            var response = batchService.submit(file);
            JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_submit_a_batch_job_via_input_stream() {

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            stubFileUpload(header.getKey(), header.getValue());
            wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(SUBMIT_RESPONSE)));

            var fileService = buildFileService(header.getKey(), header.getValue());
            var batchService = buildBatchService(header.getKey(), header.getValue(), fileService);

            var is = assertDoesNotThrow(() -> new FileInputStream(new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI())));
            var response = batchService.submit(is, "file_to_upload.jsonl");
            JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_submit_a_batch_job_via_path_with_parameters() {

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var path = assertDoesNotThrow(() -> Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var response = batchService.submit(path, BatchCreateRequest.builder().build());

        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_submit_a_batch_job_via_file_with_parameters() {

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var file = assertDoesNotThrow(() -> new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var response = batchService.submit(file, BatchCreateRequest.builder().build());

        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_submit_a_batch_job_via_input_stream_with_parameters() {

        stubFileUpload("X-IBM-Project-ID", "override-project-id");
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo("override-project-id"))
            .withHeader("X-IBM-Space-ID", equalTo("override-space-id"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var is = assertDoesNotThrow(() -> new FileInputStream(new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI())));
        var response = batchService.submit(is, "file_to_upload.jsonl",
            BatchCreateRequest.builder()
                .projectId("override-project-id")
                .spaceId("override-space-id")
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_throw_exception_when_input_file_id_is_set_on_upload() {

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var is = assertDoesNotThrow(() -> new FileInputStream(new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI())));
        assertThrows(IllegalArgumentException.class, () -> batchService.submit(is, "file_to_upload.jsonl",
            BatchCreateRequest.builder()
                .inputFileId("already-set-file-id")
                .build()));
    }

    @Test
    void should_retrieve_a_batch_job() {

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            wireMock.stubFor(get("/ml/v1/batches/%s?version=%s".formatted(BATCH_ID, API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(SUBMIT_RESPONSE)));

            var builder = BatchService.builder()
                .authenticator(mockAuthenticator)
                .endpoint(ENDPOINT)
                .fileService(mockFileService)
                .baseUrl(BASE_URL);

            if (header.getKey().equals("X-IBM-Project-ID"))
                builder.projectId(PROJECT_ID);
            else
                builder.spaceId(SPACE_ID);

            var batchService = builder.build();
            var response = batchService.retrieve(
                BatchRetrieveRequest.builder()
                    .batchId(BATCH_ID)
                    .build());

            JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_retrieve_a_batch_job_with_parameters() {

        wireMock.stubFor(get("/ml/v1/batches/%s?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo("override-project-id"))
            .withHeader("X-IBM-Space-ID", equalTo("override-space-id"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .endpoint(ENDPOINT)
            .fileService(mockFileService)
            .baseUrl(BASE_URL)
            .build();

        var response = batchService.retrieve(
            BatchRetrieveRequest.builder()
                .batchId(BATCH_ID)
                .projectId("override-project-id")
                .spaceId("override-space-id")
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_submit_and_fetch_results_when_job_completes_immediately() throws Exception {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var results = batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .build(),
            ChatResponse.class);

        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        assertEquals(3, results.size());
        assertEquals(200, results.get(0).response().statusCode());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_submit_and_fetch_results_after_polling() throws Exception {

        var IN_PROGRESS_RESPONSE = SUBMIT_RESPONSE.replace("\"completed\"", "\"in_progress\"");
        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(IN_PROGRESS_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/batches/%s?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var results = batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .build(),
            ChatResponse.class);

        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_submit_and_fetch_results_via_path() throws Exception {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var path = assertDoesNotThrow(() -> Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var results = batchService.submitAndFetch(path, ChatResponse.class);

        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_submit_and_fetch_results_via_path_with_parameters() throws Exception {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var path = assertDoesNotThrow(() -> Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var results = batchService.submitAndFetch(path, BatchCreateRequest.builder().build(), ChatResponse.class);

        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_submit_and_fetch_results_via_file() throws Exception {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var file = assertDoesNotThrow(() -> new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var results = batchService.submitAndFetch(file, ChatResponse.class);

        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_submit_and_fetch_results_via_file_with_parameters() throws Exception {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var file = assertDoesNotThrow(() -> new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var results = batchService.submitAndFetch(file, BatchCreateRequest.builder().build(), ChatResponse.class);

        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_submit_and_fetch_results_via_input_stream() throws Exception {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var is = assertDoesNotThrow(() -> new FileInputStream(new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI())));
        var results = batchService.submitAndFetch(is, "file_to_upload.jsonl", ChatResponse.class);

        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_submit_and_fetch_results_via_input_stream_with_parameters() throws Exception {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var is = assertDoesNotThrow(() -> new FileInputStream(new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI())));
        var results = batchService.submitAndFetch(is, "file_to_upload.jsonl", BatchCreateRequest.builder().build(), ChatResponse.class);

        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_throw_exception_when_batch_job_fails() throws Exception {

        var FAILED_RESPONSE = SUBMIT_RESPONSE.replace("\"completed\"", "\"failed\"");

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FAILED_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var ex = assertThrows(RuntimeException.class, () -> batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .build(),
            ChatResponse.class));

        assertEquals(true, ex.getMessage().startsWith("The batch operation did not complete successfully (status: failed)"));
        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + FILE_ID)));
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + OUTPUT_FILE_ID)));
    }

    @Test
    void should_terminate_cleanly_on_unrecognized_status() throws Exception {

        var CANCELLED_RESPONSE = SUBMIT_RESPONSE.replace("\"completed\"", "\"cancelled\"");

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(CANCELLED_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var ex = assertThrows(RuntimeException.class, () -> batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .build(),
            ChatResponse.class));

        assertEquals(true, ex.getMessage().startsWith("The batch operation did not complete successfully (status: cancelled)"));
        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + FILE_ID)));
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + OUTPUT_FILE_ID)));
    }

    @Test
    void should_throw_exception_when_timeout_is_exceeded() throws Exception {

        var IN_PROGRESS_RESPONSE = SUBMIT_RESPONSE.replace("\"completed\"", "\"in_progress\"");

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(IN_PROGRESS_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/batches/%s?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(IN_PROGRESS_RESPONSE)));

        wireMock.stubFor(post("/ml/v1/batches/%s/cancel?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{}")));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var ex = assertThrows(RuntimeException.class, () -> batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .timeout(Duration.ofMillis(1))
                .build(),
            ChatResponse.class));

        assertEquals(true, ex.getMessage().startsWith(
            "The execution of the batch operation for the file \"%s\" took longer than the timeout".formatted(FILE_ID)));
        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + FILE_ID))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID)));
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + OUTPUT_FILE_ID))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID)));
    }

    @Test
    void should_throw_exception_when_service_level_timeout_is_exceeded() throws Exception {

        var IN_PROGRESS_RESPONSE = SUBMIT_RESPONSE.replace("\"completed\"", "\"in_progress\"");

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(IN_PROGRESS_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/batches/%s?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(IN_PROGRESS_RESPONSE)));

        wireMock.stubFor(post("/ml/v1/batches/%s/cancel?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{}")));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .timeout(Duration.ofMillis(200))
            .baseUrl(BASE_URL)
            .build();

        // No timeout set on the request - service-level timeout (200ms) should apply.
        // The first sleep in the polling loop is 100ms, so after one poll the timeout will be exceeded.
        var ex = assertThrows(RuntimeException.class, () -> batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .build(),
            ChatResponse.class));

        assertEquals(true, ex.getMessage().startsWith(
            "The execution of the batch operation for the file \"%s\" took longer than the timeout".formatted(FILE_ID)));
        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + FILE_ID))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID)));
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + OUTPUT_FILE_ID))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID)));
    }

    @Test
    void should_use_request_timeout_over_service_level_timeout() throws Exception {

        var IN_PROGRESS_RESPONSE = SUBMIT_RESPONSE.replace("\"completed\"", "\"in_progress\"");

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(IN_PROGRESS_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/batches/%s?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(IN_PROGRESS_RESPONSE)));

        wireMock.stubFor(post("/ml/v1/batches/%s/cancel?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{}")));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        // Service-level timeout is very long, but request timeout overrides it to 1ms
        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .timeout(Duration.ofMinutes(10))
            .baseUrl(BASE_URL)
            .build();

        var ex = assertThrows(RuntimeException.class, () -> batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .timeout(Duration.ofMillis(1))
                .build(),
            ChatResponse.class));

        assertEquals(true, ex.getMessage().startsWith(
            "The execution of the batch operation for the file \"%s\" took longer than the timeout".formatted(FILE_ID)));
        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + FILE_ID))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID)));
        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/" + OUTPUT_FILE_ID))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_submit_and_fetch_results_with_overridden_parameters() {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        withWatsonxServiceMock(() -> {

            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn(SUBMIT_RESPONSE);
            when(mockFileService.retrieve(any(FileRetrieveRequest.class))).thenReturn(OUTPUT_CONTENT);

            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var batchService = BatchService.builder()
                .authenticator(mockAuthenticator)
                .projectId(PROJECT_ID)
                .spaceId(SPACE_ID)
                .endpoint(ENDPOINT)
                .fileService(mockFileService)
                .baseUrl(BASE_URL)
                .build();

            var results = batchService.submitAndFetch(
                BatchCreateRequest.builder()
                    .inputFileId(FILE_ID)
                    .projectId("override-project-id")
                    .spaceId("override-space-id")
                    .transactionId("transaction-id")
                    .build(),
                ChatResponse.class);

            assertEquals(3, results.size());
            assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());

            // Verify that the submit request carried the overridden headers
            var submitRequest = mockHttpRequest.getValue();
            assertEquals("override-project-id", submitRequest.headers().firstValue("X-IBM-Project-ID").orElse(null));
            assertEquals("override-space-id", submitRequest.headers().firstValue("X-IBM-Space-ID").orElse(null));
            assertEquals("transaction-id", submitRequest.headers().firstValue(TRANSACTION_ID_HEADER).orElse(null));
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_submit_and_fetch_results_with_overridden_parameters_after_polling() {

        var IN_PROGRESS_RESPONSE = SUBMIT_RESPONSE.replace("\"completed\"", "\"in_progress\"");
        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        withWatsonxServiceMock(() -> {

            when(mockHttpResponse.statusCode()).thenReturn(200);
            // First call (submit) returns in_progress; second call (retrieve) returns completed
            when(mockHttpResponse.body())
                .thenReturn(IN_PROGRESS_RESPONSE)
                .thenReturn(SUBMIT_RESPONSE);
            when(mockFileService.retrieve(any(FileRetrieveRequest.class))).thenReturn(OUTPUT_CONTENT);

            try {
                when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
                    .thenReturn(mockHttpResponse);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            var batchService = BatchService.builder()
                .authenticator(mockAuthenticator)
                .projectId(PROJECT_ID)
                .spaceId(SPACE_ID)
                .endpoint(ENDPOINT)
                .fileService(mockFileService)
                .baseUrl(BASE_URL)
                .build();

            var results = batchService.submitAndFetch(
                BatchCreateRequest.builder()
                    .inputFileId(FILE_ID)
                    .projectId("override-project-id")
                    .spaceId("override-space-id")
                    .transactionId("transaction-id")
                    .build(),
                ChatResponse.class);

            assertEquals(3, results.size());
            assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());

            // Verify that both the submit and the polling retrieve requests carried the overridden headers
            var capturedRequests = mockHttpRequest.getAllValues();
            assertEquals(2, capturedRequests.size());

            var submitRequest = capturedRequests.get(0);
            assertEquals("override-project-id", submitRequest.headers().firstValue("X-IBM-Project-ID").orElse(null));
            assertEquals("override-space-id", submitRequest.headers().firstValue("X-IBM-Space-ID").orElse(null));
            assertEquals("transaction-id", submitRequest.headers().firstValue(TRANSACTION_ID_HEADER).orElse(null));

            var retrieveRequest = capturedRequests.get(1);
            assertEquals("override-project-id", retrieveRequest.headers().firstValue("X-IBM-Project-ID").orElse(null));
            assertEquals("override-space-id", retrieveRequest.headers().firstValue("X-IBM-Space-ID").orElse(null));
        });
    }

    @Test
    void should_submit_a_batch_job_via_path_with_overridden_parameters() {

        stubFileUpload("X-IBM-Project-ID", "override-project-id");
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo("override-project-id"))
            .withHeader("X-IBM-Space-ID", equalTo("override-space-id"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var path = assertDoesNotThrow(() -> Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var response = batchService.submit(path,
            BatchCreateRequest.builder()
                .projectId("override-project-id")
                .spaceId("override-space-id")
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_submit_a_batch_job_via_file_with_overridden_parameters() {

        stubFileUpload("X-IBM-Project-ID", "override-project-id");
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo("override-project-id"))
            .withHeader("X-IBM-Space-ID", equalTo("override-space-id"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var file = assertDoesNotThrow(() -> new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI()));
        var response = batchService.submit(file,
            BatchCreateRequest.builder()
                .projectId("override-project-id")
                .spaceId("override-space-id")
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_cancel_a_batch_request() {

        var RESPONSE = """
            {
                "id": "%s",
                "object": "batch",
                "endpoint": "/v1/chat/completions",
                "input_file_id": "%s",
                "completion_window": "24h",
                "status": "cancelled",
                "created_at": 1772355982,
                "in_progress_at": 1772355983,
                "expires_at": 1772442382,
                "finalizing_at": 1772355990,
                "cancelled_at": 1772355990,
                "request_counts": {
                    "total": 13,
                    "completed": 13,
                    "failed": 0
                },
                "metadata": {
                    "created": "Andrea",
                    "runtime_instance_id": "2bf49f89-f825-4f7b-ab02-831f5caaffc5"
                }
            }""".formatted(BATCH_ID, FILE_ID);

        wireMock.stubFor(post("/ml/v1/batches/%s/cancel?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .withHeader("X-IBM-Space-ID", equalTo(SPACE_ID))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)));

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .endpoint(ENDPOINT)
            .fileService(mockFileService)
            .baseUrl(BASE_URL)
            .build();

        var batchData = batchService.cancel(
            BatchCancelRequest.builder()
                .batchId(BATCH_ID)
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(RESPONSE, Json.toJson(batchData), true);
    }

    @Test
    void should_fail_to_cancel_batch() {

        wireMock.stubFor(post("/ml/v1/batches/%s/cancel?version=%s".formatted(FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("""
                    {
                        "error": {
                            "code": "Internal Server Error",
                            "message": "failed to cancel batch: failed to cancel batch",
                            "request_id": "c725a5d4e7a3f3f28aab791485723a75"
                        }
                    }""")));

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(mockFileService)
            .baseUrl(BASE_URL)
            .build();

        var ex = assertThrows(WatsonxException.class, () -> batchService.cancel(FILE_ID));
        JSONAssert.assertEquals("""
            {
                "error": {
                    "code": "Internal Server Error",
                    "message": "failed to cancel batch: failed to cancel batch",
                    "request_id": "c725a5d4e7a3f3f28aab791485723a75"
                }
            }""", ex.getMessage(), true);
    }

    @Test
    void should_retrieve_a_batch_by_id() {

        wireMock.stubFor(get("/ml/v1/batches/%s?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(mockFileService)
            .baseUrl(BASE_URL)
            .build();

        var batchData = batchService.retrieve(BATCH_ID);
        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(batchData), true);
    }

    @Test
    void should_cancel_a_batch_by_id() {

        wireMock.stubFor(post("/ml/v1/batches/%s/cancel?version=%s".formatted(BATCH_ID, API_VERSION))
            .withHeader("X-IBM-Space-ID", equalTo(SPACE_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .spaceId(SPACE_ID)
            .endpoint(ENDPOINT)
            .baseUrl(BASE_URL)
            .fileService(mockFileService)
            .build();

        var batchData = batchService.cancel(BATCH_ID);
        JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(batchData), true);
    }

    @Test
    void should_list_batches() {

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            wireMock.stubFor(get("/ml/v1/batches?version=%s".formatted(API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(LIST_RESPONSE)));

            var batchService = buildBatchService(header.getKey(), header.getValue(), mockFileService);
            JSONAssert.assertEquals(LIST_RESPONSE, Json.toJson(batchService.list()), true);
        });
    }

    @Test
    void should_list_batches_with_limit() {

        wireMock.stubFor(get("/ml/v1/batches?version=%s&limit=2".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .withHeader("X-IBM-Space-ID", equalTo(SPACE_ID))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction-id"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(LIST_RESPONSE)));

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .spaceId(SPACE_ID)
            .endpoint(ENDPOINT)
            .baseUrl(BASE_URL)
            .fileService(mockFileService)
            .build();

        var response = batchService.list(
            BatchListRequest.builder()
                .limit(2)
                .transactionId("transaction-id")
                .build());

        JSONAssert.assertEquals(LIST_RESPONSE, Json.toJson(response), true);
    }

    @Test
    void should_submit_a_batch_job_via_input_stream_without_file_name() {

        Stream.of(Map.entry("X-IBM-Project-ID", PROJECT_ID), Map.entry("X-IBM-Space-ID", SPACE_ID)).forEach(header -> {

            wireMock.resetAll();
            stubFileUpload(header.getKey(), header.getValue());
            wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
                .withHeader(header.getKey(), equalTo(header.getValue()))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(SUBMIT_RESPONSE)));

            var fileService = buildFileService(header.getKey(), header.getValue());
            var batchService = buildBatchService(header.getKey(), header.getValue(), fileService);

            var is = assertDoesNotThrow(() -> new FileInputStream(new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI())));
            var response = batchService.submit(is);
            JSONAssert.assertEquals(SUBMIT_RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_submit_and_fetch_results_via_input_stream_without_file_name() {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .inScenario("delete")
            .willSetStateTo("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .inScenario("delete_outputfile")
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var is = assertDoesNotThrow(() -> new FileInputStream(new File(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI())));
        var results = batchService.submitAndFetch(is, ChatResponse.class);

        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_not_remove_uploaded_file_and_output_file_after_submit_and_fetch() {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .removeOutputFile(false)
            .removeUploadedFile(false)
            .build();

        var results = batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .build(),
            ChatResponse.class);

        assertEquals(3, results.size());
        assertEquals(200, results.get(0).response().statusCode());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());

        wireMock.verify(0, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_not_remove_uploaded_file_and_output_file_after_submit_and_fetch_with_parameters() {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(OUTPUT_CONTENT)));

        var fileService = FileService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .baseUrl(BASE_URL)
            .build();

        var batchService = BatchService.builder()
            .authenticator(mockAuthenticator)
            .projectId(PROJECT_ID)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL)
            .build();

        var results = batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .removeOutputFile(false)
                .removeUploadedFile(false)
                .build(),
            ChatResponse.class);

        assertEquals(3, results.size());
        assertEquals(200, results.get(0).response().statusCode());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());

        wireMock.verify(0, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_throw_runtime_exception_on_http_client_error() throws Exception {

        when(mockSecureHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));
        withWatsonxServiceMock(() -> {
            var batchService = BatchService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .authenticator(mockAuthenticator)
                .projectId("project-id")
                .endpoint("endpoint")
                .fileService(mockFileService)
                .build();

            assertThrows(RuntimeException.class, () -> batchService.cancel("test"), "IOException");
            assertThrows(RuntimeException.class, () -> batchService.list(), "IOException");
            assertThrows(RuntimeException.class, () -> batchService.retrieve("test"), "IOException");
            assertThrows(RuntimeException.class, () -> batchService.submit(BatchCreateRequest.builder().build()), "IOException");
        });
    }

    @Test
    void should_throw_exception_when_remove_uploaded_file_is_true() {
        var batchService = buildBatchService("X-IBM-Project-ID", "project-id", buildFileService("X-IBM-Project-ID", "project-id"));
        var request = BatchCreateRequest.builder()
            .inputFileId("input-file-id")
            .removeUploadedFile(true)
            .build();
        assertThrows(IllegalArgumentException.class, () -> batchService.submit(request), "removeUploadedFile is not supported for the async method");
    }

    @Test
    void should_throw_exception_when_remove_output_file_is_true() {
        var batchService = buildBatchService("X-IBM-Project-ID", "project-id", buildFileService("X-IBM-Project-ID", "project-id"));
        var request = BatchCreateRequest.builder()
            .inputFileId("input-file-id")
            .removeOutputFile(true)
            .build();
        assertThrows(IllegalArgumentException.class, () -> batchService.submit(request), "removeOutputFile is not supported for the async method");
    }

    @Test
    void should_batch_chat_responses() throws InterruptedException {

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);

        wireMock.stubFor(post("/ml/v1/batches?version=%s".formatted(API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(SUBMIT_RESPONSE)));

        // Mock the output file with results in non-sequential order (2, 0, 1) to test sorting
        wireMock.stubFor(get("/ml/v1/files/%s/content?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(
                    """
                            {"id":"batch-result-2","custom_id":"2","response":{"status_code":200,"request_id":"req-2","body":{"id":"chatcmpl-2","object":"chat.completion","choices":[{"index":0,"message":{"content":"Berlin","role":"assistant"},"finish_reason":"stop"}],"created":1772183793,"model":"ibm/granite-4-h-small","usage":{"completion_tokens":5,"prompt_tokens":30,"total_tokens":35}}},"processed_at":1772183795}
                            {"id":"batch-result-0","custom_id":"0","response":{"status_code":200,"request_id":"req-0","body":{"id":"chatcmpl-0","object":"chat.completion","choices":[{"index":0,"message":{"content":"Rome","role":"assistant"},"finish_reason":"stop"}],"created":1772183793,"model":"ibm/granite-4-h-small","usage":{"completion_tokens":5,"prompt_tokens":30,"total_tokens":35}}},"processed_at":1772183793}
                            {"id":"batch-result-1","custom_id":"1","response":{"status_code":200,"request_id":"req-1","body":{"id":"chatcmpl-1","object":"chat.completion","choices":[{"index":0,"message":{"content":"Paris","role":"assistant"},"finish_reason":"stop"}],"created":1772183793,"model":"ibm/granite-4-h-small","usage":{"completion_tokens":5,"prompt_tokens":30,"total_tokens":35}}},"processed_at":1772183794}
                        """)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        wireMock.stubFor(delete("/ml/v1/files/%s?version=%s".formatted(OUTPUT_FILE_ID, API_VERSION))
            .withHeader("X-IBM-Project-ID", equalTo(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(FILE_DELETE_RESPONSE)));

        var batchService = buildBatchService("X-IBM-Project-ID", PROJECT_ID, buildFileService("X-IBM-Project-ID", PROJECT_ID));

        var parameters = ChatParameters.builder()
            .modelId("ibm/granite-4-h-small")
            .temperature(0.0)
            .maxCompletionTokens(0)
            .build();

        var chatRequests =
            Stream.of("Italy", "French", "Germany")
                .map(city -> {
                    var messages = List.of(
                        SystemMessage.of("You are an helpful assistant"),
                        UserMessage.text("What is the capital of %s? Answer with only the name of the city.".formatted(city))
                    );
                    return ChatRequest.builder()
                        .parameters(parameters)
                        .messages(messages)
                        .build();
                })
                .toList();

        var results = batchService.submitChatRequestsAndFetch(chatRequests);
        waitForRequests(wireMock, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")), 2);

        assertEquals(3, results.size());
        assertEquals("0", results.get(0).customId());
        assertEquals("1", results.get(1).customId());
        assertEquals("2", results.get(2).customId());
        assertEquals(200, results.get(0).response().statusCode());
        assertEquals("Rome", results.get(0).response().body().toAssistantMessage().content());
        assertEquals(200, results.get(1).response().statusCode());
        assertEquals("Paris", results.get(1).response().body().toAssistantMessage().content());
        assertEquals(200, results.get(2).response().statusCode());
        assertEquals("Berlin", results.get(2).response().body().toAssistantMessage().content());
        wireMock.verify(2, deleteRequestedFor(urlPathMatching("/ml/v1/files/.*")));
    }

    @Test
    void should_throw_exception_when_a_batch_chat_responses_does_not_contain_model_id() {
        var batchService = buildBatchService("X-IBM-Project-ID", PROJECT_ID, buildFileService("X-IBM-Project-ID", PROJECT_ID));
        var ex = assertThrows(
            NullPointerException.class,
            () -> batchService.submitChatRequestsAndFetch(
                List.of(ChatRequest.builder().messages(UserMessage.text("Hello")).build()))
        );
        assertEquals("The modelId parameter is mandatory", ex.getMessage());
    }

    private FileService buildFileService(String headerKey, String headerValue) {
        var builder = FileService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl(BASE_URL);
        if (headerKey.equals("X-IBM-Project-ID"))
            builder.projectId(headerValue);
        else
            builder.spaceId(headerValue);
        return builder.build();
    }

    private BatchService buildBatchService(String headerKey, String headerValue, FileService fileService) {
        var builder = BatchService.builder()
            .authenticator(mockAuthenticator)
            .endpoint(ENDPOINT)
            .fileService(fileService)
            .baseUrl(BASE_URL);
        if (headerKey.equals("X-IBM-Project-ID"))
            builder.projectId(headerValue);
        else
            builder.spaceId(headerValue);
        return builder.build();
    }

    private void stubFileUpload(String headerKey, String headerValue) {
        stubFileUpload(headerKey, headerValue, FILE_UPLOAD_RESPONSE);
    }

    private void stubFileUpload(String headerKey, String headerValue, String response) {
        wireMock.stubFor(post("/ml/v1/files?version=%s".formatted(API_VERSION))
            .withHeader(headerKey, equalTo(headerValue))
            .withMultipartRequestBody(
                aMultipart()
                    .withName("purpose")
                    .withBody(equalTo("batch"))
            )
            .withMultipartRequestBody(
                aMultipart()
                    .withName("file")
                    .withHeader("Content-Type", containing("application/octet-stream"))
            )
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(response)));
    }

    @Nested
    class Cleanup {

        @Test
        void should_delete_uploaded_input_when_batch_submission_fails() throws Exception {
            assertInputCleanupAfterSubmissionFailure(true);
        }

        @Test
        void should_retain_uploaded_input_when_cleanup_disabled_and_batch_submission_fails() throws Exception {
            assertInputCleanupAfterSubmissionFailure(false);
        }

        private void assertInputCleanupAfterSubmissionFailure(boolean removeUploadedFile) throws Exception {
            when(mockAuthenticator.token()).thenReturn("token");
            when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
            wireMock.stubFor(post(urlPathEqualTo("/ml/v1/files"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":\"input\"}")));
            wireMock.stubFor(post(urlPathEqualTo("/ml/v1/batches"))
                .willReturn(aResponse().withStatus(400).withHeader("Content-Type", "text/plain").withBody("Batch creation rejected")));
            wireMock.stubFor(delete(urlPathEqualTo("/ml/v1/files/input"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":\"input\",\"deleted\":true}")));

            var files = FileService.builder().baseUrl(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .projectId("file-service-project").authenticator(mockAuthenticator).build();
            var service = BatchService.builder().baseUrl(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .projectId("batch-service-project").authenticator(mockAuthenticator)
                .endpoint("/v1/chat/completions").fileService(files).build();
            var parameters = BatchCreateRequest.builder().spaceId("request-space").transactionId("transaction")
                .removeUploadedFile(removeUploadedFile).build();

            var failure = assertThrows(WatsonxException.class, () -> service.submitAndFetch(
                new ByteArrayInputStream(new byte[0]), "input.jsonl", parameters, String.class));

            assertEquals(400, failure.statusCode());
            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/files"))
                .withHeader("X-IBM-Space-ID", equalTo("request-space"))
                .withHeader("X-IBM-Project-ID", absent())
                .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction")));
            if (removeUploadedFile)
                waitForRequests(wireMock, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/input")), 1);
            wireMock.verify(removeUploadedFile ? 1 : 0, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/input")));
            if (removeUploadedFile)
                wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/input"))
                    .withHeader("X-IBM-Space-ID", equalTo("request-space"))
                    .withHeader("X-IBM-Project-ID", absent())
                    .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction")));
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/batches/job/cancel")));
        }

        @Test
        void should_delete_output_when_completed_batch_arrives_after_deadline() throws Exception {
            assertOutputCleanupAfterLateCompletion(true);
        }

        @Test
        void should_retain_output_when_cleanup_disabled_and_completed_batch_arrives_after_deadline() throws Exception {
            assertOutputCleanupAfterLateCompletion(false);
        }

        private void assertOutputCleanupAfterLateCompletion(boolean removeOutputFile) throws Exception {
            var nanos = new AtomicLong();
            try (var polling = mockStatic(Polling.class)) {
                polling.when(() -> Polling.start(any(), any())).thenAnswer(invocation -> {
                    Duration budget = invocation.getArgument(0);
                    Supplier<RuntimeException> timeoutFailure = invocation.getArgument(1);
                    return new Polling<>(budget, timeoutFailure, nanos::get, nanos::addAndGet);
                });
                var timeout = Duration.ofMillis(500);
                when(mockAuthenticator.token()).thenReturn("token");
                when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockSecureHttpClient.<String>send(mockHttpRequest.capture(), any())).thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    var body = "{\"id\":\"job\",\"status\":\"in_progress\",\"input_file_id\":\"input\"}";
                    if (request.method().equals("GET")) {
                        nanos.addAndGet(timeout.toNanos());
                        body = "{\"id\":\"job\",\"status\":\"completed\",\"input_file_id\":\"input\",\"output_file_id\":\"output\"}";
                    }
                    when(mockHttpResponse.body()).thenReturn(body);
                    return mockHttpResponse;
                });
                when(mockSecureHttpClient.<String>sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
                var service = new BatchService[1];
                withWatsonxServiceMock(() -> {
                    var files = FileService.builder().baseUrl("https://api.example.com")
                        .projectId("file-service-project").authenticator(mockAuthenticator).build();
                    service[0] = BatchService.builder().baseUrl("https://api.example.com")
                        .projectId("batch-service-project").authenticator(mockAuthenticator)
                        .endpoint("/v1/chat/completions").fileService(files).build();
                });
                var request = BatchCreateRequest.builder().inputFileId("input")
                    .spaceId("request-space").transactionId("transaction")
                    .removeUploadedFile(true).removeOutputFile(removeOutputFile).timeout(timeout).build();

                var failure = assertThrows(RuntimeException.class, () -> service[0].submitAndFetch(request, String.class));

                assertEquals("The execution of the batch operation for the file \"input\" took longer than the timeout set by 500 milliseconds",
                    failure.getMessage());
                assertEquals(3, mockHttpRequest.getAllValues().size());
                assertEquals("/ml/v1/batches/job/cancel", mockHttpRequest.getValue().uri().getPath());
                var deletedRequests = ArgumentCaptor.forClass(HttpRequest.class);
                verify(mockSecureHttpClient, times(removeOutputFile ? 2 : 1)).sendAsync(deletedRequests.capture(), any());
                assertEquals("/ml/v1/files/input", deletedRequests.getAllValues().get(0).uri().getPath());
                if (removeOutputFile)
                    assertEquals("/ml/v1/files/output", deletedRequests.getAllValues().get(1).uri().getPath());
                for (var deletedRequest : deletedRequests.getAllValues()) {
                    assertEquals("request-space", deletedRequest.headers().firstValue("X-IBM-Space-ID").orElseThrow());
                    assertTrue(deletedRequest.headers().firstValue("X-IBM-Project-ID").isEmpty());
                    assertEquals("transaction", deletedRequest.headers().firstValue(TRANSACTION_ID_HEADER).orElseThrow());
                }
            }
        }

        @Test
        void should_delete_known_output_when_batch_fails() throws Exception {
            assertOutputCleanupAfterTerminalFailure("failed", true, "output");
        }

        @Test
        void should_retain_output_when_cleanup_disabled_and_batch_fails() throws Exception {
            assertOutputCleanupAfterTerminalFailure("failed", false, "output");
        }

        @Test
        void should_delete_only_input_when_expired_batch_has_no_output() throws Exception {
            assertOutputCleanupAfterTerminalFailure("expired", true, null);
        }

        private void assertOutputCleanupAfterTerminalFailure(String status, boolean removeOutputFile,
            String outputFileId) throws Exception {
            var files = mock(FileService.class);
            when(files.deleteAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
            var service = service(files);
            var batch = new LinkedHashMap<String, Object>(Map.of("id", "job", "status", status, "input_file_id", "input"));
            if (nonNull(outputFileId))
                batch.put("output_file_id", outputFileId);
            when(mockHttpResponse.body()).thenReturn(Json.toJson(batch));

            var failure = assertThrows(RuntimeException.class,
                () -> service.submitAndFetch(BatchCreateRequest.builder().inputFileId("input").removeOutputFile(removeOutputFile).build(),
                    String.class));

            assertTrue(failure.getMessage().startsWith("The batch operation did not complete successfully (status: " + status + ")"));
            var expectedFileIds = removeOutputFile && nonNull(outputFileId) ? List.of("input", outputFileId) : List.of("input");
            var deletedRequests = ArgumentCaptor.forClass(FileDeleteRequest.class);
            verify(files, times(expectedFileIds.size())).deleteAsync(deletedRequests.capture());
            assertEquals(expectedFileIds, deletedRequests.getAllValues().stream().map(FileDeleteRequest::fileId).toList());
        }

        @Test
        void should_cancel_and_delete_input_when_batch_poll_returns_json_null() throws Exception {
            when(mockAuthenticator.token()).thenReturn("token");
            when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
            wireMock.stubFor(post(urlPathEqualTo("/ml/v1/batches"))
                .willReturn(aResponse().withStatus(200).withBody("""
                    {"id":"job","status":"in_progress","input_file_id":"input"}
                    """)));
            wireMock.stubFor(get(urlPathEqualTo("/ml/v1/batches/job"))
                .willReturn(aResponse().withStatus(200).withBody("null")));
            wireMock.stubFor(post(urlPathEqualTo("/ml/v1/batches/job/cancel"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));
            wireMock.stubFor(delete(urlPathEqualTo("/ml/v1/files/input"))
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":\"input\",\"deleted\":true}")));

            var files = FileService.builder().baseUrl(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .projectId("file-service-project").authenticator(mockAuthenticator).build();
            var service = BatchService.builder().baseUrl(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .projectId("batch-service-project").authenticator(mockAuthenticator)
                .endpoint("/v1/chat/completions").fileService(files).build();
            var request = BatchCreateRequest.builder().inputFileId("input")
                .spaceId("request-space").transactionId("transaction")
                .removeUploadedFile(true).timeout(Duration.ofSeconds(5)).build();

            var failure = assertThrows(NullPointerException.class, () -> service.submitAndFetch(request, String.class));

            assertEquals("The batch details response cannot be null", failure.getMessage());
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/batches/job"))
                .withHeader("X-IBM-Space-ID", equalTo("request-space"))
                .withHeader("X-IBM-Project-ID", absent())
                .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction")));
            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/batches/job/cancel"))
                .withHeader("X-IBM-Space-ID", equalTo("request-space"))
                .withHeader("X-IBM-Project-ID", absent())
                .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction")));
            waitForRequests(wireMock, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/input")), 1);
            wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/files/input"))
                .withHeader("X-IBM-Space-ID", equalTo("request-space"))
                .withHeader("X-IBM-Project-ID", absent())
                .withHeader(TRANSACTION_ID_HEADER, equalTo("transaction")));
        }

        @Test
        void should_attempt_both_file_deletions_when_output_is_invalid() throws Exception {
            var files = mock(FileService.class);
            when(files.retrieve(any(FileRetrieveRequest.class))).thenReturn("invalid JSON");
            when(files.deleteAsync(any())).thenThrow(new IllegalStateException("Cannot start cleanup"));
            var service = service(files);

            assertThrows(RuntimeException.class, () -> service.submitAndFetch(request(), String.class));

            verify(files, times(2)).deleteAsync(any());
        }

        @Test
        void should_preserve_success_when_file_cleanup_cannot_start() throws Exception {
            var files = mock(FileService.class);
            when(files.retrieve(any(FileRetrieveRequest.class))).thenReturn("");
            when(files.deleteAsync(any())).thenThrow(new IllegalStateException("Cannot start cleanup"));

            assertEquals(List.of(), service(files).submitAndFetch(request(), String.class));

            verify(files, times(2)).deleteAsync(any());
        }

        @Test
        void should_clean_both_files_after_output_retrieval_fails() throws Exception {
            var files = mock(FileService.class);
            var failure = new IllegalStateException("Cannot retrieve output");
            when(files.retrieve(any(FileRetrieveRequest.class))).thenThrow(failure);
            when(files.deleteAsync(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Cleanup failed")));
            var service = service(files);

            assertEquals(failure, assertThrows(IllegalStateException.class, () -> service.submitAndFetch(request(), String.class)));

            verify(files, times(2)).deleteAsync(any());
        }

        private BatchService service(FileService files) throws Exception {
            when(mockAuthenticator.token()).thenReturn("token");
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn("""
                {"id":"job","status":"completed","input_file_id":"input","output_file_id":"output"}
                """);
            when(mockSecureHttpClient.<String>send(any(), any())).thenReturn(mockHttpResponse);
            var service = new BatchService[1];
            withWatsonxServiceMock(() -> service[0] = BatchService.builder()
                .baseUrl("https://api.example.com").projectId("project").authenticator(mockAuthenticator)
                .endpoint("/v1/chat/completions").fileService(files).build());
            return service[0];
        }

        private BatchCreateRequest request() {
            return BatchCreateRequest.builder().inputFileId("input").build();
        }
    }

    @Nested
    class Scope {

        @Test
        void should_use_batch_project_for_upload_output_and_cleanup() throws Exception {
            assertBatchScope(false);
        }

        @Test
        void should_use_batch_space_for_upload_output_and_cleanup() throws Exception {
            assertBatchScope(true);
        }

        private void assertBatchScope(boolean useSpace) throws Exception {
            var requests = new CopyOnWriteArrayList<HttpRequest>();
            var deletions = new CountDownLatch(2);
            when(mockAuthenticator.token()).thenReturn("token");
            when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockSecureHttpClient.send(any(), any())).thenAnswer(invocation -> {
                HttpRequest request = invocation.getArgument(0);
                requests.add(request);
                var path = request.uri().getPath();
                var body = path.endsWith("/batches")
                    ? "{\"id\":\"job\",\"status\":\"completed\",\"input_file_id\":\"input\",\"output_file_id\":\"output\"}"
                    : path.endsWith("/files") ? "{\"id\":\"input\"}" : "";
                when(mockHttpResponse.body()).thenReturn(body);
                return mockHttpResponse;
            });
            when(mockSecureHttpClient.sendAsync(any(), any())).thenAnswer(invocation -> {
                requests.add(invocation.getArgument(0));
                when(mockHttpResponse.body()).thenReturn("{\"id\":\"deleted\",\"deleted\":true}");
                deletions.countDown();
                return CompletableFuture.completedFuture(mockHttpResponse);
            });
            var service = new BatchService[1];
            withWatsonxServiceMock(() -> {
                var files = FileService.builder().baseUrl("https://api.example.com")
                    .projectId("file-service-project").authenticator(mockAuthenticator).build();
                service[0] = BatchService.builder().baseUrl("https://api.example.com")
                    .projectId("batch-service-project").authenticator(mockAuthenticator)
                    .endpoint("/v1/chat/completions").fileService(files).build();
            });
            var parameters = BatchCreateRequest.builder().transactionId("transaction");
            if (useSpace)
                parameters.spaceId("override");
            else
                parameters.projectId("override");

            service[0].submitAndFetch(new ByteArrayInputStream(new byte[0]), "batch.jsonl", parameters.build(), String.class);

            assertTrue(deletions.await(5, TimeUnit.SECONDS));
            assertEquals(5, requests.size());
            for (var request : requests) {
                assertEquals("override", request.headers().firstValue(useSpace ? "X-IBM-Space-ID" : "X-IBM-Project-ID").orElseThrow());
                assertTrue(request.headers().firstValue(useSpace ? "X-IBM-Project-ID" : "X-IBM-Space-ID").isEmpty());
                assertEquals("transaction", request.headers().firstValue(TRANSACTION_ID_HEADER).orElseThrow());
            }
        }

        @Test
        void should_apply_configured_http_timeout_to_batch_requests() {
            when(mockAuthenticator.token()).thenReturn("token");
            wireMock.stubFor(get(urlPathEqualTo("/ml/v1/batches/job"))
                .willReturn(aResponse().withFixedDelay(500).withBody("{\"id\":\"job\",\"status\":\"completed\"}")));
            var files = FileService.builder().baseUrl(BASE_URL).projectId("project").authenticator(mockAuthenticator).build();
            var service = BatchService.builder().baseUrl(BASE_URL).projectId("project").authenticator(mockAuthenticator)
                .endpoint("/v1/chat/completions").fileService(files).timeout(Duration.ofMillis(100)).build();

            var error = assertThrows(RuntimeException.class, () -> service.retrieve("job"));

            assertInstanceOf(HttpTimeoutException.class, error.getCause());
        }
    }
}
