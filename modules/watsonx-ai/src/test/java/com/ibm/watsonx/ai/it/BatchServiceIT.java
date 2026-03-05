/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.batch.BatchCreateRequest;
import com.ibm.watsonx.ai.batch.BatchService;
import com.ibm.watsonx.ai.chat.ChatRequest;
import com.ibm.watsonx.ai.chat.ChatResponse;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.SystemMessage;
import com.ibm.watsonx.ai.chat.model.UserMessage;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.file.FileData;
import com.ibm.watsonx.ai.file.FileService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class BatchServiceIT {

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
        .build();

    static final BatchService batchService = BatchService.builder()
        .baseUrl(URL)
        .authenticator(authentication)
        .projectId(PROJECT_ID)
        .logRequests(true)
        .logResponses(true)
        .endpoint("/v1/chat/completions")
        .fileService(fileService)
        .build();

    @Test
    void should_submit_a_batch_and_get_results() throws Exception {

        var before = fileService.list().data().size();
        var path = Path.of(ClassLoader.getSystemResource("file_to_upload.jsonl").toURI());
        FileData fileData = fileService.upload(path);

        var request = BatchCreateRequest.builder()
            .inputFileId(fileData.id())
            .build();

        var batch = batchService.submitAndFetch(request, ChatResponse.class);
        assertEquals(3, batch.size());
        assertNotNull(batch.get(0).response().body().toAssistantMessage().content());
        var after = fileService.list().data().size();
        assertEquals(before, after);
    }

    @Test
    void should_submit_a_chat_response_batch_and_get_results() throws Exception {

        var parameters = ChatParameters.builder()
            .modelId("ibm/granite-4-h-small")
            .temperature(0.0)
            .maxCompletionTokens(0)
            .build();

        var chatRequests =
            Stream.of("Italy", "French", "Germany")
                .map(city -> {
                    var messages = List.of(
                        SystemMessage.of("You are an helpful assistant"),
                        UserMessage.text("What is the capital of %s? Answer with only the name of the city.".formatted(city))
                    );
                    return ChatRequest.builder()
                        .parameters(parameters)
                        .messages(messages)
                        .build();
                })
                .toList();

        var result = batchService.submitChatRequestsAndFetch(chatRequests);
        assertEquals("Rome", result.get(0).response().body().toAssistantMessage().content());
        assertEquals("Paris", result.get(1).response().body().toAssistantMessage().content());
        assertEquals("Berlin", result.get(2).response().body().toAssistantMessage().content());
    }

    @Test
    void should_list_batches() throws Exception {
        var batches = batchService.list();
        assertTrue(batches.data().size() > 0);
    }
}
