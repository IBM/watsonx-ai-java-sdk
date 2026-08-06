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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
import java.util.List;
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
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaException;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaFetchParameters;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaResponse;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemas;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClusterSchemaTest extends AbstractWatsonxTest {

    @RegisterExtension
    WireMockExtension watsonxServer = WireMockExtension.newInstance()
        .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    ClusterSchemaService clusterSchemaService;

    static final ClusterSchemas PASSPORT = new ClusterSchemas("Passport",
        Schema.builder().documentType("Passport").documentDescription("A government-issued travel document").build());

    static final ClusterSchemas NATIONAL_ID = new ClusterSchemas("National_ID",
        Schema.builder().documentType("National ID").documentDescription("A government-issued national identity card").build());

    @BeforeEach
    void beforeEach() {
        watsonxServer.resetAll();
        when(mockAuthenticator.token()).thenReturn("token");
        clusterSchemaService = ClusterSchemaService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .build();
    }

    // -------------------------------------------------------------------------
    // startClusterSchema
    // -------------------------------------------------------------------------

    @Test
    void should_start_cluster_schema_with_varargs() throws Exception {

        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "project-id",
                    "parameters": {
                        "schemas": [
                            {
                                "document_name": "Passport",
                                "schema": {
                                    "document_type": "Passport",
                                    "document_description": "A government-issued travel document"
                                }
                            },
                            {
                                "document_name": "National_ID",
                                "schema": {
                                    "document_type": "National ID",
                                    "document_description": "A government-issued national identity card"
                                }
                            }
                        ]
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(201)
                .withBody(RESPONSE.formatted("submitted"))
            ));

        var result = clusterSchemaService.startClusterSchema(PASSPORT, NATIONAL_ID);
        assertNotNull(result);
        assertEquals("id", result.metadata().id());
        assertEquals("submitted", result.entity().results().status());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster")));
    }

    @Test
    void should_start_cluster_schema_with_parameters_and_semantic_config() throws Exception {

        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("tx-id"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "custom-project",
                    "parameters": {
                        "schemas": [
                            {
                                "document_name": "Passport",
                                "schema": {
                                    "document_type": "Passport",
                                    "document_description": "A government-issued travel document"
                                }
                            }
                        ],
                        "semantic_config": {
                            "default_model_name": "my-model"
                        }
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(201)
                .withBody(RESPONSE.formatted("submitted"))
            ));

        var parameters = ClusterSchemaParameters.builder()
            .projectId("custom-project")
            .transactionId("tx-id")
            .semanticConfig(ClusterSchemaSemanticConfig.builder().defaultModelName("my-model").build())
            .build();

        var result = clusterSchemaService.startClusterSchema(parameters, PASSPORT);
        assertNotNull(result);

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster")));
    }

    @Test
    void should_start_cluster_schema_with_space_id() throws Exception {

        var service = ClusterSchemaService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .spaceId("space-id")
            .build();

        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withRequestBody(equalToJson("""
                {
                    "space_id": "space-id",
                    "parameters": {
                        "schemas": [
                            {
                                "document_name": "Passport",
                                "schema": {
                                    "document_type": "Passport",
                                    "document_description": "A government-issued travel document"
                                }
                            }
                        ]
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(201)
                .withBody(RESPONSE.formatted("submitted"))
            ));

        var result = service.startClusterSchema(PASSPORT);
        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // clusterSchemaAndFetch (polling)
    // -------------------------------------------------------------------------

    @Test
    void should_cluster_schema_and_fetch_completed_immediately() throws Exception {

        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .willReturn(aResponse().withStatus(201).withBody(RESPONSE)));

        var groups = clusterSchemaService.clusterSchemaAndFetch(PASSPORT, NATIONAL_ID);
        assertNotNull(groups);
        assertEquals(1, groups.size());
        var names = groups.get(0).stream().map(ClusterSchemas::documentName).toList();
        assertTrue(names.contains("Passport"));
        assertTrue(names.contains("National_ID"));

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster/id")));
    }

    @Test
    void should_cluster_schema_with_polling_retries() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job.json").toURI()));
        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_response.json").toURI()));
        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .inScenario("polling")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("first")
            .willReturn(aResponse().withStatus(201).withBody(JOB.formatted("submitted"))));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .inScenario("polling")
            .whenScenarioStateIs("first")
            .willSetStateTo("second")
            .willReturn(aResponse().withStatus(200).withBody(JOB.formatted("running"))));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .inScenario("polling")
            .whenScenarioStateIs("second")
            .willSetStateTo(Scenario.STARTED)
            .willReturn(aResponse().withStatus(200).withBody(RESPONSE)));

        var groups = clusterSchemaService.clusterSchemaAndFetch(PASSPORT, NATIONAL_ID);
        assertNotNull(groups);
        assertFalse(groups.isEmpty());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster")));
        watsonxServer.verify(2, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster/id")));
    }

    @Test
    void should_throw_exception_when_cluster_schema_job_fails() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job.json").toURI()));
        var JOB_ERROR = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job_error.json").toURI()));
        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .inScenario("fail")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("running")
            .willReturn(aResponse().withStatus(201).withBody(JOB.formatted("submitted"))));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .inScenario("fail")
            .whenScenarioStateIs("running")
            .willSetStateTo(Scenario.STARTED)
            .willReturn(aResponse().withStatus(200).withBody(JOB_ERROR)));

        var ex = assertThrows(ClusterSchemaException.class,
            () -> clusterSchemaService.clusterSchemaAndFetch(PASSPORT, NATIONAL_ID));
        assertEquals("cluster_error", ex.code());
        assertEquals("cluster error message", ex.getMessage());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster/id")));
    }

    @Test
    void should_throw_cluster_schema_exception_with_no_error_details_when_job_fails_without_error() throws Exception {

        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .willReturn(aResponse().withStatus(201).withBody("""
                {
                    "metadata": {"id": "id", "created_at": "2025-01-01T00:00:00Z", "project_id": "project-id"},
                    "entity": {"parameters": {"schemas": []}, "results": {"status": "submitted"}}
                }""")));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .willReturn(aResponse().withStatus(200).withBody("""
                {
                    "metadata": {"id": "id", "created_at": "2025-01-01T00:00:00Z", "project_id": "project-id"},
                    "entity": {"parameters": {"schemas": []}, "results": {"status": "failed"}}
                }""")));

        var ex = assertThrows(ClusterSchemaException.class,
            () -> clusterSchemaService.clusterSchemaAndFetch(PASSPORT));
        assertEquals("generic_error", ex.code());
        assertEquals("The cluster schema failed without error details", ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // fetchRequest
    // -------------------------------------------------------------------------

    @Test
    void should_fetch_cluster_schema_request() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job.json").toURI()));
        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        watsonxServer.stubFor(get("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse().withStatus(200).withBody(JOB.formatted(Status.SUBMITTED.value()))));

        var response = clusterSchemaService.fetchRequest("id");
        JSONAssert.assertEquals(JOB.formatted(Status.SUBMITTED.value()), Json.toJson(response), true);

        // With explicit project override + transaction id
        var newProjectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());
        watsonxServer.resetAll();

        watsonxServer.stubFor(get("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, newProjectId))
            .withHeader("Authorization", equalTo("Bearer token"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("tx-123"))
            .willReturn(aResponse().withStatus(200).withBody("{}")));

        var p = ClusterSchemaFetchParameters.builder().projectId("new-project-id").transactionId("tx-123").build();
        response = clusterSchemaService.fetchRequest("id", p);
        assertNotNull(response);

        // With space id override
        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());
        watsonxServer.resetAll();

        watsonxServer.stubFor(get("/ml/v1/text/schemas/cluster/id?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(200).withBody("{}")));

        p = ClusterSchemaFetchParameters.builder().spaceId("new-space-id").build();
        response = clusterSchemaService.fetchRequest("id", p);
        assertNotNull(response);
    }

    // -------------------------------------------------------------------------
    // deleteRequest
    // -------------------------------------------------------------------------

    @Test
    void should_delete_cluster_schema_request() {

        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        // Default delete - returns 204 success
        watsonxServer.stubFor(delete("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(204)));

        assertTrue(clusterSchemaService.deleteRequest("id"));

        // 404 returns false
        watsonxServer.stubFor(delete("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .withHeader("Authorization", equalTo("Bearer token"))
            .willReturn(aResponse().withStatus(404).withBody("""
                {
                    "trace": "abc123",
                    "errors": [{"code": "not_found", "message": "Request not found."}]
                }""")));

        assertFalse(clusterSchemaService.deleteRequest("id"));

        // With hard_delete=true + custom project + transaction id
        var newProjectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());
        watsonxServer.stubFor(
            delete("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s&hard_delete=true".formatted(API_VERSION, newProjectId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader(TRANSACTION_ID_HEADER, equalTo("tx-id"))
                .willReturn(aResponse().withStatus(204)));

        var p = ClusterSchemaDeleteParameters.builder()
            .projectId("new-project-id")
            .hardDelete(true)
            .transactionId("tx-id")
            .build();
        assertTrue(clusterSchemaService.deleteRequest("id", p));

        // With space id
        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());
        watsonxServer.stubFor(
            delete("/ml/v1/text/schemas/cluster/id?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer token"))
                .willReturn(aResponse().withStatus(204)));

        p = ClusterSchemaDeleteParameters.builder().spaceId("new-space-id").build();
        assertTrue(clusterSchemaService.deleteRequest("id", p));
    }

    // -------------------------------------------------------------------------
    // JSON serialisation
    // -------------------------------------------------------------------------

    @Test
    void should_serialise_cluster_schema_response() throws Exception {

        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_response.json").toURI()));

        var response = Json.fromJson(RESPONSE, ClusterSchemaResponse.class);
        assertNotNull(response);
        assertEquals("id", response.metadata().id());
        assertEquals("completed", response.entity().results().status());
        assertEquals("2025-10-23T13:12:20.000Z", response.entity().results().runningAt());
        assertEquals("2025-10-23T13:12:25.000Z", response.entity().results().completedAt());
        assertNotNull(response.entity().results().schemas());
        assertEquals(1, response.entity().results().schemas().size());
        var cluster = response.entity().results().schemas().get(0);
        assertEquals(2, cluster.size());
        assertEquals("Passport", cluster.get(0).documentName());
        assertEquals("National_ID", cluster.get(1).documentName());

        JSONAssert.assertEquals(RESPONSE, toJson(response), true);
    }

    @Test
    void should_build_cluster_schema_parameters() {

        var p = ClusterSchemaParameters.builder()
            .projectId("proj")
            .transactionId("tx")
            .schemas(PASSPORT, NATIONAL_ID)
            .semanticConfig(ClusterSchemaSemanticConfig.builder().defaultModelName("model").build())
            .build();

        assertEquals("proj", p.projectId());
        assertEquals("tx", p.transactionId());
        assertEquals(2, p.schemas().size());
        assertEquals("model", p.semanticConfig().defaultModelName());

        var params = p.toParameters();
        assertEquals(2, params.schemas().size());
        assertEquals("model", params.semanticConfig().defaultModelName());
    }

    @Test
    void should_build_cluster_schema_delete_parameters_with_hard_delete() {

        var p = ClusterSchemaDeleteParameters.builder()
            .projectId("proj")
            .hardDelete(true)
            .build();

        assertEquals("proj", p.projectId());
        assertTrue(p.hardDelete().orElseThrow());
        assertTrue(p.toString().contains("hardDelete=Optional[true]"));
    }

    @Test
    void should_have_empty_hard_delete_when_not_set() {

        var p = ClusterSchemaDeleteParameters.builder().projectId("proj").build();
        assertTrue(p.hardDelete().isEmpty());
    }

    @Test
    void should_build_cluster_schema_exception_correctly() {

        var ex = new ClusterSchemaException("code", "message");
        assertEquals("code", ex.code());
        assertEquals("message", ex.getMessage());
        assertEquals("ClusterSchemaException [code=code, message=message]", ex.toString());

        var cause = new RuntimeException("cause");
        var ex2 = new ClusterSchemaException("code2", "message2", cause);
        assertEquals("code2", ex2.code());
        assertEquals(cause, ex2.getCause());
    }

    // -------------------------------------------------------------------------
    // List overloads
    // -------------------------------------------------------------------------

    @Test
    void should_start_cluster_schema_with_list() throws Exception {

        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .willReturn(aResponse().withStatus(201).withBody(RESPONSE.formatted("submitted"))));

        var result = clusterSchemaService.startClusterSchema(List.of(PASSPORT, NATIONAL_ID));
        assertNotNull(result);
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster")));
    }

    @Test
    void should_cluster_schema_and_fetch_with_list() throws Exception {

        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .willReturn(aResponse().withStatus(201).withBody(RESPONSE)));

        var groups = clusterSchemaService.clusterSchemaAndFetch(List.of(PASSPORT, NATIONAL_ID));
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
    }

    @Test
    void should_cluster_schema_and_fetch_with_params_and_varargs() throws Exception {

        var RESPONSE = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_response.json").toURI()));

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .willReturn(aResponse().withStatus(201).withBody(RESPONSE)));

        var params = ClusterSchemaParameters.builder().build();
        var groups = clusterSchemaService.clusterSchemaAndFetch(params, PASSPORT, NATIONAL_ID);
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Immediate FAILED from POST response (before any polling)
    // -------------------------------------------------------------------------

    @Test
    void should_throw_exception_when_post_response_is_immediately_failed() throws Exception {

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .willReturn(aResponse().withStatus(201).withBody("""
                {
                    "metadata": {"id": "id", "created_at": "2025-01-01T00:00:00Z", "project_id": "project-id"},
                    "entity": {"parameters": {"schemas": []}, "results": {"status": "failed",
                        "error": {"code": "immediate_error", "message": "failed right away"}}}
                }""")));

        var ex = assertThrows(ClusterSchemaException.class,
            () -> clusterSchemaService.clusterSchemaAndFetch(PASSPORT));
        assertEquals("immediate_error", ex.code());
        assertEquals("failed right away", ex.getMessage());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster/id")));
    }

    @Test
    void should_throw_generic_error_when_post_response_is_failed_without_error() throws Exception {

        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .willReturn(aResponse().withStatus(201).withBody("""
                {
                    "metadata": {"id": "id", "created_at": "2025-01-01T00:00:00Z", "project_id": "project-id"},
                    "entity": {"parameters": {"schemas": []}, "results": {"status": "failed"}}
                }""")));

        var ex = assertThrows(ClusterSchemaException.class,
            () -> clusterSchemaService.clusterSchemaAndFetch(PASSPORT));
        assertEquals("generic_error", ex.code());

        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster/id")));
    }

    // -------------------------------------------------------------------------
    // Timeout during polling
    // -------------------------------------------------------------------------

    @Test
    void should_throw_exception_on_timeout_and_delete_job() throws Exception {

        var JOB = Files.readString(Path.of(ClassLoader.getSystemResource("cluster_schema_job.json").toURI()));
        var projectId = URLEncoder.encode("project-id", Charset.defaultCharset());

        // POST returns "submitted", GET always returns "running" → polling never completes
        watsonxServer.stubFor(post("/ml/v1/text/schemas/cluster?version=%s".formatted(API_VERSION))
            .inScenario("timeout")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("polling")
            .willReturn(aResponse().withStatus(201).withBody(JOB.formatted("submitted"))));

        watsonxServer.stubFor(get("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .inScenario("timeout")
            .whenScenarioStateIs("polling")
            .willReturn(aResponse().withStatus(200).withBody(JOB.formatted("running"))));

        watsonxServer.stubFor(delete("/ml/v1/text/schemas/cluster/id?version=%s&project_id=%s".formatted(API_VERSION, projectId))
            .willReturn(aResponse().withStatus(204)));

        // 100ms is enough for HTTP calls but short enough that polling times out after first GET
        var shortTimeoutService = ClusterSchemaService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .timeout(java.time.Duration.ofMillis(100))
            .build();

        var ex = assertThrows(ClusterSchemaException.class,
            () -> shortTimeoutService.clusterSchemaAndFetch(PASSPORT));
        assertEquals("timeout", ex.code());
        assertTrue(ex.getMessage().contains("milliseconds"));

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster")));
        watsonxServer.verify(1, deleteRequestedFor(urlPathEqualTo("/ml/v1/text/schemas/cluster/id")));
    }

    // -------------------------------------------------------------------------
    // toString / builder coverage
    // -------------------------------------------------------------------------

    @Test
    void should_cover_to_string_methods() {

        var p = ClusterSchemaParameters.builder()
            .schemas(List.of(PASSPORT))
            .semanticConfig(ClusterSchemaSemanticConfig.builder().defaultModelName("m").build())
            .build();
        assertTrue(p.toString().contains("ClusterSchemaParameters"));
        assertTrue(p.toString().contains("schemas="));

        assertTrue(ClusterSchemaSemanticConfig.builder().defaultModelName("x").build().toString()
            .contains("ClusterSchemaSemanticConfig"));
        assertTrue(ClusterSchemaFetchParameters.builder().projectId("p").build().toString()
            .contains("ClusterSchemaFetchParameters"));
    }

    @Test
    void should_build_parameters_with_list_schemas() {

        var p = ClusterSchemaParameters.builder()
            .schemas(List.of(PASSPORT, NATIONAL_ID))
            .build();
        assertEquals(2, p.schemas().size());
        assertEquals("Passport", p.schemas().get(0).documentName());
    }
}
