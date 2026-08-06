/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.catalog;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.net.URI;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.AbstractWatsonxTest;

public class ModelGatewayCatalogServiceTest extends AbstractWatsonxTest {

    private ModelGatewayCatalogService buildService() {
        return ModelGatewayCatalogService.builder()
            .authenticator(mockAuthenticator)
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .version(API_VERSION)
            .build();
    }

    // -------------------------------------------------------------------------
    // listModels
    // -------------------------------------------------------------------------

    @Test
    void should_list_models_and_parse_all_fields() {

        wireMock.stubFor(get("/ml/gateway/v1/models?version=%s".formatted(API_VERSION))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "object": "list",
                        "data": [
                            {
                                "uuid": "123e4567-e89b-12d3-a456-426614174000",
                                "object": "model",
                                "created": 1677649963,
                                "owned_by": "openai",
                                "id": "gpt-4o",
                                "alias": "gpt-4o-alias",
                                "description": "A flagship model",
                                "metadata": {
                                    "cost": 0.02,
                                    "model_family": "gpt-4",
                                    "recommender_label": "gpt-4o",
                                    "region": "us-east-1",
                                    "batch": false,
                                    "context_window": 128000
                                }
                            }
                        ]
                    }""")));

        when(mockAuthenticator.token()).thenReturn("my-super-token");

        var models = buildService().listModels();

        assertNotNull(models);
        assertEquals(1, models.size());
        var m = models.get(0);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", m.uuid());
        assertEquals("model", m.object());
        assertEquals(1677649963L, m.created());
        assertEquals("openai", m.ownedBy());
        assertEquals("gpt-4o", m.id());
        assertEquals("gpt-4o-alias", m.alias());
        assertEquals("A flagship model", m.description());
        assertNotNull(m.metadata());
        assertEquals(0.02, m.metadata().cost());
        assertEquals("gpt-4", m.metadata().modelFamily());
        assertEquals("gpt-4o", m.metadata().recommenderLabel());
        assertEquals("us-east-1", m.metadata().region());
        assertEquals(false, m.metadata().batch());
        assertEquals(128000, m.metadata().contextWindow());
    }

    @Test
    void should_return_empty_list_when_no_models_configured() {

        wireMock.stubFor(get("/ml/gateway/v1/models?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    { "object": "list", "data": [] }""")));

        when(mockAuthenticator.token()).thenReturn("my-super-token");

        var models = buildService().listModels();

        assertNotNull(models);
        assertTrue(models.isEmpty());
    }

    @Test
    void should_wrap_io_exception_from_list_models() {

        withWatsonxServiceMock(() -> {

            var service = ModelGatewayCatalogService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(URI.create("http://localhost"))
                .version(API_VERSION)
                .build();

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockSecureHttpClient.sendAsync(any(), any()))
                .thenThrow(new RuntimeException("connection refused"));

            // listModels uses the sync client — simulate by building against a port with no listener
            assertThrows(Exception.class, () -> service.listModels());
        });
    }

    // -------------------------------------------------------------------------
    // getModel
    // -------------------------------------------------------------------------

    @Test
    void should_get_model_by_uuid_and_parse_all_fields() {

        var uuid = "123e4567-e89b-12d3-a456-426614174000";
        wireMock.stubFor(get("/ml/gateway/v1/models/%s?version=%s".formatted(uuid, API_VERSION))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "uuid": "123e4567-e89b-12d3-a456-426614174000",
                        "object": "model",
                        "created": 1677649963,
                        "owned_by": "openai:vllm4",
                        "id": "gpt-3.5-turbo-456723",
                        "alias": "gpt-3.5-turbo",
                        "description": "A fast and cheap model",
                        "metadata": {
                            "cost": 0.002,
                            "model_family": "gpt-3.5",
                            "recommender_label": "gpt-3.5-turbo",
                            "region": "us-east-1",
                            "batch": true,
                            "context_window": 4096
                        }
                    }""")));

        when(mockAuthenticator.token()).thenReturn("my-super-token");

        var m = buildService().getModel(uuid);

        assertNotNull(m);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", m.uuid());
        assertEquals("model", m.object());
        assertEquals(1677649963L, m.created());
        assertEquals("openai:vllm4", m.ownedBy());
        assertEquals("gpt-3.5-turbo-456723", m.id());
        assertEquals("gpt-3.5-turbo", m.alias());
        assertEquals("A fast and cheap model", m.description());
        assertNotNull(m.metadata());
        assertEquals(0.002, m.metadata().cost());
        assertEquals("gpt-3.5", m.metadata().modelFamily());
        assertEquals("gpt-3.5-turbo", m.metadata().recommenderLabel());
        assertEquals("us-east-1", m.metadata().region());
        assertEquals(true, m.metadata().batch());
        assertEquals(4096, m.metadata().contextWindow());
    }

    @Test
    void should_return_empty_optional_fields_when_absent() {

        wireMock.stubFor(get("/ml/gateway/v1/models/minimal?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "uuid": "minimal",
                        "object": "model",
                        "created": 1677649963,
                        "owned_by": "openai",
                        "id": "gpt-4o"
                    }""")));

        when(mockAuthenticator.token()).thenReturn("my-super-token");

        var m = buildService().getModel("minimal");

        assertNull(m.alias());
        assertNull(m.description());
        assertNull(m.metadata());
    }

    @Test
    void should_return_present_optionals_when_fields_are_set() {

        wireMock.stubFor(get("/ml/gateway/v1/models/full?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "uuid": "full",
                        "object": "model",
                        "created": 1677649963,
                        "owned_by": "openai",
                        "id": "gpt-4o",
                        "alias": "friendly-name",
                        "description": "A useful model",
                        "metadata": { "cost": 0.01 }
                    }""")));

        when(mockAuthenticator.token()).thenReturn("my-super-token");

        var m = buildService().getModel("full");

        assertNotNull(m.alias());
        assertEquals("friendly-name", m.alias());
        assertNotNull(m.description());
        assertEquals("A useful model", m.description());
        assertNotNull(m.metadata());
        assertEquals(0.01, m.metadata().cost());
    }

    @Test
    void should_throw_npe_when_uuid_is_null() {

        withWatsonxServiceMock(() -> {

            var service = ModelGatewayCatalogService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(URI.create("http://localhost"))
                .version(API_VERSION)
                .build();

            assertThrows(NullPointerException.class, () -> service.getModel(null));
        });
    }

    @Test
    void should_wrap_io_exception_from_get_model() {

        withWatsonxServiceMock(() -> {

            var service = ModelGatewayCatalogService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl(URI.create("http://localhost"))
                .version(API_VERSION)
                .build();

            when(mockAuthenticator.token()).thenReturn("my-super-token");
            when(mockSecureHttpClient.sendAsync(any(), any()))
                .thenThrow(new RuntimeException("connection refused"));

            // getModel uses the sync client — simulate by building against a port with no listener
            assertThrows(Exception.class, () -> service.getModel("some-uuid"));
        });
    }

    // -------------------------------------------------------------------------
    // Builder validation
    // -------------------------------------------------------------------------

    @Test
    void should_throw_when_authenticator_is_missing() {

        var ex = assertThrows(NullPointerException.class, () -> ModelGatewayCatalogService.builder()
            .baseUrl(URI.create("http://localhost"))
            .build());
        assertEquals("authenticator cannot be null", ex.getMessage());
    }
}
