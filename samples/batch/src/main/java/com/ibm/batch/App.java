/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.batch;

import java.io.ByteArrayInputStream;
import java.net.URI;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.batch.BatchService;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.file.FileService;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        try {

            var url = URI.create(config.getValue("WATSONX_URL", String.class));
            var apiKey = config.getValue("WATSONX_API_KEY", String.class);
            var projectId = config.getValue("WATSONX_PROJECT_ID", String.class);

            FileService fileService = FileService.builder()
                .apiKey(apiKey)
                .baseUrl(url)
                .projectId(projectId)
                .build();

            BatchService batchService = BatchService.builder()
                .apiKey(apiKey)
                .baseUrl(url)
                .projectId(projectId)
                .endpoint("/v1/chat/completions")
                .fileService(fileService)
                .build();

            String JSONL =
                """
                    {"custom_id": "first_request",  "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small", "messages":[{"role":"user","content":[{"type":"text","text":"Capital of Italy"}]}],"max_completion_tokens":0, "temperature":0}}
                    {"custom_id": "second_request", "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small", "messages":[{"role":"user","content":[{"type":"text","text":"Capital of French"}]}],"max_completion_tokens":0, "temperature":0}}
                    {"custom_id": "third_request",  "method": "POST", "url":"/v1/chat/completions", "body": { "model":"ibm/granite-4-h-small", "messages":[{"role":"user","content":[{"type":"text","text":"Capital of Germany"}]}],"max_completion_tokens":0, "temperature":0}}""";

            try (var inputStream = new ByteArrayInputStream(JSONL.getBytes())) {
                var results = batchService.submitAndFetch(inputStream, ChatResponse.class);
                results.forEach(result -> System.out.println(result.customId() + ": " + result.response().body().toAssistantMessage().content()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
