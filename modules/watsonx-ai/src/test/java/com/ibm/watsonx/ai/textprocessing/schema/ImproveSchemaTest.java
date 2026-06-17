/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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
import static org.mockito.Mockito.when;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.exception.model.WatsonxError;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.Metadata;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaException;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaFetchParameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaResponse;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaResponse.Entity;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaResponse.ImproveSchemaResult;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.improve.Parameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.Parameters.SemanticConfig;


@ExtendWith(MockitoExtension.class)
public class ImproveSchemaTest extends AbstractWatsonxTest {

    @RegisterExtension
    WireMockExtension cosServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort().http2PlainDisabled(true))
        .build();

    @RegisterExtension
    WireMockExtension watsonxServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

    ImproveSchemaService improveSchemaService;

    final Schema schema = Schema.builder()
        .documentType("Passport")
        .documentDescription("Passport document to get the schema")
        .fields(
            KvpFields.builder()
                .add("name", KvpField.of("name of the user", "Andrea"))
                .build())
        .build();

    @BeforeEach
    void beforeEach() {
        cosServer.resetAll();
        watsonxServer.resetAll();
        when(mockAuthenticator.token()).thenReturn("token");
        improveSchemaService = ImproveSchemaService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .build();
    }


    @Test
    void should_improve_schema_with_parameters() throws Exception {

        var PARAMETERS = """
            "parameters": {
              "schema": {
                "document_type": "Passport",
                "document_description": "Passport document to get the schema",
                "fields": {
                  "name": {
                      "description": "name of the user",
                      "example": "Andrea"
                  }
                },
                "additional_prompt_instructions": "Passaport"
              },
              "semantic_config": {
                  "default_model_name": "ibm/granite-4-h-small"
              }
            }""";

        var RESULT =
            """
                {
                    "entity": {
                        "parameters": {
                            "schema": {
                                "additional_prompt_instructions": "Passaport",
                                "document_description": "Passport document to get the schema",
                                "document_type": "Passport",
                                "fields": {
                                    "name": {
                                        "description": "name of the user",
                                        "example": "Andrea"
                                    }
                                }
                            },
                            "semantic_config": {
                                "default_model_name": "ibm/granite-4-h-small"
                            }
                        },
                        "results": {
                            "completed_at": "2026-06-16T14:44:12.401Z",
                            "running_at": "2026-06-16T14:44:05.747Z",
                            "schema": {
                                "additional_prompt_instructions": "Passaport",
                                "document_description": "A Passport document serves as an official government-issued identification for international travel, verifying the holder's identity and nationality. It is required for border crossings and visa applications. Key fields include: name, passport number, and expiration date.",
                                "document_type": "Passport",
                                "fields": {
                                    "name": {
                                        "description": "name of the user",
                                        "example": "Andrea"
                                    }
                                }
                            },
                            "status": "completed"
                        }
                    },
                    "metadata": {
                        "created_at": "2026-06-16T14:44:04.430Z",
                        "id": "id",
                        "modified_at": "2026-06-16T14:44:12.528Z",
                        "project_id": "project-id",
                        "space_id": "space-id"
                    }
                }"""
                .formatted(PARAMETERS);


        Schema schemaToImprove = Schema.builder()
            .documentType("Passport")
            .documentDescription("Passport document to get the schema")
            .fields(
                KvpFields.builder()
                    .add("name", KvpField.of("name of the user", "Andrea"))
                    .build())
            .additionalPromptInstructions("Passaport")
            .build();

        Schema schemaImproved = Schema.builder()
            .documentType("Passport")
            .documentDescription(
                "A Passport document serves as an official government-issued identification for international travel, verifying the holder's identity and nationality. It is required for border crossings and visa applications. Key fields include: name, passport number, and expiration date.")
            .fields(
                KvpFields.builder()
                    .add("name", KvpField.of("name of the user", "Andrea"))
                    .build())
            .additionalPromptInstructions("Passaport")
            .build();

        Metadata metadata = new Metadata("id", "2026-06-16T14:44:04.430Z", "2026-06-16T14:44:12.528Z", "space-id", "project-id");
        ImproveSchemaResult improveSchemaResult =
            new ImproveSchemaResult("completed", "2026-06-16T14:44:05.747Z", "2026-06-16T14:44:12.401Z", schemaImproved, null, null);

        ImproveSchemaSemanticConfig semanticConfig = ImproveSchemaSemanticConfig
            .builder()
            .defaultModelName("ibm/granite-4-h-small")
            .build();

        ImproveSchemaParameters p = ImproveSchemaParameters.builder()
            .projectId("project-id")
            .spaceId("space-id")
            .semanticConfig(semanticConfig)
            .build();

        Entity entity = new Entity(new Parameters(schemaToImprove, new SemanticConfig("ibm/granite-4-h-small")), improveSchemaResult);
        ImproveSchemaResponse response = new ImproveSchemaResponse(metadata, entity);
        JSONAssert.assertEquals(RESULT, toJson(response), true);

        watsonxServer.stubFor(post("/ml/v1/text/schemas/improve?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "project-id",
                    "space_id": "space-id",
                    %s
                }""".formatted(PARAMETERS)))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESULT)
            ));

        var result = improveSchemaService.startImproveSchema(schemaToImprove, p);
        assertEquals(metadata, result.metadata());
        assertEquals(entity, result.entity());
    }

    @Test
    void should_start_improve_schema() throws Exception {

        var RESULT = Files.readString(Path.of(ClassLoader.getSystemResource("improve_schema_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/improve?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "project-id",
                    "parameters": {
                        "schema": {
                            "document_type": "Passport",
                            "document_description": "Passport document to get the schema",
                            "fields": {
                                "name": {
                                    "description": "name of the user",
                                    "example": "Andrea"
                                }
                            }
                        }
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESULT)
            ));

        var result = improveSchemaService.startImproveSchema(schema);
        assertNotNull(result);
    }

    @Test
    void should_fetch_improve_schema_request() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("improve_schema_job.json").toURI()));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s"
            .formatted(API_VERSION, URLEncoder.encode("project-id", Charset.defaultCharset())))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted(Status.SUBMITTED.value()))
            ));

        var response = improveSchemaService.fetchRequest("id");
        JSONAssert.assertEquals(JOB.formatted(Status.SUBMITTED.value()), Json.toJson(response), true);

        var projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());

        watsonxServer.resetAll();

        watsonxServer
            .stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")
                ));

        var parameters = ImproveSchemaFetchParameters.builder()
            .projectId("new-project-id")
            .transactionId("my-transaction-id")
            .build();

        response = improveSchemaService.fetchRequest("id", parameters);
        assertNotNull(response);

        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

        watsonxServer.resetAll();

        watsonxServer
            .stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")
                ));

        parameters = ImproveSchemaFetchParameters.builder()
            .spaceId("new-space-id")
            .transactionId("my-transaction-id")
            .build();

        response = improveSchemaService.fetchRequest("id", parameters);
        assertNotNull(response);
    }

    @Test
    void should_delete_improve_schema_request() {

        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(delete("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        assertTrue(improveSchemaService.deleteRequest("id"));

        watsonxServer
            .stubFor(delete("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
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

        assertFalse(improveSchemaService.deleteRequest("id"));

        projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(
                delete("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, projectId)
                    + "&hard_delete=true")
                    .withHeader("Authorization", equalTo("Bearer token"))
                    .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                    .willReturn(aResponse()
                        .withStatus(204)
                    ));

        var parameters = ImproveSchemaDeleteParameters.builder()
            .projectId("new-project-id")
            .hardDelete(true)
            .transactionId("my-transaction-id")
            .build();

        assertTrue(improveSchemaService.deleteRequest("id", parameters));

        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(delete("/ml/v1/text/schemas/improve/id?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        parameters = ImproveSchemaDeleteParameters.builder()
            .spaceId("new-space-id")
            .build();

        assertTrue(improveSchemaService.deleteRequest("id", parameters));
    }

    @Test
    void should_handle_long_running_improve_schema_with_retries() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("improve_schema_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("improve_schema_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/improve?version=%s".formatted(API_VERSION))
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

        watsonxServer.stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("firstIteration")
            .willSetStateTo("secondIteration")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("secondIteration")
            .willSetStateTo(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESPONSE)
            ));


        var result = improveSchemaService.improveSchemaAndFetch(schema);
        assertNotNull(result);
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/improve")));
        watsonxServer.verify(2, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/improve/id")));
    }

    @Test
    void should_throw_exception_when_improve_schema_timeout_exceeded() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("improve_schema_job.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/improve?version=%s".formatted(API_VERSION))
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

        watsonxServer.stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("long_response")
            .whenScenarioStateIs("firstIteration")
            .willSetStateTo("secondIteration")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
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
            .stubFor(delete("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        cosServer.stubFor(delete("/%s/%s".formatted("my-bucket", "test.pdf"))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200)));

        ImproveSchemaParameters parameters = ImproveSchemaParameters.builder()
            .timeout(Duration.ofMillis(100))
            .build();

        var ex = assertThrows(
            ImproveSchemaException.class,
            () -> improveSchemaService.improveSchemaAndFetch(schema, parameters));

        assertEquals("Execution to improve schema took longer than the timeout set by 100 milliseconds",
            ex.getMessage());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/improve")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/improve/id")));
    }

    @Test
    void should_throw_exception_when_improve_schema_job_fails() throws Exception {

        when(mockAuthenticator.tokenAsync()).thenReturn(CompletableFuture.completedFuture("token"));
        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("improve_schema_job.json").toURI()));
        var JOB_ERROR = Files.readString(Path.of(ClassLoader.getSystemResource("improve_schema_job_error.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/improve?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
            .inScenario("simulate_failed")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("failed")
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(JOB.formatted("running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
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
            ImproveSchemaException.class,
            () -> improveSchemaService.improveSchemaAndFetch(schema));

        assertEquals(ex.code(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/improve")));
        watsonxServer.verify(2, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/improve/id")));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted("my-bucket", "test.pdf"))));
    }

    @Test
    void should_throw_exception_when_improve_schema_event_not_found() {

        watsonxServer.stubFor(get("/ml/v1/text/schemas/improve/id?version=%s&project_id=%s".formatted(API_VERSION, "project-id"))
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
            () -> improveSchemaService.fetchRequest("id"));
        assertEquals(WatsonxError.Code.SCHEMA_EVENT_DOES_NOT_EXIST.value(),
            ex.details().orElseThrow().errors().get(0).code());
    }
}
