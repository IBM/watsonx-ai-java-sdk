/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.project.ProjectService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_WX_URL", matches = ".+")
public class ProjectServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_WX_URL");

    static final ProjectService projectService = ProjectService.builder()
        .baseUrl(URL.replace("/wx", ""))
        .apiKey(API_KEY)
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_return_project_with_valid_metadata() {
        var project = projectService.findProject(PROJECT_ID).orElseThrow();
        assertNotNull(project.guid());
        assertNotNull(project.name());
        assertNotNull(project.createdAt());
        assertNotNull(project.updatedAt());
        assertNotNull(project.url());
    }

    @Test
    void should_return_project_scope() {
        var project = projectService.findProject(PROJECT_ID).orElseThrow();
        assertNotNull(project.scope());
        assertNotNull(project.scope().bssAccountId());
    }

    @Test
    void should_return_empty_when_project_not_found() {
        var result = projectService.findProject("00000000-0000-0000-0000-000000000000");
        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_all_project_fields() {
        var project = projectService.findProject(PROJECT_ID).orElseThrow();

        assertNotNull(project.guid());
        assertNotNull(project.url());
        assertNotNull(project.createdAt());
        assertNotNull(project.updatedAt());

        assertNotNull(project.name());
        assertNotNull(project.generator());
        assertNotNull(project.creator());
        assertNotNull(project.creatorIamId());
        assertNotNull(project.type());

        var storage = project.storage();
        assertNotNull(storage);
        assertNotNull(storage.type());
        assertNotNull(storage.guid());

        var props = storage.properties();
        assertNotNull(props);
        assertNotNull(props.bucketName());
        assertNotNull(props.bucketRegion());
        assertNotNull(props.endpointUrl());

        var credentials = props.credentials();
        assertNotNull(credentials);
        assertFalse(credentials.isEmpty());

        var admin = credentials.get("admin");
        assertNotNull(admin);
        assertNotNull(admin.apiKey());
        assertNotNull(admin.serviceId());
        assertNotNull(admin.accessKeyId());
        assertNotNull(admin.secretAccessKey());

        var editor = credentials.get("editor");
        assertNotNull(editor);
        assertNotNull(editor.apiKey());
        assertNotNull(editor.serviceId());
        assertNotNull(editor.accessKeyId());
        assertNotNull(editor.secretAccessKey());

        var viewer = credentials.get("viewer");
        assertNotNull(viewer);
        assertNotNull(viewer.apiKey());
        assertNotNull(viewer.serviceId());
        assertNotNull(viewer.accessKeyId());
        assertNotNull(viewer.secretAccessKey());

        var scope = project.scope();
        assertNotNull(scope);
        assertNotNull(scope.bssAccountId());
        assertNotNull(scope.samlInstanceName());

        var catalog = project.catalog();
        assertNotNull(catalog);
        assertNotNull(catalog.guid());

        var compute = project.compute();
        assertNotNull(compute);
        assertFalse(compute.isEmpty());

        var computeInstance = compute.get(0);
        assertNotNull(computeInstance.name());
        assertNotNull(computeInstance.guid());
        assertNotNull(computeInstance.type());
        assertNotNull(computeInstance.crn());
    }
}
