/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.file.FileService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
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
    void should_submit_and_fetch_results_when_job_completes_immediately() {

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

        assertEquals(3, results.size());
        assertEquals(200, results.get(0).response().statusCode());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_submit_and_fetch_results_after_polling() {

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

        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_submit_and_fetch_results_via_path() {

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

        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_submit_and_fetch_results_via_path_with_parameters() {

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

        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_submit_and_fetch_results_via_file() {

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

        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_submit_and_fetch_results_via_file_with_parameters() {

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

        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_submit_and_fetch_results_via_input_stream() {

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

        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_submit_and_fetch_results_via_input_stream_with_parameters() {

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

        assertEquals(3, results.size());
        assertEquals("The capital of Italy is Rome.", results.get(0).response().body().toAssistantMessage().content());
    }

    @Test
    void should_throw_exception_when_batch_job_fails() {

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

        assertEquals(true, ex.getMessage().startsWith("The batch operation failed:"));
    }

    @Test
    void should_throw_exception_when_timeout_is_exceeded() {

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
    }

    @Test
    void should_throw_exception_when_service_level_timeout_is_exceeded() {

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

        // No timeout set on the request — service-level timeout (200ms) should apply.
        // The first sleep in the polling loop is 100ms, so after one poll the timeout will be exceeded.
        var ex = assertThrows(RuntimeException.class, () -> batchService.submitAndFetch(
            BatchCreateRequest.builder()
                .inputFileId(FILE_ID)
                .build(),
            ChatResponse.class));

        assertEquals(true, ex.getMessage().startsWith(
            "The execution of the batch operation for the file \"%s\" took longer than the timeout".formatted(FILE_ID)));
    }

    @Test
    void should_use_request_timeout_over_service_level_timeout() {

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
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_submit_and_fetch_results_with_overridden_parameters() {

        var OUTPUT_CONTENT = assertDoesNotThrow(() -> Files.readString(Path.of(ClassLoader.getSystemResource("file_retrive.jsonl").toURI())));

        withWatsonxServiceMock(() -> {

            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.body()).thenReturn(SUBMIT_RESPONSE);
            when(mockFileService.retrieve(OUTPUT_FILE_ID)).thenReturn(OUTPUT_CONTENT);

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
            when(mockFileService.retrieve(OUTPUT_FILE_ID)).thenReturn(OUTPUT_CONTENT);

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

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
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

        stubFileUpload("X-IBM-Project-ID", PROJECT_ID);
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
    void should_batch_chat_responses() {

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
        assertDoesNotThrow(() -> Thread.sleep(200)); // Wait for async files deletion to complete

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
}
