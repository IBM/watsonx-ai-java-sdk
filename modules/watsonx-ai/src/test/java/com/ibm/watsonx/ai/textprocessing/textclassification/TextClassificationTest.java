/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
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
import com.ibm.watsonx.ai.project.Project;
import com.ibm.watsonx.ai.project.ProjectService;
import com.ibm.watsonx.ai.project.ProjectStorage;
import com.ibm.watsonx.ai.project.ProjectStorageProperties;
import com.ibm.watsonx.ai.textprocessing.ContainerReference;
import com.ibm.watsonx.ai.textprocessing.CosDataConnection;
import com.ibm.watsonx.ai.textprocessing.CosDataLocation;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.DataReference;
import com.ibm.watsonx.ai.textprocessing.ExtendedSemanticConfig.GroundingMode;
import com.ibm.watsonx.ai.textprocessing.ExtendedSemanticConfig.SchemaMergeStrategy;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.KvpPage;
import com.ibm.watsonx.ai.textprocessing.KvpSlice;
import com.ibm.watsonx.ai.textprocessing.Language;
import com.ibm.watsonx.ai.textprocessing.Metadata;
import com.ibm.watsonx.ai.textprocessing.OcrMode;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationParameters.ClassificationMode;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationResponse.ClassificationResult;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationResponse.Entity;

@ExtendWith(MockitoExtension.class)
@DisabledInNativeImage
public class TextClassificationTest extends AbstractWatsonxTest {

    @RegisterExtension
    WireMockExtension cosServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort().http2PlainDisabled(true))
        .build();

    @RegisterExtension
    WireMockExtension watsonxServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

    TextClassificationService classificationService;

    @BeforeEach
    void beforeEach() {
        cosServer.resetAll();
        watsonxServer.resetAll();
        when(mockAuthenticator.token()).thenReturn("token");
        classificationService = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(CosReference.of("connection_id", "my-bucket"))
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    @Test
    void should_build_text_classification_parameters_and_start_classification() throws Exception {

        var PARAMETERS = """
            "parameters": {
                    "ocr_mode": "forced",
                    "classification_mode": "exact",
                    "auto_rotation_correction": true,
                    "languages": [
                        "en"
                    ],
                    "semantic_config": {
                        "enable_text_hints": true,
                        "enable_schema_kvp": true,
                        "grounding_mode": "fast",
                        "schemas_merge_strategy": "replace",
                        "force_schema_name": "None",
                        "default_model_name": "defaultModelName",
                        "schemas": [
                            {
                                "document_description": "A vendor-issued invoice listing purchased items, prices, and payment information.",
                                "document_type": "Invoice",
                                "fields": {
                                    "invoice_date": {
                                        "description": "The date when the invoice was issued.",
                                        "example": "2024-07-10"
                                    },
                                    "invoice_number": {
                                        "description": "The unique number identifying the invoice.",
                                        "example": "INV-2024-001"
                                    },
                                    "total_amount": {
                                        "description": "The total amount to be paid.",
                                        "example": "1250.50"
                                    }
                                },
                                "pages": {
                                    "page_description": "Invoice page",
                                    "slices": [
                                        {
                                            "fields": {
                                                "invoice_date": {
                                                    "description": "The date when the invoice was issued.",
                                                    "example": "2024-07-10"
                                                },
                                                "invoice_number": {
                                                    "description": "The unique number identifying the invoice.",
                                                    "example": "INV-2024-001"
                                                },
                                                "total_amount": {
                                                    "description": "The total amount to be paid.",
                                                    "example": "1250.50"
                                                }
                                            },
                                            "normalized_bbox": [ 0.0, 0.0, 1.0, 1.0 ]
                                        }
                                    ]
                                },
                                "additional_prompt_instructions": "additional instructions"
                            },
                            {
                                "document_description": "A legal document outlining terms and conditions between two parties.",
                                "document_type": "Contract"
                            }
                        ],
                        "task_model_name_override": {
                            "test": "test"
                        }
                    }
            }""";

        var RESULT = """
            {
                "metadata": {
                    "created_at": "2025-10-23T07:32:11.013Z",
                    "id": "id",
                    "modified_at": "2025-10-23T07:32:43.003Z",
                    "project_id": "project-id",
                    "space_id": "space-id"
                },
                "entity": {
                    "document_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "connection-id"
                        },
                        "location": {
                            "bucket": "my-bucket",
                            "file_name": "test.pdf"
                        }
                    },
                    "results": {
                        "completed_at": "2025-10-23T07:32:42.981Z",
                        "document_classified": true,
                        "document_type": "Invoice",
                        "running_at": "2025-10-23T07:32:24.272Z",
                        "status": "completed"
                    },
                    %s,
                    "custom": {
                        "custom_1": "custom_value_1",
                        "custom_2": "custom_value_2"
                    }
                }
            }""".formatted(PARAMETERS);

        Metadata metadata = new Metadata("id", "2025-10-23T07:32:11.013Z", "2025-10-23T07:32:43.003Z", "space-id", "project-id");
        DataReference documentReference =
            new DataReference("connection_asset", new CosDataConnection("connection-id"), new CosDataLocation("test.pdf", "my-bucket", null));
        ClassificationResult classificationResult = new ClassificationResult(
            "completed",
            "2025-10-23T07:32:24.272Z",
            "2025-10-23T07:32:42.981Z",
            true, "Invoice", null);

        KvpFields fields = KvpFields.builder()
            .add("invoice_date", KvpField.of("The date when the invoice was issued.", "2024-07-10"))
            .add("invoice_number", KvpField.of("The unique number identifying the invoice.", "INV-2024-001"))
            .add("total_amount", KvpField.of("The total amount to be paid.", "1250.50"))
            .build();

        KvpPage pages = KvpPage.of("Invoice page", KvpSlice.of(fields, List.of(0.0, 0.0, 1.0, 1.0)));

        TextClassificationSemanticConfig semanticConfig = TextClassificationSemanticConfig.builder()
            .enableTextHints(true)
            .enableSchemaKvp(true)
            .groundingMode(GroundingMode.FAST)
            .forceSchemaName("None")
            .defaultModelName("defaultModelName")
            .taskModelNameOverride(Map.of("test", "test"))
            .schemasMergeStrategy(SchemaMergeStrategy.REPLACE)
            .schemas(
                Schema.builder()
                    .documentDescription("A vendor-issued invoice listing purchased items, prices, and payment information.")
                    .documentType("Invoice")
                    .fields(fields)
                    .pages(pages)
                    .additionalPromptInstructions("additional instructions")
                    .build(),
                Schema.builder()
                    .documentDescription("A legal document outlining terms and conditions between two parties.")
                    .documentType("Contract")
                    .build()
            ).build();

        TextClassificationParameters p = TextClassificationParameters.builder()
            .addCustomProperty("custom_1", "custom_value_1")
            .addCustomProperty("custom_2", "custom_value_2")
            .autoRotationCorrection(true)
            .classificationMode(ClassificationMode.EXACT)
            .documentReference(CosReference.of("connection-id", "my-bucket"))
            .languages(Language.ENGLISH)
            .ocrMode(OcrMode.FORCED)
            .projectId("project-id")
            .spaceId("space-id")
            .semanticConfig(semanticConfig)
            .transactionId("transaction-id")
            .build();

        Parameters parameters = p.toParameters();
        Map<String, Object> custom = Map.of(
            "custom_1", "custom_value_1",
            "custom_2", "custom_value_2"
        );

        Entity entity = new Entity(documentReference, classificationResult, parameters, custom);
        TextClassificationResponse response = new TextClassificationResponse(metadata, entity);
        JSONAssert.assertEquals(RESULT, toJson(response), true);

        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
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
                    %s,
                    "custom": {
                        "custom_1": "custom_value_1",
                        "custom_2": "custom_value_2"
                    }
                }""".formatted(PARAMETERS)))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESULT)
            ));

        var result = classificationService.startClassification("test.pdf", p);
        assertEquals(metadata, result.metadata());
        assertEquals(entity, result.entity());
    }

    @Test
    void should_start_classification() throws Exception {

        var RESULT = Files.readString(Path.of(ClassLoader.getSystemResource("classification_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
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
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESULT)
            ));

        var result = classificationService.startClassification("test.pdf");
        assertNotNull(result);
    }

    @Test
    void should_fetch_classification_request() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s"
            .formatted(API_VERSION, URLEncoder.encode("project-id", Charset.defaultCharset())))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted(Status.SUBMITTED.value()))
            ));

        var response = classificationService.fetchClassificationRequest("id");
        JSONAssert.assertEquals(JOB.formatted(Status.SUBMITTED.value()), Json.toJson(response), true);

        var projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());

        watsonxServer.resetAll();

        watsonxServer
            .stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")
                ));

        var parameters = TextClassificationFetchParameters.builder()
            .projectId("new-project-id")
            .transactionId("my-transaction-id")
            .build();

        response = classificationService.fetchClassificationRequest("id", parameters);
        assertNotNull(response);

        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

        watsonxServer.resetAll();

        watsonxServer
            .stubFor(get("/ml/v1/text/classifications/id?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")
                ));

        parameters = TextClassificationFetchParameters.builder()
            .spaceId("new-space-id")
            .transactionId("my-transaction-id")
            .build();

        response = classificationService.fetchClassificationRequest("id", parameters);
        assertNotNull(response);
    }

    @Test
    void should_delete_classification_request() {

        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(delete("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        assertTrue(classificationService.deleteRequest("id"));

        watsonxServer
            .stubFor(delete("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
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

        assertFalse(classificationService.deleteRequest("id"));

        projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(
                delete("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, projectId)
                    + "&hard_delete=true")
                    .withHeader("Authorization", equalTo("Bearer token"))
                    .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                    .willReturn(aResponse()
                        .withStatus(204)
                    ));

        var parameters = TextClassificationDeleteParameters.builder()
            .projectId("new-project-id")
            .hardDelete(true)
            .transactionId("my-transaction-id")
            .build();

        assertTrue(classificationService.deleteRequest("id", parameters));

        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(delete("/ml/v1/text/classifications/id?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        parameters = TextClassificationDeleteParameters.builder()
            .spaceId("new-space-id")
            .build();

        assertTrue(classificationService.deleteRequest("id", parameters));
    }

    @Test
    void should_classify_and_fetch_result_for_existing_file() throws Exception {

        mockServers(false);
        ClassificationResult result = classificationService.classifyAndFetch("test.pdf");
        assertEquals("completed", result.status());
        assertEquals("2025-10-23T07:32:24.272Z", result.runningAt());
        assertEquals("2025-10-23T07:32:42.981Z", result.completedAt());
        assertEquals(true, result.documentClassified());
        assertEquals("Invoice", result.documentType());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_upload_classify_and_fetch_using_input_stream() throws Exception {

        mockServers(false);
        var inputStream = ClassLoader.getSystemResourceAsStream("test.pdf");
        ClassificationResult result = classificationService.uploadClassifyAndFetch(inputStream, "test.pdf");
        assertEquals("completed", result.status());
        assertEquals("2025-10-23T07:32:24.272Z", result.runningAt());
        assertEquals("2025-10-23T07:32:42.981Z", result.completedAt());
        assertEquals(true, result.documentClassified());
        assertEquals("Invoice", result.documentType());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_upload_classify_and_fetch_using_file() throws Exception {

        mockServers(false);
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        ClassificationResult result = classificationService.uploadClassifyAndFetch(file);
        assertEquals("completed", result.status());
        assertEquals("2025-10-23T07:32:24.272Z", result.runningAt());
        assertEquals("2025-10-23T07:32:42.981Z", result.completedAt());
        assertEquals(true, result.documentClassified());
        assertEquals("Invoice", result.documentType());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_uploading_non_existent_file() throws Exception {

        mockServers(false);
        var file = new File("doesnotexist.pdf");

        TextClassificationException ex = assertThrows(TextClassificationException.class,
            () -> classificationService.uploadClassifyAndFetch(file));
        assertEquals("file_not_found", ex.code());
        assertTrue(ex.getCause() instanceof FileNotFoundException);
        assertTrue(ex.toString().startsWith("TextClassificationException [code=file_not_found, message=doesnotexist.pdf"));

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_upload_and_start_classification_using_file() throws Exception {

        mockServers(false);
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        var result = classificationService.uploadAndStartClassification(file);
        assertEquals("id", result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_uploading_and_starting_with_non_existent_file() throws Exception {

        mockServers(false);
        var file = new File("doesnotexist.pdf");

        TextClassificationException ex = assertThrows(TextClassificationException.class,
            () -> classificationService.uploadAndStartClassification(file));
        assertEquals(ex.code(), "file_not_found");
        assertTrue(ex.getCause() instanceof FileNotFoundException);

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_upload_and_start_classification_using_input_stream() throws Exception {

        mockServers(false);
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("test.pdf");

        var result = classificationService.uploadAndStartClassification(inputStream, "test.pdf");
        assertEquals("id", result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_remove_uploaded_file_after_classification() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        mockServers(true);
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .removeUploadedFile(true)
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> classificationService.startClassification("test.pdf", parameters));

        assertThrows(
            IllegalArgumentException.class,
            () -> classificationService.uploadAndStartClassification(file, parameters));

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));

        ClassificationResult result = classificationService.uploadClassifyAndFetch(file, parameters);
        assertNotNull(result);
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 1);
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_delete_preexisting_document_when_classify_and_fetch_with_remove_uploaded_file() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        mockServers(true);

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .removeUploadedFile(true)
            .build();

        ClassificationResult result = classificationService.classifyAndFetch("test.pdf", parameters);
        assertNotNull(result);
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 1);
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_handle_long_running_classification_with_retries() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("classification_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
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

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("firstIteration")
            .willSetStateTo("secondIteration")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("secondIteration")
            .willSetStateTo(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)
            ));


        var result = classificationService.classifyAndFetch("test.pdf");
        assertNotNull(result);
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(2, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
    }

    @Test
    void should_throw_exception_when_classification_timeout_exceeded() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
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

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("firstIteration")
            .willSetStateTo("secondIteration")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
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
            .stubFor(delete("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .timeout(Duration.ofMillis(100))
            .removeUploadedFile(true)
            .build();

        var ex = assertThrows(
            TextClassificationException.class,
            () -> classificationService.classifyAndFetch("test.pdf", parameters));

        assertEquals("The execution of the classification test.pdf file took longer than the timeout set by 100 milliseconds",
            ex.getMessage());

        watsonxServer.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 1);
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_throw_exception_when_classification_job_fails() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));
        var JOB_ERROR = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job_error.json").toURI()));
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .removeUploadedFile(true)
            .build();

        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("simulate_failed")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("failed")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
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
            TextClassificationException.class,
            () -> classificationService.classifyAndFetch("test.pdf", parameters));

        assertEquals(ex.code(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        ex = assertThrows(
            TextClassificationException.class,
            () -> classificationService.uploadClassifyAndFetch(file, parameters));

        assertEquals(ex.code(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 2);
        watsonxServer.verify(2, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(4, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
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
            () -> classificationService.uploadAndStartClassification(file));

        assertEquals(error, ex.details().orElseThrow());

        ex = assertThrows(
            WatsonxException.class,
            () -> classificationService.uploadClassifyAndFetch(file));

        assertEquals(error, ex.details().orElseThrow());

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(2, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_throw_exception_when_classification_event_not_found() {

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                            "trace": "9ddfccd50f6649d9913810df36578d38",
                            "errors": [
                                {
                                    "code": "text_classification_event_does_not_exist",
                                    "message": "Text classification request does not exist."
                                }
                            ],
                            "status_code": 404
                        }
                    """)
            ));

        var ex = assertThrows(WatsonxException.class,
            () -> classificationService.fetchClassificationRequest("id"));
        assertEquals(WatsonxError.Code.TEXT_CLASSIFICATION_EVENT_DOES_NOT_EXIST.value(),
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
                        <Resource>/example-project-bucket/ciao.pdf</Resource>
                        <RequestId>df887c2b-43c3-4933-a3a1-b0e19e7c2231</RequestId>
                        <httpStatusCode>403</httpStatusCode>
                    </Error>""")));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .inScenario("retry")
            .whenScenarioStateIs("retry")
            .willSetStateTo(Scenario.STARTED)
            .willReturn(aResponse().withStatus(204)));

        assertTrue(classificationService.deleteFile("my-bucket", "test.pdf"));
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 2);
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

        var classificationService = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticator(mockAuthenticator)
            .cosAuthenticator(cosAuthenticator)
            .projectId("projectid")
            .documentReference(CosReference.of("connection_id", "my-bucket"))
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
                        <Resource>/example-project-bucket/ciao.pdf</Resource>
                        <RequestId>df887c2b-43c3-4933-a3a1-b0e19e7c2231</RequestId>
                        <httpStatusCode>403</httpStatusCode>
                    </Error>""")));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer custom-token"))
            .inScenario("retry")
            .whenScenarioStateIs("retry")
            .willSetStateTo(Scenario.STARTED)
            .willReturn(aResponse().withStatus(204)));

        assertTrue(classificationService.deleteFile("my-bucket", "test.pdf"));
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 2);
        cosServer.verify(2, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_upload_file() throws Exception {

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(classificationService.uploadFile(file));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_upload_file_with_different_api_key() throws Exception {

        var cosAuthenticator = mock(Authenticator.class);
        when(cosAuthenticator.token()).thenReturn("custom-token");
        when(cosAuthenticator.scheme()).thenReturn("Bearer");

        var classificationService = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticator(mockAuthenticator)
            .cosAuthenticator(cosAuthenticator)
            .projectId("projectid")
            .documentReference(CosReference.of("connection_id", "my-bucket"))
            .logRequests(true)
            .logResponses(true)
            .build();

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer custom-token"))
            .willReturn(aResponse().withStatus(200)));

        assertTrue(classificationService.uploadFile(file));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_uploading_a_non_existent_file() throws Exception {

        var file = new File("doesnotexist.pdf");
        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        TextClassificationException ex = assertThrows(TextClassificationException.class,
            () -> classificationService.uploadFile(file));
        assertEquals(ex.code(), "file_not_found");
        assertTrue(ex.getCause() instanceof FileNotFoundException);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_compare_kvp_fields_objects_for_equality() {

        KvpFields f1 = KvpFields.builder()
            .add("invoice_date", KvpField.of("The date when the invoice was issued.", "2024-07-10"))
            .add("invoice_number", KvpField.of("The unique number identifying the invoice.", "INV-2024-001"))
            .add("total_amount", KvpField.of("The total amount to be paid.", "1250.50"))
            .build();

        KvpFields f2 = KvpFields.builder()
            .add("invoice_date", KvpField.of("The date when the invoice was issued.", "2024-07-10"))
            .add("invoice_number", KvpField.of("The unique number identifying the invoice.", "INV-2024-001"))
            .add("total_amount", KvpField.of("The total amount to be paid.", "1250.50"))
            .build();

        assertEquals(f1, f2);

    }

    @Test
    void should_start_classification_with_container_reference() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");

        var CONTAINER_CLASSIFICATION_RESPONSE = """
            {
              "metadata": {
                "id": "id",
                "created_at": "2025-10-23T07:32:11.013Z",
                "project_id": "project-id"
              },
              "entity": {
                "document_reference": {
                  "type": "container",
                  "location": { "path": "invoices/q1.pdf" }
                },
                "results": {
                  "status": "completed",
                  "document_classified": true,
                  "document_type": "Invoice",
                  "running_at": "2025-10-23T07:32:24.272Z",
                  "completed_at": "2025-10-23T07:32:42.981Z"
                }
              }
            }""";

        // ContainerReference.container() is a path-free marker; path is passed at call time.
        var service = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .build();

        watsonxServer.stubFor(post(urlPathEqualTo("/ml/v1/text/classifications"))
            .withRequestBody(equalToJson("""
                {
                  "project_id": "project-id",
                  "document_reference": {
                    "type": "container",
                    "location": { "path": "invoices/q1.pdf" }
                  }
                }""", true, false))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(CONTAINER_CLASSIFICATION_RESPONSE)));

        var response = service.startClassification("invoices/q1.pdf");

        assertNotNull(response);
        assertEquals("id", response.metadata().id());
        watsonxServer.verify(postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
    }

    @Test
    void should_upload_file_via_cos_when_document_reference_is_container() throws Exception {

        var CLASSIFICATION_RESPONSE = """
            {
              "metadata": { "id": "id", "created_at": "2025-10-23T07:32:11.013Z", "project_id": "project-id" },
              "entity": {
                "document_reference": { "type": "container", "location": { "path": "test.pdf" } },
                "results": { "status": "submitted" }
              }
            }""";

        var mockProjectService = mock(ProjectService.class);
        var mockProject = mock(Project.class);
        var mockStorage = mock(ProjectStorage.class);
        var mockProps = mock(ProjectStorageProperties.class);
        when(mockProjectService.findProject("project-id")).thenReturn(Optional.of(mockProject));
        when(mockProject.storage()).thenReturn(mockStorage);
        when(mockStorage.properties()).thenReturn(mockProps);
        when(mockProps.endpointUrl()).thenReturn("http://localhost:%s".formatted(cosServer.getPort()));
        when(mockProps.bucketName()).thenReturn("my-bucket");

        var service = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .projectService(mockProjectService)
            .build();

        cosServer.stubFor(put(urlPathMatching("/my-bucket/.*"))
            .willReturn(aResponse().withStatus(200)));

        watsonxServer.stubFor(post(urlPathEqualTo("/ml/v1/text/classifications"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(CLASSIFICATION_RESPONSE)));

        var response = service.uploadAndStartClassification(new File(
            ClassLoader.getSystemResource("test.pdf").toURI()));

        assertNotNull(response);
        assertEquals("id", response.metadata().id());
        cosServer.verify(1, putRequestedFor(urlPathMatching("/my-bucket/.*")));
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/files")));

        service.uploadFile(new File(ClassLoader.getSystemResource("test.pdf").toURI()));
        cosServer.verify(2, putRequestedFor(urlPathMatching("/my-bucket/.*")));
        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/files")));
    }

    @Test
    void should_start_classification_with_container_reference_per_call_override() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");

        var CONTAINER_CLASSIFICATION_RESPONSE = """
            {
              "metadata": {
                "id": "id",
                "created_at": "2025-10-23T07:32:11.013Z",
                "project_id": "project-id"
              },
              "entity": {
                "document_reference": {
                  "type": "container",
                  "location": { "path": "invoices/override.pdf" }
                },
                "results": {
                  "status": "completed",
                  "document_classified": true,
                  "document_type": "Invoice",
                  "running_at": "2025-10-23T07:32:24.272Z",
                  "completed_at": "2025-10-23T07:32:42.981Z"
                }
              }
            }""";

        var parameters = TextClassificationParameters.builder()
            .documentReference(ContainerReference.container())
            .build();

        watsonxServer.stubFor(post(urlPathEqualTo("/ml/v1/text/classifications"))
            .withRequestBody(equalToJson("""
                {
                  "project_id": "project-id",
                  "document_reference": {
                    "type": "container",
                    "location": { "path": "invoices/override.pdf" }
                  },
                  "parameters": {}
                }""", true, false))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(CONTAINER_CLASSIFICATION_RESPONSE)));

        var response = classificationService.startClassification("invoices/override.pdf", parameters);

        assertNotNull(response);
        watsonxServer.verify(postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
    }

    @Test
    void should_start_classification_with_container_reference_and_ocr_mode() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");

        var RESPONSE = """
            {
              "metadata": { "id": "id", "created_at": "2025-10-23T07:32:11.013Z", "project_id": "project-id" },
              "entity": {
                "document_reference": { "type": "container", "location": { "path": "invoices/q1.pdf" } },
                "results": { "status": "completed", "document_classified": true, "document_type": "Invoice" }
              }
            }""";

        var service = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .build();

        var parameters = TextClassificationParameters.builder()
            .ocrMode(OcrMode.FORCED)
            .build();

        watsonxServer.stubFor(post(urlPathEqualTo("/ml/v1/text/classifications"))
            .withRequestBody(equalToJson("""
                {
                  "project_id": "project-id",
                  "document_reference": { "type": "container", "location": { "path": "invoices/q1.pdf" } },
                  "parameters": { "ocr_mode": "forced" }
                }""", true, false))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(RESPONSE)));

        var response = service.startClassification("invoices/q1.pdf", parameters);

        assertNotNull(response);
        assertEquals("id", response.metadata().id());
        watsonxServer.verify(postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
    }

    @Test
    void should_throw_when_upload_called_with_container_reference_and_no_region_nor_project_service_on_input_stream() {
        TextClassificationService[] holder = new TextClassificationService[1];
        withWatsonxServiceMock(() -> holder[0] = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .build());

        var ex = assertThrows(IllegalStateException.class,
            () -> holder[0].uploadFile(new ByteArrayInputStream(new byte[0]), "f.pdf"));
        assertTrue(ex.getMessage().contains("ProjectService"));
    }

    @Test
    void should_resolve_cos_service_lazily_via_project_service_when_container_reference_used() throws Exception {
        var mockProjectService = mock(ProjectService.class);
        var mockProject = mock(Project.class);
        var mockStorage = mock(ProjectStorage.class);
        var mockProps = mock(ProjectStorageProperties.class);
        when(mockProjectService.findProject("project-id")).thenReturn(Optional.of(mockProject));
        when(mockProject.storage()).thenReturn(mockStorage);
        when(mockStorage.properties()).thenReturn(mockProps);
        when(mockProps.endpointUrl()).thenReturn("http://localhost:%s".formatted(cosServer.getPort()));
        when(mockProps.bucketName()).thenReturn("my-bucket");

        cosServer.stubFor(put("/my-bucket/f.pdf").willReturn(aResponse().withStatus(200)));

        TextClassificationService[] holder = new TextClassificationService[1];
        withWatsonxServiceMock(() -> holder[0] = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .projectService(mockProjectService)
            .build());
        var service = holder[0];

        // uploadFile triggers lazy COS resolution on first call
        assertTrue(service.uploadFile(new ByteArrayInputStream(new byte[0]), "f.pdf"));

        // A second call should reuse the cached COS instance - only one findProject call
        assertTrue(service.uploadFile(new ByteArrayInputStream(new byte[0]), "f.pdf"));
        verify(mockProjectService, times(1)).findProject("project-id");
    }

    @Test
    void should_cleanup_via_cos_ref_override_on_timeout() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));

        // Service configured with "cos-bucket" as the default documentReference.
        // The per-call parameter overrides documentReference to "override-bucket".
        // Cleanup must delete from "override-bucket" (the per-call ref), not "cos-bucket".
        var service = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(CosReference.of("conn-id", "cos-bucket"))
            .build();

        watsonxServer.stubFor(post(urlPathEqualTo("/ml/v1/text/classifications"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(JOB.formatted("submitted"))));

        watsonxServer.stubFor(get(urlPathEqualTo("/ml/v1/text/classifications/id"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(JOB.formatted("running"))));

        watsonxServer.stubFor(delete(urlPathEqualTo("/ml/v1/text/classifications/id"))
            .willReturn(aResponse().withStatus(204)));

        cosServer.stubFor(delete("/override-bucket/test.pdf")
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .documentReference(CosReference.of("conn-id", "override-bucket"))
            .timeout(Duration.ofMillis(100))
            .removeUploadedFile(true)
            .build();

        assertThrows(TextClassificationException.class,
            () -> service.classifyAndFetch("test.pdf", parameters));

        watsonxServer.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/override-bucket/test.pdf")), 1);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/override-bucket/test.pdf")));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/cos-bucket/test.pdf")));
    }

    @Test
    void should_throw_early_when_upload_classify_fetch_file_and_container_not_resolvable() throws Exception {

        TextClassificationService[] holder = new TextClassificationService[1];
        withWatsonxServiceMock(() -> holder[0] = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .build());

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        var ex = assertThrows(IllegalStateException.class,
            () -> holder[0].uploadClassifyAndFetch(file));
        assertTrue(ex.getMessage().contains("ProjectService"));

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        cosServer.verify(0, putRequestedFor(urlPathMatching("/.*")));
    }

    @Test
    void should_throw_early_when_upload_and_start_classification_stream_and_container_not_resolvable() throws Exception {

        TextClassificationService[] holder = new TextClassificationService[1];
        withWatsonxServiceMock(() -> holder[0] = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .build());

        var ex = assertThrows(IllegalStateException.class,
            () -> holder[0].uploadAndStartClassification(new ByteArrayInputStream(new byte[0]), "f.pdf"));
        assertTrue(ex.getMessage().contains("ProjectService"));

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        cosServer.verify(0, putRequestedFor(urlPathMatching("/.*")));
    }

    @Test
    void should_delete_uploaded_file_when_post_classification_returns_error() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(500).withBody("{}")));

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .removeUploadedFile(true)
            .build();

        var ex = assertThrows(WatsonxException.class, () -> classificationService.uploadClassifyAndFetch(file, parameters));
        assertEquals(500, ex.statusCode());

        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 1);
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_cancel_job_and_delete_uploaded_file_when_polling_returns_error() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));
        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        cosServer.stubFor(put("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200).withBody(JOB.formatted("submitted"))));

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(500).withBody("{}")));

        watsonxServer.stubFor(delete("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(204)));

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .removeUploadedFile(true)
            .build();

        var ex = assertThrows(WatsonxException.class, () -> classificationService.uploadClassifyAndFetch(file, parameters));
        assertEquals(500, ex.statusCode());

        watsonxServer.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 1);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_file_is_null() {
        assertThrows(NullPointerException.class, () -> classificationService.uploadClassifyAndFetch((File) null));
        assertThrows(NullPointerException.class,
            () -> classificationService.uploadClassifyAndFetch(null, TextClassificationParameters.builder().build()));
        assertThrows(NullPointerException.class, () -> classificationService.uploadAndStartClassification((File) null));
        assertThrows(NullPointerException.class,
            () -> classificationService.uploadAndStartClassification(null, TextClassificationParameters.builder().build()));
        assertThrows(NullPointerException.class, () -> classificationService.uploadFile((File) null));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_throw_exception_when_file_is_directory(@TempDir java.nio.file.Path tempDir) {
        var dir = tempDir.toFile();

        TextClassificationException ex1 = assertThrows(TextClassificationException.class,
            () -> classificationService.uploadClassifyAndFetch(dir));
        assertEquals("directory_not_allowed", ex1.code());

        TextClassificationException ex1p = assertThrows(TextClassificationException.class,
            () -> classificationService.uploadClassifyAndFetch(dir, TextClassificationParameters.builder().build()));
        assertEquals("directory_not_allowed", ex1p.code());

        TextClassificationException ex2 = assertThrows(TextClassificationException.class,
            () -> classificationService.uploadAndStartClassification(dir));
        assertEquals("directory_not_allowed", ex2.code());

        TextClassificationException ex2p = assertThrows(TextClassificationException.class,
            () -> classificationService.uploadAndStartClassification(dir, TextClassificationParameters.builder().build()));
        assertEquals("directory_not_allowed", ex2p.code());

        TextClassificationException ex3 = assertThrows(TextClassificationException.class,
            () -> classificationService.uploadFile(dir));
        assertEquals("directory_not_allowed", ex3.code());

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        cosServer.verify(0, putRequestedFor(urlPathMatching("/.*")));
    }


    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_check_null_and_directory_before_resolving_storage_on_container_reference(@TempDir java.nio.file.Path tempDir) {
        // Verifies that the null/directory guard fires before requireUploadCapability, so
        // ProjectService is never consulted when the argument itself is invalid.
        var mockProjectService = mock(ProjectService.class);

        TextClassificationService[] holder = new TextClassificationService[1];
        withWatsonxServiceMock(() -> holder[0] = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .projectService(mockProjectService)
            .build());

        var dir = tempDir.toFile();
        var params = TextClassificationParameters.builder().build();

        assertThrows(NullPointerException.class, () -> holder[0].uploadFile((File) null));
        assertThrows(TextClassificationException.class, () -> holder[0].uploadFile(dir));

        assertThrows(NullPointerException.class, () -> holder[0].uploadClassifyAndFetch((File) null, params));
        assertThrows(TextClassificationException.class, () -> holder[0].uploadClassifyAndFetch(dir, params));

        assertThrows(NullPointerException.class, () -> holder[0].uploadAndStartClassification((File) null, params));
        assertThrows(TextClassificationException.class, () -> holder[0].uploadAndStartClassification(dir, params));

        verify(mockProjectService, times(0)).findProject(any());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_check_null_stream_and_filename_before_resolving_storage_on_container_reference() {
        var mockProjectService = mock(ProjectService.class);

        TextClassificationService[] holder = new TextClassificationService[1];
        withWatsonxServiceMock(() -> holder[0] = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .projectService(mockProjectService)
            .build());

        var params = TextClassificationParameters.builder().build();
        var is = InputStream.nullInputStream();

        assertThrows(NullPointerException.class, () -> holder[0].uploadFile((InputStream) null, "test.pdf"));
        assertThrows(NullPointerException.class, () -> holder[0].uploadFile(is, null));

        assertThrows(NullPointerException.class,
            () -> holder[0].uploadClassifyAndFetch((InputStream) null, "test.pdf", params));
        assertThrows(NullPointerException.class, () -> holder[0].uploadClassifyAndFetch(is, null, params));

        assertThrows(NullPointerException.class,
            () -> holder[0].uploadAndStartClassification((InputStream) null, "test.pdf", params));
        assertThrows(NullPointerException.class, () -> holder[0].uploadAndStartClassification(is, null, params));

        verify(mockProjectService, times(0)).findProject(any());
    }

    @Test
    void should_upload_to_project_bucket_when_per_call_container_reference_overrides_service_cos_reference() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");

        var mockProjectService = mock(ProjectService.class);
        var mockProject = mock(Project.class);
        var mockStorage = mock(ProjectStorage.class);
        var mockProps = mock(ProjectStorageProperties.class);
        when(mockProjectService.findProject("project-id")).thenReturn(Optional.of(mockProject));
        when(mockProject.storage()).thenReturn(mockStorage);
        when(mockStorage.properties()).thenReturn(mockProps);
        when(mockProps.endpointUrl()).thenReturn("http://localhost:%s".formatted(cosServer.getPort()));
        when(mockProps.bucketName()).thenReturn("project-bucket");

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("classification_response.json").toURI()));

        // Service has CosReference("cos-bucket") as default documentReference.
        var service = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(CosReference.of("conn-id", "cos-bucket"))
            .projectService(mockProjectService)
            .build();

        // Stub PUT on project-bucket - this is where the upload MUST land.
        cosServer.stubFor(put(urlPathMatching("/project-bucket/.*"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        watsonxServer.stubFor(post(urlPathEqualTo("/ml/v1/text/classifications"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(JOB.formatted("submitted"))));

        watsonxServer.stubFor(get(urlPathEqualTo("/ml/v1/text/classifications/id"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(RESPONSE)));

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .documentReference(ContainerReference.container())
            .build();

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        var response = service.uploadAndStartClassification(file, parameters);
        assertNotNull(response);

        // Upload went to project-bucket, not to cos-bucket.
        cosServer.verify(1, putRequestedFor(urlPathMatching("/project-bucket/.*")));
        cosServer.verify(0, putRequestedFor(urlPathMatching("/cos-bucket/.*")));
    }

    @Test
    void should_return_result_when_delete_of_uploaded_file_responds_403() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        mockServers(false);

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/xml")
                .withBody("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Error>
                        <Code>AccessDenied</Code>
                        <Message>Access Denied</Message>
                        <Resource>/my-bucket/test.pdf</Resource>
                        <RequestId>df887c2b-43c3-4933-a3a1-b0e19e7c2231</RequestId>
                        <httpStatusCode>403</httpStatusCode>
                    </Error>""")));

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .removeUploadedFile(true)
            .build();

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());

        // Must succeed: 403 on delete is logged and swallowed.
        ClassificationResult result = classificationService.uploadClassifyAndFetch(file, parameters);
        assertNotNull(result);
        assertEquals("completed", result.status());

        // AccessDenied triggers a token-expiry retry, so two DELETE attempts are expected.
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))), 2);
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(2, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
    }


    @Test
    void should_interrupt_classification_restore_flag_and_delete_job() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));

        // POST returns "running" so the poll loop never exits by itself.
        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200).withBody(JOB.formatted("running"))));

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200).withBody(JOB.formatted("running"))));

        watsonxServer.stubFor(delete("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(204)));

        TextClassificationException[] result = new TextClassificationException[1];
        boolean[] interruptFlag = new boolean[1];

        var thread = new Thread(() -> {
            try {
                classificationService.classifyAndFetch("test.pdf");
            } catch (TextClassificationException e) {
                result[0] = e;
            } finally {
                interruptFlag[0] = Thread.currentThread().isInterrupted();
            }
        });
        thread.start();

        // Wait until the poll loop has fired at least one GET, then wait for the thread to
        // enter TIMED_WAITING (inside Thread.sleep in the poll loop) before interrupting.
        // This avoids a race where interrupt() fires while HttpClient is still reading the
        // HTTP response, which would throw a different exception from the sleep interruption.
        waitForRequests(watsonxServer, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")), 1);
        waitForTimedWaiting(thread, 5_000);
        thread.interrupt();
        thread.join(5_000);
        assertFalse(thread.isAlive());

        assertNotNull(result[0]);
        assertEquals("interrupted", result[0].code());
        assertTrue(interruptFlag[0], "interrupt flag must be restored on the calling thread");
        watsonxServer.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
    }

    @Test
    void should_delete_from_project_bucket_when_container_reference_used_with_remove_uploaded_file() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("classification_response.json").toURI()));

        var mockProjectService = mock(ProjectService.class);
        var mockProject = mock(Project.class);
        var mockStorage = mock(ProjectStorage.class);
        var mockProps = mock(ProjectStorageProperties.class);
        when(mockProjectService.findProject("project-id")).thenReturn(Optional.of(mockProject));
        when(mockProject.storage()).thenReturn(mockStorage);
        when(mockStorage.properties()).thenReturn(mockProps);
        when(mockProps.endpointUrl()).thenReturn("http://localhost:%s".formatted(cosServer.getPort()));
        when(mockProps.bucketName()).thenReturn("project-bucket");

        var service = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .projectService(mockProjectService)
            .build();

        // PUT and DELETE must both target the exact same project-bucket/test.pdf path.
        cosServer.stubFor(put("/project-bucket/test.pdf")
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        cosServer.stubFor(delete("/project-bucket/test.pdf")
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(204)));

        watsonxServer.stubFor(post(urlPathEqualTo("/ml/v1/text/classifications"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(JOB.formatted("submitted"))));

        watsonxServer.stubFor(get(urlPathEqualTo("/ml/v1/text/classifications/id"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(RESPONSE)));

        var file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .removeUploadedFile(true)
            .build();

        var classResult = service.uploadClassifyAndFetch(file, parameters);
        assertNotNull(classResult);

        // Upload and cleanup must both hit the same exact key.
        cosServer.verify(1, putRequestedFor(urlEqualTo("/project-bucket/test.pdf")));
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/project-bucket/test.pdf")), 1);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/project-bucket/test.pdf")));
    }

    @Test
    void should_clear_interrupt_flag_before_resolving_project_storage_during_cleanup() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));

        AtomicBoolean interruptedDuringFindProject = new AtomicBoolean(false);

        var mockProjectService = mock(ProjectService.class);
        var mockProject = mock(Project.class);
        var mockStorage = mock(ProjectStorage.class);
        var mockProps = mock(ProjectStorageProperties.class);
        when(mockProjectService.findProject("project-id")).thenAnswer(inv -> {
            interruptedDuringFindProject.set(Thread.currentThread().isInterrupted());
            return Optional.of(mockProject);
        });
        when(mockProject.storage()).thenReturn(mockStorage);
        when(mockStorage.properties()).thenReturn(mockProps);
        when(mockProps.endpointUrl()).thenReturn("http://localhost:%s".formatted(cosServer.getPort()));
        when(mockProps.bucketName()).thenReturn("project-bucket");

        var service = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .projectService(mockProjectService)
            .build();

        // POST returns "running" so the poll loop never exits on its own.
        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200).withBody(JOB.formatted("running"))));

        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200).withBody(JOB.formatted("running"))));

        watsonxServer.stubFor(delete("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(204)));

        cosServer.stubFor(delete("/project-bucket/test.pdf")
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(204)));

        TextClassificationException[] result = new TextClassificationException[1];
        boolean[] interruptFlag = new boolean[1];

        var thread = new Thread(() -> {
            try {
                service.classifyAndFetch("test.pdf", TextClassificationParameters.builder()
                    .removeUploadedFile(true)
                    .build());
            } catch (TextClassificationException e) {
                result[0] = e;
            } finally {
                interruptFlag[0] = Thread.currentThread().isInterrupted();
            }
        });
        thread.start();

        // Wait until the poll loop has fired at least one GET, then wait for TIMED_WAITING
        // so the interrupt lands inside Thread.sleep (not inside HttpClient.send).
        waitForRequests(watsonxServer, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")), 1);
        waitForTimedWaiting(thread, 5_000);
        thread.interrupt();
        thread.join(5_000);
        assertFalse(thread.isAlive());

        assertNotNull(result[0]);
        assertEquals("interrupted", result[0].code());
        assertTrue(interruptFlag[0], "interrupt flag must be restored on the calling thread");

        // The interrupt flag must have been cleared before findProject was invoked.
        assertFalse(interruptedDuringFindProject.get(), "findProject must not see the interrupt flag set");

        // The uploaded file must have been deleted from the project bucket.
        waitForRequests(cosServer, deleteRequestedFor(urlEqualTo("/project-bucket/test.pdf")), 1);
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/project-bucket/test.pdf")));
        watsonxServer.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
    }


    @Test
    void should_return_result_even_when_cleanup_throws_during_container_reference_cleanup() throws Exception {

        when(mockAuthenticator.token()).thenReturn("token");
        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("classification_response.json").toURI()));

        var mockProjectService = mock(ProjectService.class);
        when(mockProjectService.findProject("project-id")).thenThrow(new RuntimeException("storage unavailable"));

        var service = TextClassificationService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .documentReference(ContainerReference.container())
            .projectService(mockProjectService)
            .build();

        watsonxServer.stubFor(post(urlPathEqualTo("/ml/v1/text/classifications"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(JOB.formatted("submitted"))));

        watsonxServer.stubFor(get(urlPathEqualTo("/ml/v1/text/classifications/id"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(RESPONSE)));

        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .removeUploadedFile(true)
            .build();

        // POST and GET complete. Cleanup fails. Result must still be returned.
        ClassificationResult result = service.classifyAndFetch("test.pdf", parameters);
        assertNotNull(result);
        assertEquals("completed", result.status());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
    }


    private void mockServers(boolean deleteUploadedFile) throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("classification_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("classification_response.json").toURI()));
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
        watsonxServer.stubFor(post("/ml/v1/text/classifications?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("submitted"))
            ));

        // Mock result extraction.
        watsonxServer.stubFor(get("/ml/v1/text/classifications/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)
            ));
    }
}
