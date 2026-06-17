/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.textprocessing.CosDataConnection;
import com.ibm.watsonx.ai.textprocessing.CosDataLocation;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.DataReference;
import com.ibm.watsonx.ai.textprocessing.GroundingHints;
import com.ibm.watsonx.ai.textprocessing.GroundingHints.FieldData;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.Language;
import com.ibm.watsonx.ai.textprocessing.Metadata;
import com.ibm.watsonx.ai.textprocessing.Mode;
import com.ibm.watsonx.ai.textprocessing.OcrMode;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaResponse.CreateSchemaResult;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaResponse.Entity;

@ExtendWith(MockitoExtension.class)
public class CreateSchemaTest extends AbstractWatsonxTest {

    @RegisterExtension
    WireMockExtension cosServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort().http2PlainDisabled(true))
        .build();

    @RegisterExtension
    WireMockExtension watsonxServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

    CreateSchemaService createSchemaService;

    @BeforeEach
    void beforeEach() {
        cosServer.resetAll();
        watsonxServer.resetAll();
        when(mockAuthenticator.token()).thenReturn("token");
        createSchemaService = CreateSchemaService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference("connection_id", "my-bucket")
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    @Test
    void should_build_create_schema_parameters_and_start_creation() throws Exception {

        var PARAMETERS = """
            "parameters": {
                    "mode": "high_quality",
                    "ocr_mode": "forced",
                    "auto_rotation_correction": true,
                    "languages": [
                        "en"
                    ],
                    "additional_prompt_instructions": "Test",
                    "enable_grounding": true,
                    "max_pages_to_process": 3,
                    "semantic_config": {
                        "default_model_name": "test"
                    }
            }""";

        var RESULT =
            """
                    {
                            "entity": {
                                "document_reference": {
                                    "connection": {
                                        "id": "connection-id"
                                    },
                                    "location": {
                                        "file_name": "test.pdf",
                                        "bucket": "my-bucket"
                                    },
                                    "type": "connection_asset"
                                },
                                %s,
                                "results": {
                                    "completed_at": "2026-06-15T08:24:44.987Z",
                                    "grounding_hints" : {
                                        "fields" : {
                                            "bank_bic" : {
                                                "normalized_bbox" : [ 0.5803, 0.8346, 0.6783, 0.8441 ],
                                                "page_number" : 1
                                            },
                                            "bank_iban" : {
                                                "normalized_bbox" : [ 0.1111, 0.8222, 0.333, 0.41 ],
                                                "page_number" : 2
                                            }
                                        }
                                    },
                                    "number_pages_processed": 2,
                                    "running_at": "2026-06-15T08:24:01.677Z",
                                    "schema": {
                                        "document_description": "Test",
                                        "document_type": "Invoice",
                                        "fields": {
                                            "bank_bic": {
                                                "description": "bank bic",
                                                "example": "000"
                                            },
                                            "bank_iban": {
                                                "description": "bank iban",
                                                "example": "AAA"
                                            }
                                        }
                                    },
                                    "status": "completed",
                                    "total_pages": 2
                                }
                            },
                            "metadata": {
                                "created_at": "2026-06-15T08:23:58.819Z",
                                "id": "id",
                                "modified_at": "2026-06-15T08:24:45.043Z",
                                "project_id": "project-id",
                                "space_id": "space-id"
                            }
                        }
                }""".formatted(PARAMETERS);

        Schema schema = Schema.builder()
            .documentType("Invoice")
            .documentDescription("Test")
            .fields(
                KvpFields.builder()
                    .add("bank_bic", KvpField.of("bank bic", "000"))
                    .add("bank_iban", KvpField.of("bank iban", "AAA"))
                    .build())
            .build();

        GroundingHints groundingHints = GroundingHints.builder()
            .add("bank_bic", FieldData.of(List.of(0.5803, 0.8346, 0.6783, 0.8441), 1))
            .add("bank_iban", FieldData.of(List.of(0.1111, 0.8222, 0.333, 0.41), 2))
            .build();

        Metadata metadata = new Metadata("id", "2026-06-15T08:23:58.819Z", "2026-06-15T08:24:45.043Z", "space-id", "project-id");
        DataReference documentReference =
            new DataReference("connection_asset", new CosDataConnection("connection-id"), new CosDataLocation("test.pdf", "my-bucket"));
        CreateSchemaResult createSchemaResult =
            new CreateSchemaResult("completed", "2026-06-15T08:24:01.677Z", "2026-06-15T08:24:44.987Z", 2, 2, schema, groundingHints, null);

        CreateSchemaParameters p = CreateSchemaParameters.builder()
            .autoRotationCorrection(true)
            .documentReference(CosReference.of("connection-id", "my-bucket"))
            .additionalPromptInstructions("Test")
            .enableGrounding(true)
            .maxPagesToProcess(3)
            .languages(Language.ENGLISH)
            .mode(Mode.HIGH_QUALITY)
            .ocrMode(OcrMode.FORCED)
            .projectId("project-id")
            .spaceId("space-id")
            .semanticConfig(CreateSchemaSemanticConfig.builder().defaultModelName("test").build())
            .transactionId("transaction-id")
            .build();

        Parameters parameters = p.toParameters();
        assertEquals(p.mode(), parameters.mode());
        assertEquals(p.ocrMode(), parameters.ocrMode());
        assertEquals(p.autoRotationCorrection(), parameters.autoRotationCorrection());
        assertEquals(p.languages(), parameters.languages());
        assertEquals(p.additionalPromptInstructions(), parameters.additionalPromptInstructions());
        assertEquals(p.enableGrounding(), parameters.enableGrounding());
        assertEquals(p.maxPagesToProcess(), parameters.maxPagesToProcess());
        assertEquals(p.semanticConfig().defaultModelName(), parameters.semanticConfig().defaultModelName());

        Entity entity = new Entity(documentReference, createSchemaResult, parameters);
        CreateSchemaResponse response = new CreateSchemaResponse(metadata, entity);
        JSONAssert.assertEquals(RESULT, toJson(response), true);

        watsonxServer.stubFor(post("/ml/v1/text/schemas/create?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "project-id",
                    "space_id": "space-id",
                    "document_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "connection-id"
                        },
                        "location": {
                            "file_name": "test.pdf",
                            "bucket": "my-bucket"
                        }
                    },
                    %s
                }""".formatted(PARAMETERS)))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESULT)
            ));

        var result = createSchemaService.startCreateSchema("test.pdf", p);
        assertEquals(metadata, result.metadata());
        assertEquals(entity, result.entity());
    }

    @Test
    void should_start_create_schema() throws Exception {

        var RESULT = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/create?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "project-id",
                    "document_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "connection_id"
                        },
                        "location": {
                            "file_name": "test.pdf",
                            "bucket": "my-bucket"
                        }
                    },
                    "parameters": {
                        "ocr_mode": "disabled"
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESULT)
            ));

        var result = createSchemaService.startCreateSchema("test.pdf");
        assertNotNull(result);
    }

    @Test
    void should_fetch_create_schema_request() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_job.json").toURI()));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s"
            .formatted(API_VERSION, URLEncoder.encode("project-id", Charset.defaultCharset())))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted(Status.SUBMITTED.value()))
            ));

        var response = createSchemaService.fetchRequest("id");
        JSONAssert.assertEquals(JOB.formatted(Status.SUBMITTED.value()), Json.toJson(response), true);

        var projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());

        watsonxServer.resetAll();

        watsonxServer
            .stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")
                ));

        var parameters = CreateSchemaFetchParameters.builder()
            .projectId("new-project-id")
            .transactionId("my-transaction-id")
            .build();

        response = createSchemaService.fetchRequest("id", parameters);
        assertNotNull(response);

        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

        watsonxServer.resetAll();

        watsonxServer
            .stubFor(get("/ml/v1/text/schemas/create/id?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")
                ));

        parameters = CreateSchemaFetchParameters.builder()
            .spaceId("new-space-id")
            .transactionId("my-transaction-id")
            .build();

        response = createSchemaService.fetchRequest("id", parameters);
        assertNotNull(response);
    }

    @Test
    void should_delete_create_schema_request() {

        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(delete("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        assertTrue(createSchemaService.deleteRequest("id"));

        watsonxServer
            .stubFor(delete("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withBody("""
                        {
                            "trace": "db2821f494a629c614616e458c85de36",
                            "errors": [
                                {
                                    "code": "text_classification_event_does_not_exist",
                                    "message": "Text classification request does not exist."
                                }
                            ]
                        }""")
                ));

        assertFalse(createSchemaService.deleteRequest("id"));

        projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(
                delete("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, projectId)
                    + "&hard_delete=true")
                    .withHeader("Authorization", equalTo("Bearer token"))
                    .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                    .willReturn(aResponse()
                        .withStatus(204)
                    ));

        var parameters = CreateSchemaDeleteParameters.builder()
            .projectId("new-project-id")
            .hardDelete(true)
            .transactionId("my-transaction-id")
            .build();

        assertTrue(createSchemaService.deleteRequest("id", parameters));

        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(delete("/ml/v1/text/schemas/create/id?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        parameters = CreateSchemaDeleteParameters.builder()
            .spaceId("new-space-id")
            .build();

        assertTrue(createSchemaService.deleteRequest("id", parameters));
    }

    @Test
    void should_create_schema_and_fetch_result_for_existing_file() throws Exception {

        mockServers(false);
        CreateSchemaResult result = createSchemaService.createSchemaAndFetch("test.pdf");
        assertEquals("completed", result.status());
        assertEquals("2026-06-15T13:31:27.466Z", result.runningAt());
        assertEquals("2026-06-15T13:32:10.844Z", result.completedAt());
        assertEquals(2, result.numberPagesProcessed());
        assertNotNull(result.schema());
        assertNotNull(result.schema().documentDescription());
        assertNotNull(result.schema().documentType());
        assertEquals(
            List.of(0.0735, 0.3092, 0.1916, 0.3228),
            result.groundingHints().bbox("arrival_station")
        );
        assertEquals(
            2,
            result.groundingHints().pageNumber("arrival_station")
        );
        assertEquals(
            FieldData.of(List.of(0.0735, 0.3092, 0.1916, 0.3228), 2),
            result.groundingHints().field("arrival_station")
        );
        assertEquals(
            KvpField.of("The station where the train arrives.", "MI CENTRALE"),
            result.schema().fields().get("arrival_station")
        );

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_create_schema_and_fetch_using_input_stream() throws Exception {

        mockServers(false);
        var inputStream = ClassLoader.getSystemResourceAsStream("test.pdf");
        CreateSchemaResult result = createSchemaService.uploadCreateSchemaAndFetch(inputStream, "test.pdf");
        assertEquals("completed", result.status());
        assertEquals("2026-06-15T13:31:27.466Z", result.runningAt());
        assertEquals("2026-06-15T13:32:10.844Z", result.completedAt());
        assertEquals(2, result.numberPagesProcessed());
        assertNotNull(result.schema());
        assertNotNull(result.schema().documentDescription());
        assertNotNull(result.schema().documentType());
        assertEquals(
            List.of(0.0735, 0.3092, 0.1916, 0.3228),
            result.groundingHints().bbox("arrival_station")
        );
        assertEquals(
            2,
            result.groundingHints().pageNumber("arrival_station")
        );
        assertEquals(
            FieldData.of(List.of(0.0735, 0.3092, 0.1916, 0.3228), 2),
            result.groundingHints().field("arrival_station")
        );
        assertEquals(
            KvpField.of("The station where the train arrives.", "MI CENTRALE"),
            result.schema().fields().get("arrival_station")
        );

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_create_schema_and_fetch_using_file() throws Exception {

        mockServers(false);
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        CreateSchemaResult result = createSchemaService.uploadCreateSchemaAndFetch(file);
        assertEquals("completed", result.status());
        assertEquals("2026-06-15T13:31:27.466Z", result.runningAt());
        assertEquals("2026-06-15T13:32:10.844Z", result.completedAt());
        assertEquals(2, result.numberPagesProcessed());
        assertNotNull(result.schema());
        assertNotNull(result.schema().documentDescription());
        assertNotNull(result.schema().documentType());
        assertEquals(
            List.of(0.0735, 0.3092, 0.1916, 0.3228),
            result.groundingHints().bbox("arrival_station")
        );
        assertEquals(
            2,
            result.groundingHints().pageNumber("arrival_station")
        );
        assertEquals(
            FieldData.of(List.of(0.0735, 0.3092, 0.1916, 0.3228), 2),
            result.groundingHints().field("arrival_station")
        );
        assertEquals(
            KvpField.of("The station where the train arrives.", "MI CENTRALE"),
            result.schema().fields().get("arrival_station")
        );

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_uploading_non_existent_file() throws Exception {

        mockServers(false);
        var file = new File("doesnotexist.pdf");

        CreateSchemaException ex = assertThrows(CreateSchemaException.class,
            () -> createSchemaService.uploadCreateSchemaAndFetch(file));
        assertEquals(ex.code(), "file_not_found");
        assertTrue(ex.getCause() instanceof FileNotFoundException);
        assertEquals("CreateSchemaException [code=file_not_found, message=doesnotexist.pdf (No such file or directory)]", ex.toString());

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_upload_file_and_start_create_schema_using_file() throws Exception {

        mockServers(false);
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        var result = createSchemaService.uploadAndStartCreateSchema(file);
        assertEquals("id", result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_uploading_and_starting_with_non_existent_file() throws Exception {

        mockServers(false);
        var file = new File("doesnotexist.pdf");

        CreateSchemaException ex = assertThrows(CreateSchemaException.class,
            () -> createSchemaService.uploadAndStartCreateSchema(file));
        assertEquals(ex.code(), "file_not_found");
        assertTrue(ex.getCause() instanceof FileNotFoundException);

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_upload_and_create_schema_using_input_stream() throws Exception {

        mockServers(false);
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("test.pdf");

        var result = createSchemaService.uploadAndStartCreateSchema(inputStream, "test.pdf");
        assertEquals("id", result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_remove_uploaded_file_after_create_schema() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        mockServers(true);
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        CreateSchemaParameters parameters = CreateSchemaParameters.builder()
            .removeUploadedFile(true)
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> createSchemaService.startCreateSchema("test.pdf", parameters));

        assertThrows(
            IllegalArgumentException.class,
            () -> createSchemaService.uploadAndStartCreateSchema(file, parameters));

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));

        CreateSchemaResult result = createSchemaService.createSchemaAndFetch("test.pdf", parameters);
        assertNotNull(result);
        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));

        watsonxServer.resetAll();
        cosServer.resetAll();

        mockServers(true);

        result = createSchemaService.uploadCreateSchemaAndFetch(file, parameters);
        assertNotNull(result);
        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_handle_long_running_create_schema_with_retries() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/create?version=%s".formatted(API_VERSION))
            .inScenario("long_response")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("firstIteration")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("firstIteration")
            .willSetStateTo("secondIteration")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("secondIteration")
            .willSetStateTo(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)
            ));


        var result = createSchemaService.createSchemaAndFetch("test.pdf");
        assertNotNull(result);
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(2, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
    }

    @Test
    void should_throw_exception_when_create_schema_timeout_exceeded() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_job.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/create?version=%s".formatted(API_VERSION))
            .inScenario("long_response")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("firstIteration")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("firstIteration")
            .willSetStateTo("secondIteration")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("secondIteration")
            .willSetStateTo(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("completed"))
            ));

        watsonxServer
            .stubFor(delete("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        CreateSchemaParameters parameters = CreateSchemaParameters.builder()
            .timeout(Duration.ofMillis(100))
            .removeUploadedFile(true)
            .build();

        var ex = assertThrows(
            CreateSchemaException.class,
            () -> createSchemaService.createSchemaAndFetch("test.pdf", parameters));

        assertEquals("Execution to create schema for test.pdf file took longer than the timeout set by 100 milliseconds",
            ex.getMessage());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
    }

    @Test
    void should_throw_exception_when_create_schema_job_fails() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_job.json").toURI()));
        var JOB_ERROR = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_job_error.json").toURI()));
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        CreateSchemaParameters parameters = CreateSchemaParameters.builder()
            .removeUploadedFile(true)
            .build();

        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/create?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("simulate_failed")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("failed")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("simulate_failed")
            .whenScenarioStateIs("failed")
            .willSetStateTo(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB_ERROR)
            ));

        var ex = assertThrows(
            CreateSchemaException.class,
            () -> createSchemaService.createSchemaAndFetch("test.pdf", parameters));

        assertEquals(ex.code(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        ex = assertThrows(
            CreateSchemaException.class,
            () -> createSchemaService.uploadCreateSchemaAndFetch(file, parameters));

        assertEquals(ex.code(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(2, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(4, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(2, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_throw_exception_when_bucket_does_not_exist() throws Exception {

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/xml")
                .withBody("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Error>
                        <Code>NoSuchBucket</Code>
                        <Message>The specified bucket does not exist.</Message>
                        <Resource>/my-bucket-name/test.pdf</Resource>
                        <RequestId>my-request-id</RequestId>
                        <httpStatusCode>404</httpStatusCode>
                    </Error>""")));


        var detail = new com.ibm.watsonx.ai.core.exception.model.WatsonxError.Error("NoSuchBucket", "The specified bucket does not exist.",
            "/my-bucket-name/test.pdf");
        WatsonxError error = new WatsonxError(404, "my-request-id", List.of(detail));

        var ex = assertThrows(
            WatsonxException.class,
            () -> createSchemaService.uploadAndStartCreateSchema(file),
            "The specified bucket does not exist.");

        assertEquals(error, ex.details().orElseThrow());

        ex = assertThrows(
            WatsonxException.class,
            () -> createSchemaService.uploadCreateSchemaAndFetch(file),
            "The specified bucket does not exist.");

        assertEquals(error, ex.details().orElseThrow());

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/create/id")));
        cosServer.verify(2, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_throw_exception_when_create_schema_event_not_found() {

        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                            "error": "Schema 'c6ba78bc-1a45-4cf6-827f-694d4c38f2fbs' not found"
                        }
                    """)
            ));

        var ex = assertThrows(WatsonxException.class,
            () -> createSchemaService.fetchRequest("id"));
        assertEquals(WatsonxError.Code.CREATE_SCHEMA_EVENT_DOES_NOT_EXIST.value(),
            ex.details().orElseThrow().errors().get(0).code());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_delete_file() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        cosServer.resetAll();

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("retry")
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/xml")
                .withBody("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Error>
                        <Code>AccessDenied</Code>
                        <Message>Access Denied</Message>
                        <Resource>/andreaproject-donotdelete-pr-xnran4g4ptd1wo/ciao.pdf</Resource>
                        <RequestId>df887c2b-43c3-4933-a3a1-b0e19e7c2231</RequestId>
                        <httpStatusCode>403</httpStatusCode>
                    </Error>""")));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .inScenario("retry")
            .whenScenarioStateIs("retry")
            .willSetStateTo(Scenario.STARTED)
            .willReturn(aResponse().withStatus(204)));

        assertTrue(createSchemaService.deleteFile("my-bucket", "test.pdf"));
        Thread.sleep(500);
        cosServer.verify(2, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_delete_file_with_custom_api_key() throws Exception {

        var cosAuthenticator = mock(Authenticator.class);
        when(cosAuthenticator.token()).thenReturn("custom-token");
        when(cosAuthenticator.scheme()).thenReturn("Bearer");
        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        cosServer.resetAll();

        var createSchemaService = CreateSchemaService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticator(mockAuthenticator)
            .cosAuthenticator(cosAuthenticator)
            .projectId("projectid")
            .documentReference("connection_id", "my-bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer custom-token"))
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("retry")
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/xml")
                .withBody("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Error>
                        <Code>AccessDenied</Code>
                        <Message>Access Denied</Message>
                        <Resource>/andreaproject-donotdelete-pr-xnran4g4ptd1wo/ciao.pdf</Resource>
                        <RequestId>df887c2b-43c3-4933-a3a1-b0e19e7c2231</RequestId>
                        <httpStatusCode>403</httpStatusCode>
                    </Error>""")));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer custom-token"))
            .inScenario("retry")
            .whenScenarioStateIs("retry")
            .willSetStateTo(Scenario.STARTED)
            .willReturn(aResponse().withStatus(204)));

        assertTrue(createSchemaService.deleteFile("transactionId", "my-bucket", "test.pdf"));
        Thread.sleep(500);
        cosServer.verify(2, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_upload_file() throws Exception {

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(createSchemaService.uploadFile(file));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_upload_file_with_different_api_key() throws Exception {

        var cosAuthenticator = mock(Authenticator.class);
        when(cosAuthenticator.token()).thenReturn("custom-token");
        when(cosAuthenticator.scheme()).thenReturn("Bearer");

        var createSchemaService = CreateSchemaService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticator(mockAuthenticator)
            .cosAuthenticator(cosAuthenticator)
            .projectId("projectid")
            .documentReference("connection_id", "my-bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer custom-token"))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(createSchemaService.uploadFile(file));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_uploading_a_non_existent_file() throws Exception {

        var file = new File("doesnotexist.pdf");
        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        CreateSchemaException ex = assertThrows(CreateSchemaException.class,
            () -> createSchemaService.uploadFile(file));
        assertEquals(ex.code(), "file_not_found");
        assertTrue(ex.getCause() instanceof FileNotFoundException);
    }

    private void mockServers(boolean deleteUploadedFile) throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("create_schema_response.json").toURI()));
        var BUCKET_NAME = "my-bucket";
        var FILE_NAME = "test.pdf";

        // Mock the upload local file operation.
        cosServer.stubFor(put("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));


        if (deleteUploadedFile) {
            // Mock delete uploaded file.
            cosServer.stubFor(delete("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse().withStatus(200)));
        }

        // Mock start extraction.
        watsonxServer.stubFor(post("/ml/v1/text/schemas/create?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("submitted"))
            ));

        // Mock result extraction.
        watsonxServer.stubFor(get("/ml/v1/text/schemas/create/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)
            ));
    }
}
