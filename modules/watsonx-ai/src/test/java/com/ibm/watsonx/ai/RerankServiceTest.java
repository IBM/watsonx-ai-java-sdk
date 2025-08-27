/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.ibm.watsonx.ai.WatsonxService.TRANSACTION_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.rerank.RerankParameters;
import com.ibm.watsonx.ai.rerank.RerankService;
import com.ibm.watsonx.ai.utils.Utils;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class RerankServiceTest extends AbstractWatsonxTest {

    @BeforeEach
    void setUp() {
        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
    }

    @Test
    void test_rerank() throws Exception {

        final String REQUEST =
            """
                {
                  "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
                  "project_id": "12ac4cf1-252f-424b-b52d-5cdd9814987f",
                  "inputs": [
                    {
                      "text": "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine."
                    },
                    {
                      "text": "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
                    }
                  ],
                  "query": "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit."
                }""";

        final String RESPONSE = """
            {
              "model_id": "cross-encoder/ms-marco-minilm-l-12-v2",
              "results": [
                {
                  "index": 1,
                  "score": 0.7461
                },
                {
                  "index": 0,
                  "score": 0.8274
                }
              ],
              "created_at": "2024-02-21T17:32:28Z",
              "input_token_count": 20
            }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var rerankService = RerankService.builder()
                .url(CloudRegion.LONDON)
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
                .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
                .build();

            var response = rerankService.rerank(
                "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
                List.of(
                    "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine.",
                    "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
                )
            );

            JSONAssert.assertEquals(REQUEST, Utils.bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void test_rerank_parameters() throws Exception {

        final String REQUEST =
            """
                {
                  "model_id": "my-new-model-id",
                  "project_id": "my-new-project-id",
                  "space_id": "my-new-space-id",
                  "inputs": [
                    {
                      "text": "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine."
                    },
                    {
                      "text": "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
                    }
                  ],
                  "query": "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
                  "parameters": {
                    "truncate_input_tokens": 512,
                    "return_options": {
                        "top_n": 2,
                        "query": true,
                        "inputs": true
                    }
                  }
                }""";

        final String RESPONSE =
            """
                {
                    "model_id": "my-new-model-id",
                    "created_at": "2025-06-15T14:15:09.622Z",
                    "results": [
                        {
                            "index": 0,
                            "score": 0.2686532437801361,
                            "input": {
                                "text": "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine."
                            }
                        },
                        {
                            "index": 1,
                            "score": -1.575758934020996,
                            "input": {
                                "text": "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
                            }
                        }
                    ],
                    "query": "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
                    "input_token_count": 130
                }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var rerankService = RerankService.builder()
                .url(CloudRegion.LONDON)
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
                .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
                .build();

            var parameters = RerankParameters.builder()
                .projectId("my-new-project-id")
                .spaceId("my-new-space-id")
                .modelId("my-new-model-id")
                .transactionId("my-transaction-id")
                .query(true)
                .topN(2)
                .inputs(true)
                .truncateInputTokens(512)
                .build();

            assertEquals(true, parameters.getQuery());
            assertEquals(2, parameters.getTopN());
            assertEquals(true, parameters.getInputs());
            assertEquals(512, parameters.getTruncateInputTokens());

            var response = rerankService.rerank(
                "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
                List.of(
                    "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine.",
                    "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
                ),
                parameters
            );

            JSONAssert.assertEquals(REQUEST, Utils.bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
            assertEquals(mockHttpRequest.getValue().headers().firstValue(TRANSACTION_ID_HEADER).orElse(null), "my-transaction-id");
        });
    }

    @Test
    void test_rerank_parameters_2() throws Exception {

        final String REQUEST =
            """
                {
                  "model_id": "my-new-model-id",
                  "project_id": "my-new-project-id",
                  "space_id": "my-new-space-id",
                  "inputs": [
                    {
                      "text": "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine."
                    },
                    {
                      "text": "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
                    }
                  ],
                  "query": "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
                  "parameters": {
                    "truncate_input_tokens": 512,
                    "return_options": {
                        "inputs": true
                    }
                  }
                }""";

        final String RESPONSE =
            """
                {
                    "model_id": "my-new-model-id",
                    "created_at": "2025-06-15T14:15:09.622Z",
                    "results": [
                        {
                            "index": 0,
                            "score": 0.2686532437801361,
                            "input": {
                                "text": "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine."
                            }
                        },
                        {
                            "index": 1,
                            "score": -1.575758934020996,
                            "input": {
                                "text": "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
                            }
                        }
                    ],
                    "input_token_count": 130
                }""";

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(RESPONSE);
        when(mockHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class))).thenReturn(mockHttpResponse);

        withWatsonxServiceMock(() -> {
            var rerankService = RerankService.builder()
                .url(CloudRegion.LONDON)
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
                .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
                .build();

            var parameters = RerankParameters.builder()
                .projectId("my-new-project-id")
                .spaceId("my-new-space-id")
                .modelId("my-new-model-id")
                .inputs(true)
                .truncateInputTokens(512)
                .build();

            var response = rerankService.rerank(
                "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
                List.of(
                    "In my younger years, I often reveled in the excitement of spontaneous adventures and embraced the thrill of the unknown, whereas in my grownup life, I've come to appreciate the comforting stability of a well-established routine.",
                    "As a young man, I frequently sought out exhilarating experiences, craving the adrenaline rush of life's novelties, while as a responsible adult, I've come to understand the profound value of accumulated wisdom and life experience."
                ),
                parameters
            );

            JSONAssert.assertEquals(REQUEST, Utils.bodyPublisherToString(mockHttpRequest), true);
            JSONAssert.assertEquals(RESPONSE, Json.toJson(response), true);
        });
    }

    @Test
    void test_rerank_exeception() throws Exception {

        when(mockHttpClient.send(any(), any())).thenThrow(new IOException("error"));

        withWatsonxServiceMock(() -> {
            var rerankService = RerankService.builder()
                .url(CloudRegion.LONDON)
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("12ac4cf1-252f-424b-b52d-5cdd9814987f")
                .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
                .build();

            assertThrows(RuntimeException.class, () -> rerankService.rerank("test", List.of("test")));
        });
    }
}
