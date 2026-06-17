/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.Metadata;
import com.ibm.watsonx.ai.textprocessing.Schema;
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

    @BeforeEach
    void beforeEach() {
        cosServer.resetAll();
        watsonxServer.resetAll();
        when(mockAuthenticator.token()).thenReturn("token");
        improveSchemaService = ImproveSchemaService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .authenticator(mockAuthenticator)
            .projectId("project-id")
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    @Test
    void should_improve_schema__with_parameters() throws Exception {

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
}
