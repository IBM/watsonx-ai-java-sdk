/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.core.exception.WatsonxException;

@DisabledInNativeImage
public class ProjectServiceTest extends AbstractWatsonxTest {

    private static final String PROJECT_RESPONSE = """
        {
          "metadata": {
            "guid": "c2d2f907-4dfd-49f3-ac88-3c23a3830728",
            "url": "/v2/projects/c2d2f907-4dfd-49f3-ac88-3c23a3830728",
            "created_at": "2026-09-17T10:10:29.450Z",
            "updated_at": "2026-09-17T12:01:24.631Z"
          },
          "entity": {
            "name": "adm_project",
            "generator": "wx-portal-projects",
            "description": "My description",
            "public": false,
            "type": "wx",
            "creator": "andrea.dimaio@it.ibm.com",
            "creator_iam_id": "IBMid-310002AU2V",
            "scope": {
              "bss_account_id": "4d86deaa53914d5894e275ab280afffa",
              "enforce_members": true
            },
            "catalog": {
              "public": false,
              "guid": "01a0aed8-a7f7-77d4-8d0a-7e3cd09653c2"
            },
            "compute": [
              {
                "name": "wml-310002au2v",
                "guid": "6a278d1e-85a6-4118-9091-9ea87676ca59",
                "type": "machine_learning"
              }
            ]
          }
        }
        """;

    private static final String PROJECT_ID = "c2d2f907-4dfd-49f3-ac88-3c23a3830728";

    @Test
    void should_return_project_by_id() throws Exception {
        wireMock.stubFor(get("/v2/projects/%s".formatted(PROJECT_ID))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(jsonResponse(PROJECT_RESPONSE, 200)));

        var service = ProjectService.builder()
            .baseUrl("http://localhost:%d".formatted(wireMock.getPort()))
            .build();

        var project = service.findProject(PROJECT_ID).orElseThrow();

        assertEquals(PROJECT_ID, project.guid());
        assertEquals("/v2/projects/%s".formatted(PROJECT_ID), project.url());
        assertEquals("2026-09-17T10:10:29.450Z", project.createdAt());
        assertEquals("2026-09-17T12:01:24.631Z", project.updatedAt());
        assertEquals("adm_project", project.name());
        assertEquals("My description", project.description());
        assertEquals("wx", project.type());
        assertFalse(project.publicProject());
        assertEquals("andrea.dimaio@it.ibm.com", project.creator());
        assertEquals("IBMid-310002AU2V", project.creatorIamId());
        assertEquals("wx-portal-projects", project.generator());

        assertNotNull(project.scope());
        assertEquals("4d86deaa53914d5894e275ab280afffa", project.scope().bssAccountId());

        assertNotNull(project.catalog());
        assertEquals("01a0aed8-a7f7-77d4-8d0a-7e3cd09653c2", project.catalog().guid());

        assertNotNull(project.compute());
        assertEquals(1, project.compute().size());
        assertEquals("wml-310002au2v", project.compute().get(0).name());
    }

    @Test
    void should_return_empty_optional_when_project_not_found() throws Exception {
        wireMock.stubFor(get("/v2/projects/unknown-id")
            .willReturn(aResponse().withStatus(404)));

        var service = ProjectService.builder()
            .baseUrl("http://localhost:%d".formatted(wireMock.getPort()))
            .build();

        var result = service.findProject("unknown-id");
        assertTrue(result.isEmpty());
    }

    @Test
    void should_handle_null_optional_entity_fields() throws Exception {
        var minimalResponse = """
            {
              "metadata": {
                "guid": "abc-123",
                "url": "/v2/projects/abc-123",
                "created_at": "2026-01-01T00:00:00.000Z",
                "updated_at": "2026-01-01T00:00:00.000Z"
              },
              "entity": {
                "name": "minimal",
                "type": "cpd",
                "public": true
              }
            }
            """;

        wireMock.stubFor(get("/v2/projects/abc-123")
            .willReturn(jsonResponse(minimalResponse, 200)));

        var service = ProjectService.builder()
            .baseUrl("http://localhost:%d".formatted(wireMock.getPort()))
            .build();

        var project = service.findProject("abc-123").orElseThrow();
        assertEquals("abc-123", project.guid());
        assertEquals("minimal", project.name());
        assertEquals("cpd", project.type());
        assertNull(project.description());
        assertNull(project.creator());
        assertNull(project.compute());
        assertNull(project.storage());
        assertNull(project.tags());
        assertNull(project.members());
        assertNull(project.scope());
        assertNull(project.catalog());
    }

    @Test
    void should_throw_null_pointer_when_project_id_is_null() {
        var service = ProjectService.builder()
            .baseUrl("http://localhost:%d".formatted(wireMock.getPort()))
            .build();

        assertThrows(NullPointerException.class, () -> service.findProject(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_use_correct_url_with_baseUrl() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(PROJECT_RESPONSE);

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var service = ProjectService.builder()
                .baseUrl("https://api.dataplatform.cloud.ibm.com")
                .build();

            service.findProject(PROJECT_ID);
            var uri = mockHttpRequest.getValue().uri();
            assertEquals(
                URI.create("https://api.dataplatform.cloud.ibm.com/v2/projects/%s".formatted(PROJECT_ID)),
                uri);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_throw_runtime_exception_when_http_request_fails() {
        withWatsonxServiceMock(() -> {
            try {
                when(mockSecureHttpClient.send(any(), any(BodyHandler.class)))
                    .thenThrow(IOException.class);

                var service = ProjectService.builder()
                    .baseUrl("https://api.dataplatform.cloud.ibm.com")
                    .build();

                assertThrows(RuntimeException.class, () -> service.findProject(PROJECT_ID));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_use_correct_url_with_cloud_region() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(PROJECT_RESPONSE);

        withWatsonxServiceMock(() -> {
            mockHttpClientSend(mockHttpRequest.capture(), any(BodyHandler.class));

            var service = ProjectService.builder()
                .baseUrl(CloudRegion.DALLAS)
                .build();

            service.findProject(PROJECT_ID);
            var uri = mockHttpRequest.getValue().uri();
            assertEquals(
                URI.create(CloudRegion.DALLAS.wxEndpoint().replace("/wx", "") + "/v2/projects/%s".formatted(PROJECT_ID)),
                uri);
        });
    }

    @Test
    void should_rethrow_non_404_watsonx_exception() {
        wireMock.stubFor(get("/v2/projects/%s".formatted(PROJECT_ID))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\":{\"code\":\"Internal Server Error\",\"message\":\"unexpected error\"}}")));

        var service = ProjectService.builder()
            .baseUrl("http://localhost:%d".formatted(wireMock.getPort()))
            .build();

        assertThrows(WatsonxException.class, () -> service.findProject(PROJECT_ID));
    }
}
