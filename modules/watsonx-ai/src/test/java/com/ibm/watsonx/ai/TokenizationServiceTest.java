/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpResponse.BodyHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;
import com.ibm.watsonx.ai.tokenization.TokenizationParameters;
import com.ibm.watsonx.ai.tokenization.TokenizationService;
import com.ibm.watsonx.ai.utils.HttpUtils;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TokenizationServiceTest extends AbstractWatsonxTest {

    @BeforeEach
    void setUp() {
        when(mockAuthenticator.token()).thenReturn("my-super-token");
        when(mockAuthenticator.asyncToken()).thenReturn(completedFuture("my-super-token"));
    }

    @Test
    void should_tokenize_text_synchronously() throws Exception {

        final String REQUEST = """
            {
              "model_id": "google/flan-ul2",
              "input": "Write a tagline for an alumni association: Together we",
              "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f"
            }""";

        final String RESPONSE = """
            {
              "model_id": "google/flan-ul2",
              "result": {
                "token_count": 11
              }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var tokenizationService = TokenizationService.builder()
                .baseUrl(CloudRegion.LONDON)
                .authenticator(mockAuthenticator)
                .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
                .modelId("google/flan-ul2")
                .build();

            var response = tokenizationService.tokenize("Write a tagline for an alumni association: Together we");

            JSONAssert.assertEquals(REQUEST, HttpUtils.bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_tokenize_text_asynchronously() throws Exception {

        final String REQUEST = """
            {
              "model_id": "google/flan-ul2",
              "input": "Write a tagline for an alumni association: Together we",
              "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f"
            }""";

        final String RESPONSE = """
            {
              "model_id": "google/flan-ul2",
              "result": {
                "token_count": 11
              }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockHttpClient.sendAsync(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(completedFuture(mockHttpResponse));

        withWatsonxServiceMock(() -> {
            var tokenizationService = TokenizationService.builder()
                .baseUrl(CloudRegion.LONDON)
                .authenticator(mockAuthenticator)
                .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
                .modelId("google/flan-ul2")
                .build();

            try {
                var response = tokenizationService.asyncTokenize("Write a tagline for an alumni association: Together we").get();
                JSONAssert.assertEquals(REQUEST, HttpUtils.bodyPublisherToString(mockHttpRequest), true);
                JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_tokenize_text_with_parameter() throws Exception {

        final String REQUEST = """
            {
              "model_id": "google/flan-ul2",
              "input": "Write a tagline for an alumni association: Together we",
              "parameters": {
                "return_tokens": true
              },
              "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f"
            }""";

        final String RESPONSE = """
            {
              "model_id": "google/flan-ul2",
              "result": {
                "token_count": 11,
                "tokens": [
                  "Write",
                  "a",
                  "tag",
                  "line",
                  "for",
                  "an",
                  "alumni",
                  "associ",
                  "ation:",
                  "Together",
                  "we"
                ]
              }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var tokenizationService = TokenizationService.builder()
                .baseUrl(CloudRegion.LONDON)
                .authenticator(mockAuthenticator)
                .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
                .modelId("google/flan-ul2")
                .build();

            var parameters = TokenizationParameters.builder()
                .returnTokens(true)
                .transactionId("my-transaction-id")
                .build();

            assertEquals(true, parameters.returnTokens());

            var response = tokenizationService.tokenize(
                "Write a tagline for an alumni association: Together we",
                parameters
            );

            JSONAssert.assertEquals(REQUEST, HttpUtils.bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }

    @Test
    void should_override_default_parameters() throws Exception {

        final String REQUEST = """
            {
              "model_id": "my-model-id",
              "input": "Write a tagline for an alumni association: Together we",
              "project_id": "my-project-id",
              "space_id": "my-space-id"
            }""";

        final String RESPONSE = """
            {
              "model_id": "my-model-id",
              "result": {
                "token_count": 11
              }
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var tokenizationService = TokenizationService.builder()
                .baseUrl(CloudRegion.LONDON)
                .authenticator(mockAuthenticator)
                .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
                .modelId("google/flan-ul2")
                .build();

            var parameters = TokenizationParameters.builder()
                .modelId("my-model-id")
                .projectId("my-project-id")
                .spaceId("my-space-id")
                .build();

            var response = tokenizationService.tokenize(
                "Write a tagline for an alumni association: Together we",
                parameters
            );

            JSONAssert.assertEquals(REQUEST, HttpUtils.bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void should_throw_runtime_exception_when_tokenization_fails() throws Exception {

        when(mockHttpClient.send(any(), any())).thenThrow(new IOException("error"));

        var tokenizationService = TokenizationService.builder()
            .baseUrl(CloudRegion.LONDON)
            .authenticator(mockAuthenticator)
            .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
            .modelId("google/flan-ul2")
            .build();

        assertThrows(RuntimeException.class, () -> tokenizationService.tokenize("test"));
    }

    @Test
    void should_use_correct_executors() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
            {
              "model_id": "google/flan-ul2",
              "result": {
                "token_count": 11
              }
            }""");

        List<String> threadNames = new ArrayList<>();

        Executor cpuExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(() -> {
            threadNames.add(Thread.currentThread().getName());
            r.run();
        }, "cpu-thread"));

        Executor ioExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(() -> {
            threadNames.add(Thread.currentThread().getName());
            r.run();
        }, "io-thread"));

        try (MockedStatic<ExecutorProvider> mockedStatic = mockStatic(ExecutorProvider.class)) {
            mockedStatic.when(ExecutorProvider::cpuExecutor).thenReturn(cpuExecutor);
            mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(ioExecutor);

            withWatsonxServiceMock(() -> {

                when(mockHttpClient.sendAsync(any(), any(BodyHandler.class)))
                    .thenReturn(completedFuture(mockHttpResponse));

                var tokenizationService = TokenizationService.builder()
                    .baseUrl(CloudRegion.LONDON)
                    .authenticator(mockAuthenticator)
                    .projectId("project-id")
                    .modelId("model-id")
                    .logRequests(true)
                    .logResponses(true)
                    .build();

                try {
                    tokenizationService.asyncTokenize("input")
                        .thenRunAsync(() -> threadNames.add(Thread.currentThread().getName()), ioExecutor)
                        .thenRunAsync(() -> threadNames.add(Thread.currentThread().getName()), cpuExecutor)
                        .get(3, TimeUnit.SECONDS);

                    assertEquals(4, threadNames.size());
                    assertEquals("io-thread", threadNames.get(0));
                    assertEquals("cpu-thread", threadNames.get(1));
                    assertEquals("io-thread", threadNames.get(2));
                    assertEquals("cpu-thread", threadNames.get(3));
                } catch (Exception e) {
                    fail(e);
                }
            });
        }
    }
}
