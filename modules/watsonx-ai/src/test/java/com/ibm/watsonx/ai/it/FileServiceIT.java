/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.file.FileData;
import com.ibm.watsonx.ai.file.FileListRequest;
import com.ibm.watsonx.ai.file.FileService;
import com.ibm.watsonx.ai.file.Order;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class FileServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final Authenticator authentication = IBMCloudAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final FileService fileService = FileService.builder()
        .baseUrl(URL)
        .authenticator(authentication)
        .projectId(PROJECT_ID)
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_upload_files() throws Exception {

        var path = Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI());
        FileData fileData = fileService.upload(path);
        assertNotNull(fileData.id());
        assertEquals("file", fileData.object());
        assertNotNull(fileData.bytes());
        assertNotNull(fileData.createdAt());
        assertNotNull(fileData.expiresAt());
        assertEquals("file_to_upload.jsonl", fileData.filename());
        assertEquals("batch", fileData.purpose());
        assertEquals(Files.readString(path), fileService.retrieve(fileData.id()));

        path = Path.of(ClassLoader.getSystemResource("test.txt").toURI());
        fileData = fileService.upload(path);
        assertNotNull(fileData.id());
        assertEquals("file", fileData.object());
        assertNotNull(fileData.bytes());
        assertNotNull(fileData.createdAt());
        assertNotNull(fileData.expiresAt());
        assertEquals("test.txt", fileData.filename());
        assertEquals("batch", fileData.purpose());
        assertEquals(Files.readString(path), fileService.retrieve(fileData.id()));
    }

    @Test
    void should_list_files() throws Exception {
        var request = FileListRequest.builder()
            .limit(2)
            .order(Order.DESC)
            .build();

        var fileListResponse = fileService.list(request);
        assertEquals("list", fileListResponse.object());
        assertEquals(2, fileListResponse.data().size());
        assertEquals("test.txt", fileListResponse.data().get(0).filename());
        assertEquals("file_to_upload.jsonl", fileListResponse.data().get(1).filename());
        assertNotNull(fileListResponse.firstId());
        assertNotNull(fileListResponse.lastId());
        assertNotNull(fileListResponse.hasMore());
    }

    @Test
    void should_delete_files() throws Exception {
        var files = fileService.list();
        for (var file : files.data())
            assertTrue(fileService.delete(file.id()).deleted());
        assertEquals(0, fileService.list().data().size());
    }
}
