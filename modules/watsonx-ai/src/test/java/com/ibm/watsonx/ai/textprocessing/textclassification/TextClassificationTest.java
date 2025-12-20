/*
 * Copyright IBM Corp. 2025 - 2025
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
import java.util.Map;
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
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.KvpPage;
import com.ibm.watsonx.ai.textprocessing.KvpSlice;
import com.ibm.watsonx.ai.textprocessing.Language;
import com.ibm.watsonx.ai.textprocessing.Metadata;
import com.ibm.watsonx.ai.textprocessing.OcrMode;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.SemanticConfig.SchemaMergeStrategy;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationParameters.ClassificationMode;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationResponse.ClassificationResult;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationResponse.Entity;

@ExtendWith(MockitoExtension.class)
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
            .documentReference("connection_id", "my-bucket")
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
                        "enable_generic_kvp": true,
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
                        "number_pages_processed": 10,
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
            new DataReference("connection_asset", new CosDataConnection("connection-id"), new CosDataLocation("test.pdf", "my-bucket"));
        ClassificationResult classificationResult = new ClassificationResult(
            "completed",
            "2025-10-23T07:32:24.272Z",
            "2025-10-23T07:32:42.981Z",
            10, true, "Invoice", null);

        KvpFields fields = KvpFields.builder()
            .add("invoice_date", KvpField.of("The date when the invoice was issued.", "2024-07-10"))
            .add("invoice_number", KvpField.of("The unique number identifying the invoice.", "INV-2024-001"))
            .add("total_amount", KvpField.of("The total amount to be paid.", "1250.50"))
            .build();

        KvpPage pages = KvpPage.of("Invoice page", KvpSlice.of(fields, List.of(0.0, 0.0, 1.0, 1.0)));

        TextClassificationSemanticConfig semanticConfig = TextClassificationSemanticConfig.builder()
            .enableGenericKvp(true)
            .enableTextHints(true)
            .enableSchemaKvp(true)
            .groundingMode("fast")
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
        assertEquals(10, result.numberPagesProcessed());
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
        assertEquals(10, result.numberPagesProcessed());
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
        assertEquals(10, result.numberPagesProcessed());
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
        assertEquals(ex.getCode(), "file_not_found");
        assertTrue(ex.getCause() instanceof FileNotFoundException);
        assertEquals("TextClassificationException [code=file_not_found, message=doesnotexist.pdf (No such file or directory)]", ex.toString());

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/id")));
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
        assertEquals(ex.getCode(), "file_not_found");
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

        when(mockAuthenticator.asyncToken()).thenReturn(CompletableFuture.completedFuture("token"));
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
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));

        ClassificationResult result = classificationService.classifyAndFetch("test.pdf", parameters);
        assertNotNull(result);
        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));

        watsonxServer.resetAll();
        cosServer.resetAll();

        mockServers(true);

        result = classificationService.uploadClassifyAndFetch(file, parameters);
        assertNotNull(result);
        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
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


        TextClassificationParameters parameters = TextClassificationParameters.builder()
            .timeout(Duration.ofMillis(100))
            .build();

        var ex = assertThrows(
            TextClassificationException.class,
            () -> classificationService.classifyAndFetch("test.pdf", parameters));

        assertEquals("The execution of the classification test.pdf file took longer than the timeout set by 100 milliseconds",
            ex.getMessage());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/classifications")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/classifications/id")));
    }

    @Test
    void should_throw_exception_when_classification_job_fails() throws Exception {

        when(mockAuthenticator.asyncToken()).thenReturn(CompletableFuture.completedFuture("token"));
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

        assertEquals(ex.getCode(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        ex = assertThrows(
            TextClassificationException.class,
            () -> classificationService.uploadClassifyAndFetch(file, parameters));

        assertEquals(ex.getCode(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        Thread.sleep(200); // Wait for the async calls.
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
            () -> classificationService.uploadAndStartClassification(file),
            "The specified bucket does not exist.");

        assertEquals(error, ex.details().orElseThrow());

        ex = assertThrows(
            WatsonxException.class,
            () -> classificationService.uploadClassifyAndFetch(file),
            "The specified bucket does not exist.");

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
                            ]
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

        when(mockAuthenticator.asyncToken()).thenReturn(CompletableFuture.completedFuture("token"));
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

        assertTrue(classificationService.deleteFile("my-bucket", "test.pdf"));
        Thread.sleep(500);
        cosServer.verify(2, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void should_delete_file_with_custom_api_key() throws Exception {

        var cosAuthenticator = mock(Authenticator.class);
        when(cosAuthenticator.asyncToken()).thenReturn(CompletableFuture.completedFuture("custom-token"));
        when(mockAuthenticator.asyncToken()).thenReturn(CompletableFuture.completedFuture("token"));
        cosServer.resetAll();

        var classificationService = TextClassificationService.builder()
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

        assertTrue(classificationService.deleteFile("my-bucket", "test.pdf"));
        Thread.sleep(500);
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

        var classificationService = TextClassificationService.builder()
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
        assertEquals(ex.getCode(), "file_not_found");
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
