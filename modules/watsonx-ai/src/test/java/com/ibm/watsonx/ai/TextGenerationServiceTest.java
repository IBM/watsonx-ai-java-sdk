/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import com.ibm.watsonx.ai.textgeneration.Moderation;
import com.ibm.watsonx.ai.textgeneration.Moderation.InputRanges;
import com.ibm.watsonx.ai.textgeneration.Moderation.TextModeration;
import com.ibm.watsonx.ai.textgeneration.TextGenerationHandler;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters.ReturnOptions;
import com.ibm.watsonx.ai.textgeneration.TextGenerationRequest;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.ModerationRange;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.ModerationResult;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.ModerationResults;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.Result;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.TokenInfo;
import com.ibm.watsonx.ai.textgeneration.TextGenerationResponse.TopTokenInfo;
import com.ibm.watsonx.ai.textgeneration.TextGenerationService;
import com.ibm.watsonx.ai.textgeneration.TextRequest;
import com.ibm.watsonx.ai.utils.HttpUtils;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TextGenerationServiceTest extends AbstractWatsonxTest {

    @BeforeEach
    void setUp() {
        when(mockAuthenticator.token()).thenReturn("my-super-token");
        resetHttpClient();
    }

    @Test
    void should_serialize_parameters_correctly() throws Exception {

        var EXPECTED = """
            {
              "decoding_method": "sample",
              "length_penalty": {
                "decay_factor": 2.5,
                "start_index": 5
              },
              "max_new_tokens": 30,
              "min_new_tokens": 5,
              "random_seed": 42,
              "stop_sequences": [
                "\\n\\n",
                "END"
              ],
              "temperature": 1.2,
              "time_limit": 60000,
              "top_k": 50,
              "top_p": 0.9,
              "repetition_penalty": 1.3,
              "truncate_input_tokens": 512,
              "return_options": {
                "input_text": true,
                "generated_text": true,
                "input_tokens": true,
                "token_logprobs": true,
                "token_ranks": true,
                "top_n_tokens": 3
              },
              "include_stop_sequence": false
            }""";

        var parameters = TextGenerationParameters.builder()
            .decodingMethod("sample")
            .lengthPenalty(2.5, 5)
            .maxNewTokens(30)
            .minNewTokens(5)
            .randomSeed(42)
            .repetitionPenalty(1.3)
            .returnOptions(
                ReturnOptions.builder()
                    .inputText(true)
                    .generatedText(true)
                    .inputTokens(true)
                    .tokenLogprobs(true)
                    .tokenRanks(true)
                    .topNTokens(3)
                    .build()
            )
            .stopSequences(List.of("\n\n", "END"))
            .temperature(1.2)
            .timeLimit(Duration.ofMinutes(1))
            .topK(50)
            .topP(0.9)
            .truncateInputTokens(512)
            .includeStopSequence(false)
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(parameters), true);
    }

    @Test
    void should_serialize_moderation_parameters_correctly() throws Exception {

        var EXPECTED = """
            {
                "hap": {
                  "input": {
                    "enabled": true,
                    "threshold": 0.8
                  },
                  "output": {
                    "enabled": true,
                    "threshold": 0.9
                  },
                  "mask": {
                    "remove_entity_value": true
                  }
                },
                "pii": {
                  "input": {
                    "enabled": true
                  },
                  "output": {
                    "enabled": false
                  },
                  "mask": {
                    "remove_entity_value": false
                  }
                },
                "granite_guardian": {
                  "input": {
                    "enabled": true,
                    "threshold": 0.85
                  },
                  "mask": {
                    "remove_entity_value": true
                  }
                },
                "input_ranges": [
                  {
                    "start": 0,
                    "end": 50
                  },
                  {
                    "start": 100,
                    "end": 150
                  }
                ]
            }""";

        var moderation = Moderation.builder()
            .hap(TextModeration.of(0.8f), TextModeration.of(0.9f), true)
            .pii(true, false, false)
            .graniteGuardian(TextModeration.of(0.85f), true)
            .inputRanges(List.of(InputRanges.of(0, 50), InputRanges.of(100, 150)))
            .build();

        JSONAssert.assertEquals(EXPECTED, Json.toJson(moderation), true);
    }

    @Test
    void should_serialize_response_correctly() throws Exception {

        var EXPECTED = """
            {
              "model_id": "google/flan-ul2",
              "created_at": "2025-06-24T15:30:00Z",
              "model_version": "1.2.3",
              "results": [
                {
                  "generated_text": "Swimwear Unlimited - Mid-Summer Sale!",
                  "stop_reason": "token_limit",
                  "generated_token_count": 3,
                  "input_token_count": 11,
                  "seed": 42,
                  "generated_tokens": [
                    {
                      "text": "Swimwear",
                      "logprob": -0.123,
                      "rank": 1,
                      "top_tokens": {
                        "text": "Swimwear",
                        "logprob": -0.123
                      }
                    },
                    {
                      "text": " Unlimited",
                      "logprob": -0.234,
                      "rank": 2,
                      "top_tokens": {
                        "text": " Unlimited",
                        "logprob": -0.234
                      }
                    },
                    {
                      "text": " - Mid-Summer Sale!",
                      "logprob": -0.345,
                      "rank": 3,
                      "top_tokens": {
                        "text": " - Mid-Summer Sale!",
                        "logprob": -0.345
                      }
                    }
                  ],
                  "input_tokens": [
                    {
                      "text": "Check",
                      "logprob": -0.111,
                      "rank": 1,
                      "top_tokens": {
                        "text": "Check",
                        "logprob": -0.111
                      }
                    },
                    {
                      "text": " out",
                      "logprob": -0.222,
                      "rank": 2,
                      "top_tokens": {
                        "text": " out",
                        "logprob": -0.222
                      }
                    }
                  ],
                  "moderations": {
                    "hap": {
                      "score": 0.01,
                      "input": false,
                      "position": {
                        "start": 0,
                        "end": 9
                      },
                      "entity": "Profanity",
                      "word": "Swimwear"
                    },
                    "pii": {
                      "score": 0.05,
                      "input": true,
                      "position": {
                        "start": 20,
                        "end": 30
                      },
                      "entity": "EmailAddress",
                      "word": "example@email.com"
                    },
                    "granite_guardian": {
                      "score": 0.9,
                      "input": false,
                      "position": {
                        "start": 15,
                        "end": 28
                      },
                      "entity": "SensitivePhrase",
                      "word": "Mid-Summer Sale"
                    }
                  }
                }
              ]
            }""";

        var textGenerationResponse = new TextGenerationResponse(
            "google/flan-ul2",
            "2025-06-24T15:30:00Z",
            "1.2.3",
            List.of(
                new Result("Swimwear Unlimited - Mid-Summer Sale!", "token_limit", 3, 11, 42,
                    List.of(
                        new TokenInfo("Swimwear", -0.123, 1, new TopTokenInfo("Swimwear", -0.123)),
                        new TokenInfo(" Unlimited", -0.234, 2, new TopTokenInfo(" Unlimited", -0.234)),
                        new TokenInfo(" - Mid-Summer Sale!", -0.345, 3, new TopTokenInfo(" - Mid-Summer Sale!", -0.345))
                    ),
                    List.of(
                        new TokenInfo("Check", -0.111, 1, new TopTokenInfo("Check", -0.111)),
                        new TokenInfo(" out", -0.222, 2, new TopTokenInfo(" out", -0.222))
                    ),
                    new ModerationResults(
                        new ModerationResult(0.01f, false, new ModerationRange(0, 9), "Profanity", "Swimwear"),
                        new ModerationResult(0.05f, true, new ModerationRange(20, 30), "EmailAddress", "example@email.com"),
                        new ModerationResult(0.9f, false, new ModerationRange(15, 28), "SensitivePhrase", "Mid-Summer Sale")
                    )
                )
            )
        );

        JSONAssert.assertEquals(EXPECTED, Json.toJson(textGenerationResponse), true);
    }

    @Test
    void should_override_parameters_correctly() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{}");
        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {

            var textGenerationService = TextGenerationService.builder()
                .authenticator(mockAuthenticator)
                .modelId("model")
                .projectId("project-id")
                .spaceId("space-id")
                .timeout(Duration.ofSeconds(1))
                .baseUrl(CloudRegion.DALLAS)
                .build();

            var response = textGenerationService.generate("Hello!");
            assertNotNull(response);

            var expected = new TextRequest("model", "space-id", "project-id", "Hello!",
                TextGenerationParameters.builder().timeLimit(Duration.ofSeconds(1)).build(), null);

            JSONAssert.assertEquals(Json.toJson(expected), HttpUtils.bodyPublisherToString(mockHttpRequest), true);

            response = textGenerationService.generate("Hello!", TextGenerationParameters.builder()
                .modelId("new-model")
                .spaceId("new-space-id")
                .projectId("new-project-id")
                .timeLimit(Duration.ofSeconds(2))
                .transactionId("my-transaction-id")
                .promptVariables(Map.of("test", "test")) // this field must be ignored
                .build());

            expected = new TextRequest("new-model", "new-space-id", "new-project-id", "Hello!",
                TextGenerationParameters.builder().timeLimit(Duration.ofSeconds(2)).build(), null);

            JSONAssert.assertEquals(Json.toJson(expected), HttpUtils.bodyPublisherToString(mockHttpRequest), false);
            assertFalse(HttpUtils.bodyPublisherToString(mockHttpRequest).contains("prompt_variables"));
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }

    @Test
    void should_send_moderation_request_correctly() throws Exception {

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{}");
        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
            .thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var textGenerationService = TextGenerationService.builder()
                .authenticator(mockAuthenticator)
                .modelId("model")
                .projectId("project-id")
                .spaceId("space-id")
                .timeout(Duration.ofSeconds(1))
                .baseUrl(CloudRegion.DALLAS)
                .build();

            var moderation = Moderation.builder()
                .hap(TextModeration.of(0.85f), TextModeration.of(0.9f), true)
                .pii(true, false, false)
                .graniteGuardian(TextModeration.of(0.95f), true)
                .inputRanges(List.of(InputRanges.of(0, 100)))
                .build();

            var response = textGenerationService.generate("Hello!", moderation);
            assertNotNull(response);

            var expected = new TextRequest("model", "space-id", "project-id", "Hello!",
                TextGenerationParameters.builder().timeLimit(Duration.ofSeconds(1)).build(), moderation);

            JSONAssert.assertEquals(Json.toJson(expected), HttpUtils.bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_throw_exceptions_for_invalid_requests() throws Exception {

        var textGenerationService = TextGenerationService.builder()
            .authenticator(mockAuthenticator)
            .modelId("model")
            .projectId("project-id")
            .spaceId("space-id")
            .timeout(Duration.ofSeconds(1))
            .baseUrl(CloudRegion.DALLAS)
            .build();

        var ex = assertThrows(NullPointerException.class, () -> textGenerationService.generate(TextGenerationRequest.builder().build()));
        assertEquals(ex.getMessage(), "input cannot be null");

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
            .thenThrow(IOException.class);

        assertThrows(RuntimeException.class, () -> textGenerationService.generate("Hello"));

        when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
            .thenThrow(InterruptedException.class);

        assertThrows(RuntimeException.class, () -> textGenerationService.generate("Hello"));
    }

    @Test
    void should_stream_text_generation_correctly() throws Exception {

        final String VERSION = "2020-03-15";
        var httpPort = wireMock.getPort();

        wireMock.stubFor(post("/ml/v1/text/generation_stream?version=%s".formatted(VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .withRequestBody(equalToJson(
                """
                      {
                      "model_id": "ibm/granite-13b-instruct-v2",
                      "input": "<|system|>\\nYou are a translation assistant. Your job is to translate any input from the user into English. Do not explain the translation. Just output the translated text.<|user|>\\nTraduci in inglese: \\"Oggi è una bella giornata\\"<|assistant|>\\n",
                      "project_id": "63dc4cf1-252f-424b-b52d-5cdd9814987f",
                      "parameters": {
                          "time_limit": 60000
                      }
                    }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(8, 200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.500Z","results":[{"generated_text":"","generated_token_count":0,"input_token_count":68,"stop_reason":"not_finished"}],"system":{"warnings":[{"message":"Model 'ibm/granite-13b-instruct-v2' is in deprecated state from 2025-06-18. It will be in withdrawn state from 2025-10-15. IDs of alternative models: ibm/granite-3-3-8b-instruct.","id":"deprecation_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-model-lifecycle.html?context=wx&audience=wdp"},{"message":"This API is legacy. Please consider using '/ml/v1/text/chat_stream' instead.","id":"api_legacy"}]}}

                        id: 2
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.516Z","results":[{"generated_text":"Toda","generated_token_count":1,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 3
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.528Z","results":[{"generated_text":"y i","generated_token_count":2,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 4
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.540Z","results":[{"generated_text":"s ","generated_token_count":3,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 5
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.552Z","results":[{"generated_text":"a beautifu","generated_token_count":4,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 6
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.564Z","results":[{"generated_text":"l da","generated_token_count":5,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 7
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.576Z","results":[{"generated_text":"y","generated_token_count":6,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 8
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.588Z","results":[{"generated_text":".","generated_token_count":7,"input_token_count":0,"stop_reason":"eos_token"}]}
                        """)));


        when(mockAuthenticator.asyncToken()).thenReturn(completedFuture("my-super-token"));

        var textGenerationService = TextGenerationService.builder()
            .authenticator(mockAuthenticator)
            .modelId("ibm/granite-13b-instruct-v2")
            .logResponses(true)
            .projectId("63dc4cf1-252f-424b-b52d-5cdd9814987f")
            .baseUrl(URI.create("http://localhost:%s".formatted(httpPort)))
            .version(VERSION)
            .build();


        CompletableFuture<TextGenerationResponse> result = new CompletableFuture<>();
        textGenerationService.generateStreaming(
            "<|system|>\nYou are a translation assistant. Your job is to translate any input from the user into English. Do not explain the translation. Just output the translated text.<|user|>\nTraduci in inglese: \"Oggi è una bella giornata\"<|assistant|>\n",
            TextGenerationParameters.builder().transactionId("my-transaction-id").promptVariables(Map.of("test", "test")).build(), // This field must
                                                                                                                                   // be ignored
            new TextGenerationHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {

                    assertTrue(
                        partialResponse.equals("Toda") || partialResponse.equals("y i") ||
                            partialResponse.equals("s ") || partialResponse.equals("a beautifu") ||
                            partialResponse.equals("l da") || partialResponse.equals("y") || partialResponse.equals(".")
                    );
                }

                @Override
                public void onCompleteResponse(TextGenerationResponse completeResponse) {
                    result.complete(completeResponse);
                }

                @Override
                public void onError(Throwable error) {
                    result.completeExceptionally(error);
                }
            });

        TextGenerationResponse response = assertDoesNotThrow(() -> result.get(113, TimeUnit.SECONDS));
        assertNotNull(response);
        assertEquals("Today is a beautiful day.", response.toText());
        assertEquals("ibm/granite-13b-instruct-v2", response.modelId());
        assertEquals(28, response.results().get(0).generatedTokenCount());
        assertEquals(68, response.results().get(0).inputTokenCount());
    }

    @Test
    void should_use_correct_executors() throws Exception {

        wireMock.stubFor(post("/ml/v1/text/generation_stream?version=2025-12-05")

            .willReturn(aResponse()
                .withStatus(200)
                .withChunkedDribbleDelay(8, 200)
                .withBody(
                    """
                        id: 1
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.500Z","results":[{"generated_text":"","generated_token_count":0,"input_token_count":68,"stop_reason":"not_finished"}],"system":{"warnings":[{"message":"Model 'ibm/granite-13b-instruct-v2' is in deprecated state from 2025-06-18. It will be in withdrawn state from 2025-10-15. IDs of alternative models: ibm/granite-3-3-8b-instruct.","id":"deprecation_warning","more_info":"https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-model-lifecycle.html?context=wx&audience=wdp"},{"message":"This API is legacy. Please consider using '/ml/v1/text/chat_stream' instead.","id":"api_legacy"}]}}

                        id: 2
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.516Z","results":[{"generated_text":"Toda","generated_token_count":1,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 3
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.528Z","results":[{"generated_text":"y i","generated_token_count":2,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 4
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.540Z","results":[{"generated_text":"s ","generated_token_count":3,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 5
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.552Z","results":[{"generated_text":"a beautifu","generated_token_count":4,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 6
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.564Z","results":[{"generated_text":"l da","generated_token_count":5,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 7
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.576Z","results":[{"generated_text":"y","generated_token_count":6,"input_token_count":0,"stop_reason":"not_finished"}]}

                        id: 8
                        event: message
                        data: {"model_id":"ibm/granite-13b-instruct-v2","created_at":"2025-06-24T15:30:13.588Z","results":[{"generated_text":".","generated_token_count":7,"input_token_count":0,"stop_reason":"eos_token"}]}
                        """)));

        List<String> threadNames = new ArrayList<>();

        when(mockAuthenticator.asyncToken()).thenReturn(completedFuture("my-token"));

        Executor ioExecutor = Executors.newSingleThreadExecutor(r -> new Thread(() -> {
            threadNames.add(Thread.currentThread().getName());
            r.run();
        }, "my-thread"));

        try (MockedStatic<ExecutorProvider> mockedStatic = mockStatic(ExecutorProvider.class)) {
            mockedStatic.when(ExecutorProvider::ioExecutor).thenReturn(ioExecutor);
            var textGenerationService = TextGenerationService.builder()
                .authenticator(mockAuthenticator)
                .modelId("ibm/granite-13b-instruct-v2")
                .projectId("project-id")
                .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
                .build();

            CompletableFuture<TextGenerationResponse> result = new CompletableFuture<>();
            textGenerationService.generateStreaming("test",
                new TextGenerationHandler() {

                    @Override
                    public void onPartialResponse(String partialResponse) {
                        assertEquals("my-thread", Thread.currentThread().getName());
                        assertTrue(
                            partialResponse.equals("Toda") || partialResponse.equals("y i") ||
                                partialResponse.equals("s ") || partialResponse.equals("a beautifu") ||
                                partialResponse.equals("l da") || partialResponse.equals("y") || partialResponse.equals(".")
                        );
                    }

                    @Override
                    public void onCompleteResponse(TextGenerationResponse completeResponse) {
                        assertEquals("my-thread", Thread.currentThread().getName());
                        result.complete(completeResponse);
                    }

                    @Override
                    public void onError(Throwable error) {
                        fail(error);
                    }
                });


            result.get(3, TimeUnit.SECONDS);
            assertEquals(1, threadNames.size());
            assertEquals("my-thread", threadNames.get(0));
        }
    }
}
