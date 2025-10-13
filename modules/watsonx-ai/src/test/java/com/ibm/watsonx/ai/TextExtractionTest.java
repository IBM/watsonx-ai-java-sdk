/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type.HTML;
import static com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type.JSON;
import static com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type.MD;
import static com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type.PAGE_IMAGES;
import static com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type.PLAIN_TEXT;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;
import com.ibm.watsonx.ai.textextraction.CosReference;
import com.ibm.watsonx.ai.textextraction.TextExtractionDeleteParameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionException;
import com.ibm.watsonx.ai.textextraction.TextExtractionFetchParameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.CosUrl;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.EmbeddedImageMode;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.KvpMode;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Language;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Mode;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.OcrMode;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.CosDataConnection;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.CosDataLocation;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.DataReference;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.KvpField;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.Parameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.Schema;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.SemanticConfig;
import com.ibm.watsonx.ai.textextraction.TextExtractionResponse;
import com.ibm.watsonx.ai.textextraction.TextExtractionResponse.Entity;
import com.ibm.watsonx.ai.textextraction.TextExtractionResponse.Error;
import com.ibm.watsonx.ai.textextraction.TextExtractionResponse.ExtractionResult;
import com.ibm.watsonx.ai.textextraction.TextExtractionResponse.Metadata;
import com.ibm.watsonx.ai.textextraction.TextExtractionService;
import com.ibm.watsonx.ai.textextraction.TextExtractionUtils;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class TextExtractionTest extends AbstractWatsonxTest {

    private final String BUCKET_NAME = "my-bucket";
    private final String FILE_NAME = "test.pdf";
    private final String PROCESS_EXTRACTION_ID = "my-id";
    private final String TEXT_EXTRACTION_RESPONSE = """
        {
          "metadata": {
              "id": "%s",
              "created_at": "2023-05-02T16:27:51Z",
              "project_id": "<project-id>"
          },
          "entity": {
              "document_reference": {
                "type": "connection_asset",
                "connection": {
                  "id": "<connection-id>"
                },
                "location": {
                  "file_name": "%s",
                  "bucket": "%s"
                }
              },
            "results_reference": {
              "type": "connection_asset",
              "connection": {
                "id": "<connection-id>"
              },
              "location": {
                "file_name": "%s",
                "bucket": "%s"
              }
            },
            "results": {
              "status": "%s",
              "number_pages_processed": 1,
              "running_at": "2023-05-02T16:28:03Z",
              "completed_at": "2023-05-02T16:28:03Z"
            }
          }
        }""";

    private final String TEXT_EXTRACTION_FAIL_RESPONSE = """
        {
              "metadata": {
                "id": "%s",
                "created_at": "2023-05-02T16:27:51Z",
                "project_id": "<project-id>",
                "name": "extract"
              },
              "entity": {
                "document_reference": {
                  "type": "connection_asset",
                  "connection": {
                     "id": "<connection-id>"
                  },
                  "location": {
                    "file_name": "%s"
                  }
                },
                "results_reference": {
                  "type": "connection_asset",
                  "connection": {
                     "id": "<connection-id>"
                  },
                  "location": {
                    "file_name": "%s"
                  }
                },
                "results": {
                    "error": {
                        "code": "file_download_error",
                        "message": "error message"
                    },
                    "number_pages_processed": 0,
                    "status": "failed"
                }
              }
            }""";


    @RegisterExtension
    WireMockExtension cosServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort().http2PlainDisabled(true))
        .build();

    @RegisterExtension
    WireMockExtension watsonxServer = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
        .build();

    TextExtractionService textExtractionService;

    @BeforeEach
    void beforeAll() {
        textExtractionService = TextExtractionService.builder()
            .baseUrl("http://localhost:%s".formatted(watsonxServer.getPort()))
            .cosUrl("http://localhost:%s".formatted(cosServer.getPort()))
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("projectid")
            .documentReference("<connection_id>", BUCKET_NAME)
            .resultReference("<connection_id>", BUCKET_NAME)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    @Test
    void test_text_extraction_parameters() throws Exception {

        var EXPECTED = """
            {
                "metadata": {
                    "id": "abc123",
                    "created_at": "2025-06-25T10:15:30Z",
                    "space_id": "3fc54cf1-252f-424b-b52d-5cdd9814987f",
                    "project_id": "3fc54cf1-252f-424b-b52d-5cdd9814987f"
                },
                "entity": {
                    "document_reference": {
                      "type": "connection_asset",
                      "connection": {
                          "id": "conn-456"
                      },
                      "location": {
                          "file_name": "document.pdf",
                          "bucket": "my-bucket"
                      }
                    },
                    "results_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "conn-456"
                        },
                        "location": {
                            "file_name": "/results/",
                            "bucket": "my-results-bucket"
                        }
                    },
                    "results": {
                        "status": "completed",
                        "number_pages_processed": 5,
                        "running_at": "2025-06-25T10:16:00Z",
                        "completed_at": "2025-06-25T10:18:00Z",
                        "total_pages": 5,
                        "location": [
                            "cos://my-results-bucket/results/output1.txt",
                            "cos://my-results-bucket/results/output2.txt"
                        ],
                        "error": {
                            "code": "NONE",
                            "message": "No error",
                            "more_info": "more info"
                        }
                    },
                    "parameters": {
                        "requested_outputs": ["plain_text", "html"],
                        "mode": "standard",
                        "ocr_mode": "enabled",
                        "languages": ["en", "it"],
                        "auto_rotation_correction": true,
                        "create_embedded_images": "enabled_text",
                        "output_dpi": 150,
                        "output_tokens": true,
                        "kvp_mode": "generic_with_semantic",
                        "semantic_config": {
                            "target_image_width": 1024,
                            "enable_text_hints": true,
                            "enable_generic_kvp": true,
                            "schemas": [
                                {
                                    "document_type": "Invoice",
                                    "document_description": "Invoice with totals and billing details",
                                    "target_image_width": 1024,
                                    "enable_text_hints": true,
                                    "enable_generic_kvp": true,
                                    "fields": {
                                      "description": "description",
                                      "example": "example"
                                    }
                                }
                            ]
                        }
                    },
                    "custom": {
                        "name": "model",
                        "size": 2
                    }
                }
            }""";

        Metadata metadata =
            new Metadata("abc123", "2025-06-25T10:15:30Z", null, "3fc54cf1-252f-424b-b52d-5cdd9814987f", "3fc54cf1-252f-424b-b52d-5cdd9814987f");

        DataReference documentReference =
            new DataReference("connection_asset", new CosDataConnection("conn-456"), new CosDataLocation("document.pdf", "my-bucket"));

        DataReference resultReference =
            new DataReference("connection_asset", new CosDataConnection("conn-456"), new CosDataLocation("/results/", "my-results-bucket"));

        ExtractionResult result = new ExtractionResult("completed", 5, "2025-06-25T10:16:00Z", "2025-06-25T10:18:00Z", 5,
            List.of("cos://my-results-bucket/results/output1.txt", "cos://my-results-bucket/results/output2.txt"),
            new Error("NONE", "No error", "more info"));

        Schema schema = Schema.builder()
            .documentType("Invoice")
            .documentDescription("Invoice with totals and billing details")
            .targetImageWidth(1024)
            .enableGenericKvp(true)
            .enableTextHints(true)
            .fields(new KvpField("description", "example"))
            .build();

        SemanticConfig semanticConfig = SemanticConfig.builder()
            .targetImageWidth(1024)
            .enableGenericKvp(true)
            .enableTextHints(true)
            .schemas(List.of(schema))
            .build();

        Parameters parameters = new Parameters(List.of("plain_text", "html"), "standard", "enabled", List.of("en", "it"), true, "enabled_text", 150,
            true, "generic_with_semantic", semanticConfig);

        Entity entity = new Entity(documentReference, resultReference, result, parameters, Map.of("name", "model", "size", 2));
        JSONAssert.assertEquals(EXPECTED, Json.toJson(new TextExtractionResponse(metadata, entity)), true);

        TextExtractionParameters params = TextExtractionParameters.builder()
            .transactionId("my-transaction-id")
            .requestedOutputs(Type.PLAIN_TEXT, Type.HTML)
            .mode(Mode.STANDARD)
            .ocrMode(OcrMode.ENABLED)
            .languages(Language.ENGLISH, Language.ITALIAN)
            .autoRotationCorrection(true)
            .createEmbeddedImages(EmbeddedImageMode.ENABLED_TEXT)
            .outputDpi(150)
            .outputTokens(true)
            .kvpMode(KvpMode.GENERIC_WITH_SEMANTIC)
            .semanticConfig(
                SemanticConfig.builder()
                    .targetImageWidth(1024)
                    .enableTextHints(true)
                    .enableGenericKvp(true)
                    .schemas(List.of(
                        Schema.builder()
                            .documentType("Invoice")
                            .documentDescription("Invoice with totals and billing details")
                            .targetImageWidth(1024)
                            .enableTextHints(true)
                            .enableGenericKvp(true)
                            .fields(new KvpField("description", "example"))
                            .build()
                    )))
            .outputFileName("newfile")
            .removeOutputFile(true)
            .removeUploadedFile(true)
            .documentReference(CosReference.of("connection", "bucket"))
            .resultReference(CosReference.of("connection2", "bucket2"))
            .addCustomProperty("name", "model")
            .addCustomProperty("size", 2)
            .build();

        JSONAssert.assertEquals(Json.toJson(parameters), Json.toJson(params.toParameters()), true);
        assertEquals("newfile", params.getOutputFileName());
        assertTrue(params.isRemoveOutputFile());
        assertTrue(params.isRemoveUploadedFile());
        assertEquals(CosReference.of("connection", "bucket"), params.getDocumentReference());
        assertEquals(CosReference.of("connection2", "bucket2"), params.getResultReference());
    }

    @Test
    void test_start_extraction_without_parameters() throws Exception {

        var EXPECTED = """
              {
                "metadata": {
                    "id": "34f538a4-22d6-4d55-9bf9-98512ae7c256",
                    "created_at": "2025-06-27T12:08:59.582Z",
                    "project_id": "<project-id>"
                },
                "entity": {
                    "document_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "<connection-id>"
                        },
                        "location": {
                            "file_name": "myfile.pdf",
                            "bucket": "bucket"
                        }
                    },
                    "results_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "<connection-id>"
                        },
                        "location": {
                            "file_name": "myfile.md",
                            "bucket": "bucket"
                        }
                    },
                    "parameters": {
                        "requested_outputs": [
                            "md"
                        ],
                        "mode": "standard",
                        "languages": [
                            "it"
                        ],
                        "create_embedded_images": "disabled",
                        "output_dpi": 72,
                        "output_tokens": true
                    },
                    "results": {
                        "status": "submitted",
                        "number_pages_processed": 0
                    }
                }
            }""";

        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "<project-id>",
                    "document_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "<connection_id>"
                        },
                        "location": {
                            "file_name": "myfile.pdf",
                            "bucket": "bucket"
                        }
                    },
                    "results_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "<connection_id>"
                        },
                        "location": {
                            "file_name": "myfile.md",
                            "bucket": "bucket"
                        }
                    },
                    "parameters": {
                        "requested_outputs" : [ "md" ]
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(EXPECTED)
            )
        );

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var textExtractionService = TextExtractionService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("<project-id>")
            .baseUrl(URI.create("http://localhost:%s".formatted(watsonxServer.getPort())))
            .cosUrl(CosUrl.AU_SYD)
            .documentReference("<connection_id>", "bucket")
            .resultReference("<connection_id>", "bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        var response = textExtractionService.startExtraction("myfile.pdf");
        JSONAssert.assertEquals(EXPECTED, Json.toJson(response), true);
    }

    @Test
    void test_start_extraction_with_parameters() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
            .withRequestBody(equalToJson("""
                {
                    "project_id": "new-project-id",
                    "space_id": "new-space-id",
                    "document_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "my-new-connection"
                        },
                        "location": {
                            "file_name": "myfile.pdf",
                            "bucket": "my-new-bucket"
                        }
                    },
                    "results_reference": {
                        "type": "connection_asset",
                        "connection": {
                            "id": "my-new-connection-2"
                        },
                        "location": {
                            "file_name": "myfile.md",
                            "bucket": "my-new-bucket-2"
                        }
                    },
                    "parameters" : {
                      "requested_outputs" : [ "md" ]
                    },
                    "custom": {
                      "key": "value"
                    }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{}")
            ));


        var textExtractionService = TextExtractionService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("<project-id>")
            .baseUrl(URI.create("http://localhost:%s".formatted(watsonxServer.getPort())))
            .cosUrl(CosUrl.BR_SAO)
            .documentReference("<connection_id>", "bucket")
            .resultReference("<connection_id>", "bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        var parameters = TextExtractionParameters.builder()
            .projectId("new-project-id")
            .spaceId("new-space-id")
            .documentReference(CosReference.of("my-new-connection", "my-new-bucket"))
            .resultReference(CosReference.of("my-new-connection-2", "my-new-bucket-2"))
            .requestedOutputs(Type.MD)
            .addCustomProperty("key", "value")
            .transactionId("my-transaction-id")
            .build();

        var response = textExtractionService.startExtraction("myfile.pdf", parameters);
        assertNotNull(response);
    }

    @Test
    void test_text_extraction_page_images() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                  {
                      "project_id": "<project-id>",
                      "document_reference": {
                          "type": "connection_asset",
                          "connection": {
                              "id": "<connection_id>"
                          },
                          "location": {
                              "file_name": "0.png",
                              "bucket": "bucket"
                          }
                      },
                      "results_reference": {
                          "type": "connection_asset",
                          "connection": {
                              "id": "<connection_id>"
                          },
                          "location": {
                              "file_name": "/",
                              "bucket": "bucket"
                          }
                      },
                      "parameters": {
                          "requested_outputs": ["page_images"]
                      }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{}")
            ));

        var textExtractionService = TextExtractionService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("<project-id>")
            .cosUrl(CosUrl.CA_MON)
            .baseUrl(URI.create("http://localhost:%s".formatted(watsonxServer.getPort())))
            .documentReference("<connection_id>", "bucket")
            .resultReference("<connection_id>", "bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        var parameters = TextExtractionParameters.builder()
            .requestedOutputs(Type.PAGE_IMAGES)
            .build();

        var response = textExtractionService.startExtraction("0.png", parameters);
        assertNotNull(response);
    }

    @Test
    void test_text_extraction_multiple_outputs() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .withRequestBody(equalToJson("""
                  {
                      "project_id": "<project-id>",
                      "document_reference": {
                          "type": "connection_asset",
                          "connection": {
                              "id": "<connection_id>"
                          },
                          "location": {
                              "file_name": "0.png",
                              "bucket": "bucket"
                          }
                      },
                      "results_reference": {
                          "type": "connection_asset",
                          "connection": {
                              "id": "<connection_id>"
                          },
                          "location": {
                              "file_name": "/",
                              "bucket": "bucket"
                          }
                      },
                      "parameters": {
                          "requested_outputs": ["md", "assembly"]
                      }
                }"""))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{}")
            ));

        var textExtractionService = TextExtractionService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("<project-id>")
            .baseUrl(URI.create("http://localhost:%s".formatted(watsonxServer.getPort())))
            .cosUrl(CosUrl.CA_TOR)
            .documentReference("<connection_id>", "bucket")
            .resultReference("<connection_id>", "bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        var parameters = TextExtractionParameters.builder()
            .requestedOutputs(Type.MD, Type.JSON)
            .build();

        var response = textExtractionService.startExtraction("0.png", parameters);
        assertNotNull(response);
    }

    @Test
    void text_text_extraction_fetch() {

        var RESULT =
            """
                  {
                      "entity": {
                          "document_reference": {
                              "connection": {
                                  "id": "<connection_id>"
                              },
                              "location": {
                                  "bucket": "bucket",
                                  "file_name": "0.png"
                              },
                              "type": "connection_asset"
                          },
                          "parameters": {
                              "create_embedded_images": "disabled",
                              "languages": [
                                  "latn"
                              ],
                              "mode": "standard",
                              "output_dpi": 72,
                              "output_tokens": true,
                              "requested_outputs": [
                                  "md",
                                  "assembly"
                              ]
                          },
                          "results": {
                              "completed_at": "2025-06-27T14:02:32.606Z",
                              "location": [
                                  "test/assembly.json",
                                  "test/assembly.md"
                              ],
                              "number_pages_processed": 1,
                              "running_at": "2025-06-27T14:02:26.902Z",
                              "status": "completed"
                          },
                          "results_reference": {
                              "connection": {
                                  "id": "<connection_id>"
                              },
                              "location": {
                                  "bucket": "bucket",
                                  "file_name": "test/"
                              },
                              "type": "connection_asset"
                          }
                      },
                      "metadata": {
                          "created_at": "2025-06-27T14:02:25.402Z",
                          "id": "b3b85a66-7324-470c-b62e-75579eecf045",
                          "modified_at": "2025-06-27T14:02:32.633Z",
                          "project_id": "<project_id>"
                      }
                }""";

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        watsonxServer.stubFor(get("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=%s&project_id=%s"
            .formatted(API_VERSION, URLEncoder.encode("<project_id>", Charset.defaultCharset())))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(RESULT)
            ));

        var textExtractionService = TextExtractionService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("<project_id>")
            .baseUrl(URI.create("http://localhost:%s".formatted(watsonxServer.getPort())))
            .cosUrl(CosUrl.EU_DE)
            .documentReference("<connection_id>", "bucket")
            .resultReference("<connection_id>", "bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        var response = textExtractionService.fetchExtractionRequest("b3b85a66-7324-470c-b62e-75579eecf045");
        JSONAssert.assertEquals(RESULT, Json.toJson(response), true);

        var projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());
        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

        watsonxServer
            .stubFor(get("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")
                ));

        var parameters = TextExtractionFetchParameters.builder()
            .projectId("new-project-id")
            .transactionId("my-transaction-id")
            .build();

        response = textExtractionService.fetchExtractionRequest("b3b85a66-7324-470c-b62e-75579eecf045", parameters);
        assertNotNull(response);

        textExtractionService = TextExtractionService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .spaceId("<space_id>")
            .baseUrl(URI.create("http://localhost:%s".formatted(watsonxServer.getPort())))
            .cosUrl(CosUrl.JP_OSA)
            .documentReference("<connection_id>", "bucket")
            .resultReference("<connection_id>", "bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        watsonxServer
            .stubFor(get("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")
                ));

        parameters = TextExtractionFetchParameters.builder()
            .spaceId("new-space-id")
            .build();

        response = textExtractionService.fetchExtractionRequest("b3b85a66-7324-470c-b62e-75579eecf045", parameters);
        assertNotNull(response);
    }

    @Test
    void text_text_extraction_delete() {

        var projectId = URLEncoder.encode("<project_id>", Charset.defaultCharset());
        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        watsonxServer
            .stubFor(delete("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        var textExtractionService = TextExtractionService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .projectId("<project_id>")
            .baseUrl(URI.create("http://localhost:%s".formatted(watsonxServer.getPort())))
            .cosUrl(CosUrl.EU_ES)
            .documentReference("<connection_id>", "bucket")
            .resultReference("<connection_id>", "bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        assertTrue(textExtractionService.deleteRequest("b3b85a66-7324-470c-b62e-75579eecf045"));

        watsonxServer
            .stubFor(delete("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=%s&project_id=%s".formatted(API_VERSION, projectId))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withBody("""
                        {
                            "trace": "db2821f494a629c614616e458c85de36",
                            "errors": [
                                {
                                    "code": "text_extraction_event_does_not_exist",
                                    "message": "Text extraction request does not exist."
                                }
                            ]
                        }""")
                ));

        assertFalse(textExtractionService.deleteRequest("b3b85a66-7324-470c-b62e-75579eecf045"));

        projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());
        var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        watsonxServer
            .stubFor(
                delete("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=%s&project_id=%s".formatted(API_VERSION, projectId)
                    + "&hard_delete=true")
                    .withHeader("Authorization", equalTo("Bearer my-super-token"))
                    .withHeader(TRANSACTION_ID_HEADER, equalTo("my-transaction-id"))
                    .willReturn(aResponse()
                        .withStatus(204)
                    ));

        var parameters = TextExtractionDeleteParameters.builder()
            .projectId("new-project-id")
            .hardDelete(true)
            .transactionId("my-transaction-id")
            .build();

        assertTrue(textExtractionService.deleteRequest("b3b85a66-7324-470c-b62e-75579eecf045", parameters));

        textExtractionService = TextExtractionService.builder()
            .authenticationProvider(mockAuthenticationProvider)
            .spaceId("<space_id>")
            .baseUrl(URI.create("http://localhost:%s".formatted(watsonxServer.getPort())))
            .cosUrl(CosUrl.JP_TOK)
            .documentReference("<connection_id>", "bucket")
            .resultReference("<connection_id>", "bucket")
            .logRequests(true)
            .logResponses(true)
            .build();

        watsonxServer
            .stubFor(delete("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=%s&space_id=%s".formatted(API_VERSION, spaceId))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .willReturn(aResponse()
                    .withStatus(204)
                ));

        parameters = TextExtractionDeleteParameters.builder()
            .spaceId("new-space-id")
            .build();

        assertTrue(textExtractionService.deleteRequest("b3b85a66-7324-470c-b62e-75579eecf045", parameters));
    }

    @Test
    void test_execeptions() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockHttpClient.send(any(), any(BodyHandler.class)))
            .thenThrow(IOException.class);

        withWatsonxServiceMock(() -> {
            var textExtractionService = TextExtractionService.builder()
                .authenticationProvider(mockAuthenticationProvider)
                .projectId("<project_id>")
                .baseUrl(URI.create("http://localhost"))
                .cosUrl(CosUrl.EU_GB)
                .logRequests(true)
                .logResponses(true)
                .documentReference("<connection_id>", "bucket")
                .resultReference("<connection_id>", "bucket")
                .logRequests(true)
                .logResponses(true)
                .build();

            assertThrows(RuntimeException.class, () -> textExtractionService.startExtraction("file.txt"));
            assertThrows(RuntimeException.class, () -> textExtractionService.fetchExtractionRequest("id"));
            assertThrows(RuntimeException.class, () -> textExtractionService.deleteRequest("id"));
            assertThrows(RuntimeException.class, () -> textExtractionService.readFile("test", "id"));
            assertThrows(RuntimeException.class, () -> textExtractionService.deleteFile("test", "id"));

            var error = new WatsonxError(
                400, "error", List.of(new WatsonxError.Error("X", "X", "X")));

            var json = Json.toJson(error);

            when(mockHttpResponse.statusCode()).thenReturn(400);
            when(mockHttpResponse.body()).thenReturn(json);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (t, u) -> true));

            mockHttpClientSend(any(), any(BodyHandler.class));

            var ex = assertThrows(WatsonxException.class, () -> textExtractionService.deleteRequest("id"));
            JSONAssert.assertEquals(json, ex.getMessage(), true);
        });
    }

    @Test
    void test_file_utils() {
        assertEquals("file.json", TextExtractionUtils.addExtension("file.json", Type.JSON));
        assertEquals("file.html", TextExtractionUtils.addExtension("file.json", Type.HTML));
        assertEquals("/", TextExtractionUtils.addExtension("/", Type.PAGE_IMAGES));
    }

    @Test

    void test_read_file() {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        cosServer.stubFor(get("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(
                aResponse().withStatus(200)
                    .withBody("test")
            ));

        assertEquals("test", textExtractionService.readFile(BUCKET_NAME, FILE_NAME));
    }

    @Test
    void extractAndFetchTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var outputFileName = FILE_NAME.replace(".pdf", ".md");
        mockServers(outputFileName, false, false);

        String textExtracted = textExtractionService.extractAndFetch(FILE_NAME);
        assertEquals("Hello", textExtracted);

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(1, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void uploadExtractAndFetchInputStreamTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var inputStream = TextExtractionTest.class.getClassLoader().getResourceAsStream(FILE_NAME);
        var outputFileName = FILE_NAME.replace(".pdf", ".md");

        mockServers(outputFileName, false, false);

        String textExtracted = textExtractionService.uploadExtractAndFetch(inputStream, FILE_NAME);
        assertEquals("Hello", textExtracted);

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(1, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void uploadExtractAndFetchTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());
        var outputFileName = file.getName().replace(".pdf", ".txt");

        mockServers(outputFileName, false, false);

        String textExtracted = textExtractionService.uploadExtractAndFetch(file);
        assertEquals("Hello", textExtracted);

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(1, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void uploadExtractAndFetchFileNotFoundTest() throws Exception {

        var file = new File("doesnotexist.pdf");
        var outputFileName = file.getName().replace(".pdf", ".txt");

        mockServers(outputFileName, false, false);

        TextExtractionException ex = assertThrows(TextExtractionException.class,
            () -> textExtractionService.uploadExtractAndFetch(file));
        assertEquals(ex.getCode(), "file_not_found");
        assertTrue(ex.getCause() instanceof FileNotFoundException);
        assertEquals("TextExtractionException [code=file_not_found, message=doesnotexist.pdf (No such file or directory)]", ex.toString());

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void startExtractionTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var outputFileName = FILE_NAME.replace(".pdf", ".md");
        mockServers(outputFileName, false, false);

        var result = textExtractionService.startExtraction(FILE_NAME);
        assertEquals(PROCESS_EXTRACTION_ID, result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void uploadAndStartExtractionTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());
        var outputFileName = file.getName().replace(".pdf", ".txt");

        mockServers(outputFileName, false, false);

        var result = textExtractionService.uploadAndStartExtraction(file);
        assertEquals(PROCESS_EXTRACTION_ID, result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void uploadAndStartExtractionFileNotFoundTest() throws Exception {

        var file = new File("doesnotexist.pdf");
        var outputFileName = file.getName().replace(".pdf", ".txt");

        mockServers(outputFileName, false, false);

        TextExtractionException ex = assertThrows(TextExtractionException.class,
            () -> textExtractionService.uploadAndStartExtraction(file));
        assertEquals(ex.getCode(), "file_not_found");
        assertTrue(ex.getCause() instanceof FileNotFoundException);

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void uploadAndStartExtractionInputStreamTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        InputStream inputStream = TextExtractionTest.class.getClassLoader().getResourceAsStream(FILE_NAME);
        String outputFileName = FILE_NAME.replace(".pdf", ".md");

        mockServers(outputFileName, false, false);

        var result = textExtractionService.uploadAndStartExtraction(inputStream, FILE_NAME);
        assertEquals(PROCESS_EXTRACTION_ID, result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void forceRemoveOutputAndUploadedFiles() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockAuthenticationProvider.asyncToken()).thenReturn(CompletableFuture.completedFuture("my-super-token"));

        var outputFileName = "myNewOutput.json";
        var file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());

        mockServers(outputFileName, true, true);

        TextExtractionParameters parameters = TextExtractionParameters.builder()
            .removeOutputFile(true)
            .removeUploadedFile(true)
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> textExtractionService.startExtraction(FILE_NAME, parameters),
            "The asynchronous version of startExtraction doesn't allow the use of the \"removeOutputFile\" and \"removeUploadedFile\" options");

        assertThrows(
            IllegalArgumentException.class,
            () -> textExtractionService.uploadAndStartExtraction(file, parameters),
            "The asynchronous version of startExtraction doesn't allow the use of the \"removeOutputFile\" and \"removeUploadedFile\" options");

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));

        String extractedText = textExtractionService.extractAndFetch(FILE_NAME, parameters);
        assertEquals("Hello", extractedText);
        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(1, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));

        watsonxServer.resetAll();
        cosServer.resetAll();

        mockServers(outputFileName, true, true);

        extractedText = textExtractionService.uploadExtractAndFetch(file, parameters);
        assertEquals("Hello", extractedText);
        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(1, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(1, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void mdOutputFileNameTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var outputFileName = "test.md";
        mockServers(outputFileName, false, false);

        textExtractionService.startExtraction("test");
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void jsonOutputFileNameTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        var outputFileName = "test.json";

        mockServers(outputFileName, false, false);

        textExtractionService.startExtraction(FILE_NAME, TextExtractionParameters.builder().requestedOutputs(JSON).build());
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void textPlainOutputFileNameTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        var outputFileName = "test.txt";

        mockServers(outputFileName, false, false);

        textExtractionService.startExtraction(FILE_NAME, TextExtractionParameters.builder().requestedOutputs(PLAIN_TEXT).build());
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void pageImagesOutputFolderTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        var outputFolderName = "/";

        mockServers(outputFolderName, false, false);

        textExtractionService.startExtraction(FILE_NAME, TextExtractionParameters.builder().requestedOutputs(PAGE_IMAGES).build());
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFolderName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFolderName))));
    }

    @Test
    void multipleTypeOutputFolderTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        var outputFolderName = "/";

        mockServers(outputFolderName, false, false);

        textExtractionService.startExtraction(FILE_NAME,
            TextExtractionParameters.builder().requestedOutputs(MD, JSON, PLAIN_TEXT, PAGE_IMAGES, HTML).build());
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFolderName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFolderName))));
    }

    @Test
    void startExtractionWithOutputFolderName() throws Exception {

        var outputFolderName = "myFolder/";
        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var outputFileName = outputFolderName + FILE_NAME.replace(".pdf", ".md");
        mockServers(outputFileName, false, false);

        var parameters = TextExtractionParameters.builder().outputFileName(outputFolderName).build();
        var result = textExtractionService.startExtraction(FILE_NAME, parameters);
        assertEquals(PROCESS_EXTRACTION_ID, result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void startExtractionWithOutputFolderNameAndMultipleTypes() throws Exception {

        var outputFolderName = "myFolder/";
        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        mockServers(outputFolderName, false, false);

        var parameters = TextExtractionParameters.builder()
            .requestedOutputs(Type.MD, Type.PLAIN_TEXT)
            .outputFileName(outputFolderName)
            .build();

        var result = textExtractionService.startExtraction(FILE_NAME, parameters);
        assertEquals(PROCESS_EXTRACTION_ID, result.metadata().id());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(0, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFolderName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFolderName))));
    }

    @Test
    void fetchWithMultipleTypes() throws URISyntaxException {

        var outputFolderName = "myFolder/";

        mockServers(outputFolderName, false, false);
        var ex = assertThrows(TextExtractionException.class, () -> {
            textExtractionService.extractAndFetch(
                FILE_NAME,
                TextExtractionParameters.builder()
                    .outputFileName("myFolder/")
                    .requestedOutputs(MD, JSON, PLAIN_TEXT, PAGE_IMAGES, HTML)
                    .mode(Mode.STANDARD)
                    .ocrMode(OcrMode.DISABLED)
                    .autoRotationCorrection(false)
                    .createEmbeddedImages(EmbeddedImageMode.DISABLED)
                    .outputDpi(16)
                    .outputTokens(false)
                    .build());
        });

        assertEquals("fetch_operation_not_allowed", ex.getCode());
        assertEquals("The fetch operation cannot be executed if more than one file is to be generated", ex.getMessage());

        File file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());

        ex = assertThrows(TextExtractionException.class, () -> {
            textExtractionService.uploadExtractAndFetch(
                file,
                TextExtractionParameters.builder()
                    .outputFileName("myFolder/")
                    .requestedOutputs(MD, JSON, PLAIN_TEXT, PAGE_IMAGES, HTML)
                    .mode(Mode.STANDARD)
                    .ocrMode(OcrMode.DISABLED)
                    .autoRotationCorrection(false)
                    .createEmbeddedImages(EmbeddedImageMode.DISABLED)
                    .outputDpi(16)
                    .outputTokens(false)
                    .build());
        });

        assertEquals("fetch_operation_not_allowed", ex.getCode());
        assertEquals("The fetch operation cannot be executed if more than one file is to be generated",
            ex.getMessage());

        InputStream inputStream = TextExtractionTest.class.getClassLoader().getResourceAsStream(FILE_NAME);

        ex = assertThrows(TextExtractionException.class, () -> {
            textExtractionService.uploadExtractAndFetch(
                inputStream,
                FILE_NAME,
                TextExtractionParameters.builder()
                    .outputFileName("myFolder/")
                    .requestedOutputs(MD, JSON, PLAIN_TEXT, PAGE_IMAGES, HTML)
                    .mode(Mode.STANDARD)
                    .ocrMode(OcrMode.DISABLED)
                    .autoRotationCorrection(false)
                    .createEmbeddedImages(EmbeddedImageMode.DISABLED)
                    .outputDpi(16)
                    .outputTokens(false)
                    .build());
        });
        assertEquals("fetch_operation_not_allowed", ex.getCode());
        assertEquals("The fetch operation cannot be executed if more than one file is to be generated",
            ex.getMessage());
    }


    @Test
    void fetchPageImages() throws URISyntaxException {

        var outputFolderName = "myFolder/";
        var inputStream = TextExtractionTest.class.getClassLoader().getResourceAsStream(FILE_NAME);
        var file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());

        mockServers(outputFolderName, false, false);
        var ex = assertThrows(TextExtractionException.class, () -> {
            textExtractionService.extractAndFetch(
                FILE_NAME,
                TextExtractionParameters.builder()
                    .outputFileName("myFolder/")
                    .requestedOutputs(PAGE_IMAGES)
                    .build());
        });
        assertEquals("fetch_operation_not_allowed", ex.getCode());
        assertEquals("The fetch operation cannot be executed for the type \"page_images\"",
            ex.getMessage());

        ex = assertThrows(TextExtractionException.class, () -> {
            textExtractionService.uploadExtractAndFetch(
                file,
                TextExtractionParameters.builder()
                    .outputFileName("myFolder/")
                    .requestedOutputs(PAGE_IMAGES)
                    .build());
        });
        assertEquals("fetch_operation_not_allowed", ex.getCode());
        assertEquals("The fetch operation cannot be executed for the type \"page_images\"",
            ex.getMessage());

        ex = assertThrows(TextExtractionException.class, () -> {
            textExtractionService.uploadExtractAndFetch(
                inputStream,
                FILE_NAME,
                TextExtractionParameters.builder()
                    .outputFileName("myFolder/")
                    .requestedOutputs(PAGE_IMAGES)
                    .build());
        });
        assertEquals("fetch_operation_not_allowed", ex.getCode());
        assertEquals("The fetch operation cannot be executed for the type \"page_images\"",
            ex.getMessage());
    }

    @Test
    void simulateLongResponseTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        var outputFileName = FILE_NAME.replace(".pdf", ".md");

        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .inScenario("long_response")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("firstIteration")
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .inScenario("long_response")
            .whenScenarioStateIs("firstIteration")
            .willSetStateTo("secondIteration")
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .inScenario("long_response")
            .whenScenarioStateIs("secondIteration")
            .willSetStateTo(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "completed"))
            ));

        cosServer.stubFor(get("/%s/%s".formatted(BUCKET_NAME, outputFileName))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200).withBody("Hello")));

        String extractedValue = textExtractionService.extractAndFetch(FILE_NAME);
        assertEquals("Hello", extractedValue);
        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(2, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
    }

    @Test
    void simulateTimeoutResponseTest() {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        var outputFileName = FILE_NAME.replace(".pdf", ".md");

        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .inScenario("long_response")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("firstIteration")
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .inScenario("long_response")
            .whenScenarioStateIs("firstIteration")
            .willSetStateTo("secondIteration")
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .inScenario("long_response")
            .whenScenarioStateIs("secondIteration")
            .willSetStateTo(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "completed"))
            ));

        cosServer.stubFor(get("/%s/%s".formatted(BUCKET_NAME, outputFileName))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200).withBody("Hello")));

        TextExtractionParameters options = TextExtractionParameters.builder()
            .timeout(Duration.ofMillis(100))
            .build();

        var ex = assertThrows(
            TextExtractionException.class,
            () -> textExtractionService.extractAndFetch(FILE_NAME, options));

        assertEquals("Execution to extract test.pdf file took longer than the timeout set by 100 milliseconds",
            ex.getMessage());

        watsonxServer.verify(1, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(1, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
    }

    @Test
    void simulateFailedStatusOnResponseTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        when(mockAuthenticationProvider.asyncToken()).thenReturn(CompletableFuture.completedFuture("my-super-token"));

        var outputFileName = FILE_NAME.replace(".pdf", ".md");
        var file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());

        TextExtractionParameters parameters = TextExtractionParameters.builder()
            .removeOutputFile(true)
            .removeUploadedFile(true)
            .build();

        cosServer.stubFor(put("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200)));

        cosServer.stubFor(delete("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200)));

        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .inScenario("simulate_failed")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("failed")
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "running"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .inScenario("simulate_failed")
            .whenScenarioStateIs("failed")
            .willSetStateTo(Scenario.STARTED)
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_FAIL_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, outputFileName))
            ));

        var ex = assertThrows(
            TextExtractionException.class,
            () -> textExtractionService.extractAndFetch(FILE_NAME, parameters));

        assertEquals(ex.getCode(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        ex = assertThrows(
            TextExtractionException.class,
            () -> textExtractionService.uploadExtractAndFetch(file, parameters));

        assertEquals(ex.getCode(), "file_download_error");
        assertEquals(ex.getMessage(), "error message");

        Thread.sleep(200); // Wait for the async calls.
        watsonxServer.verify(2, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(4, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(2, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void noSuchBucketTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var outputFileName = FILE_NAME.replace(".pdf", ".md");
        var file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());

        cosServer.stubFor(put("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/xml")
                .withBody("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Error>
                        <Code>NoSuchBucket</Code>
                        <Message>The specified bucket does not exist.</Message>
                        <Resource>/my-bucket-name/test.pdf</Resource>
                        <RequestId>my-request-id</RequestId>
                        <httpStatusCode>404</httpStatusCode>
                    </Error>""")));


        var detail = new com.ibm.watsonx.ai.core.exeception.model.WatsonxError.Error("NoSuchBucket", "The specified bucket does not exist.",
            "/my-bucket-name/test.pdf");
        WatsonxError error = new WatsonxError(404, "my-request-id", List.of(detail));

        var ex = assertThrows(
            WatsonxException.class,
            () -> textExtractionService.uploadAndStartExtraction(file),
            "The specified bucket does not exist.");

        assertEquals(error, ex.details().orElseThrow());

        ex = assertThrows(
            WatsonxException.class,
            () -> textExtractionService.uploadExtractAndFetch(file),
            "The specified bucket does not exist.");

        assertEquals(error, ex.details().orElseThrow());

        watsonxServer.verify(0, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(0, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(2, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void noSuchKeyTest() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var outputFileName = FILE_NAME.replace(".pdf", ".md");
        var file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());

        cosServer.stubFor(put("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200)));

        cosServer.stubFor(get("/%s/%s".formatted(BUCKET_NAME, outputFileName))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/xml")
                .withBody("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Error>
                        <Code>NoSuchKey</Code>
                        <Message>The specified key does not exist.</Message>
                        <Resource>/my-bucket-name/test.pdf</Resource>
                        <RequestId>my-request-id</RequestId>
                        <httpStatusCode>404</httpStatusCode>
                    </Error>""")));

        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "submitted"))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME,
                    "completed"))
            ));

        var detail = new com.ibm.watsonx.ai.core.exeception.model.WatsonxError.Error("NoSuchKey", "The specified key does not exist.",
            "/my-bucket-name/test.pdf");
        WatsonxError error = new WatsonxError(404, "my-request-id", List.of(detail));

        var ex = assertThrows(
            WatsonxException.class,
            () -> textExtractionService.extractAndFetch(FILE_NAME),
            "The specified key does not exist.");

        assertEquals(error, ex.details().orElseThrow());

        ex = assertThrows(
            WatsonxException.class,
            () -> textExtractionService.uploadExtractAndFetch(file),
            "The specified key does not exist.");

        assertEquals(error, ex.details().orElseThrow());

        watsonxServer.verify(2, postRequestedFor(urlPathEqualTo("/ml/v1/text/extractions")));
        watsonxServer.verify(2, getRequestedFor(urlPathEqualTo("/ml/v1/text/extractions/" + PROCESS_EXTRACTION_ID)));
        cosServer.verify(1, putRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(2, getRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        cosServer.verify(0, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, outputFileName))));
    }

    @Test
    void textExtractionEventDoesntExistTest() {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                            "trace": "9ddfccd50f6649d9913810df36578d38",
                            "errors": [
                                {
                                    "code": "text_extraction_event_does_not_exist",
                                    "message": "Text extraction request does not exist."
                                }
                            ]
                        }
                    """)
            ));

        var ex = assertThrows(WatsonxException.class,
            () -> textExtractionService.fetchExtractionRequest(PROCESS_EXTRACTION_ID));
        assertEquals(WatsonxError.Code.TEXT_EXTRACTION_EVENT_DOES_NOT_EXIST.value(),
            ex.details().orElseThrow().errors().get(0).code());
    }

    @Test
    void checkExtractionStatusTest() throws TextExtractionException {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");
        var outputFileName = FILE_NAME.replace(".pdf", ".md");
        var EXPECTED = TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, FILE_NAME, "completed");

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(EXPECTED)
            ));

        TextExtractionResponse response = textExtractionService.fetchExtractionRequest(PROCESS_EXTRACTION_ID);
        JSONAssert.assertEquals(EXPECTED, toJson(response), true);
    }

    @Test
    void overrideConnectionIdAndBucket() throws Exception {

        when(mockAuthenticationProvider.token()).thenReturn("my-super-token");

        var outputFileName = FILE_NAME.replace(".pdf", ".md");
        var file = new File(TextExtractionTest.class.getClassLoader().getResource(FILE_NAME).toURI());

        String NEW_CONNECTION_ID = "my-new-connection-id";
        String NEW_BUCKET_NAME = "my-new-bucket";

        var parameters = TextExtractionParameters.builder()
            .documentReference(CosReference.of(NEW_CONNECTION_ID, NEW_BUCKET_NAME))
            .resultReference(CosReference.of(NEW_CONNECTION_ID, NEW_BUCKET_NAME))
            .build();

        cosServer.stubFor(put("/%s/%s".formatted(NEW_BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200)));

        cosServer.stubFor(get("/%s/%s".formatted(NEW_BUCKET_NAME, outputFileName))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200).withBody("Hello")));

        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""
                    {
                      "metadata": {
                          "id": "%s",
                          "created_at": "2023-05-02T16:27:51Z",
                          "project_id": "<project-id>"
                      },
                      "entity": {
                          "document_reference": {
                            "type": "connection_asset",
                            "connection": {
                              "id": "%s"
                            },
                            "location": {
                              "file_name": "%s",
                              "bucket": "%s"
                            }
                          },
                        "results_reference": {
                          "type": "connection_asset",
                          "connection": {
                            "id": "%s"
                          },
                          "location": {
                            "file_name": "%s",
                            "bucket": "%s"
                          }
                        },
                        "results": {
                          "status": "submitted",
                          "number_pages_processed": 1,
                          "running_at": "2023-05-02T16:28:03Z",
                          "completed_at": "2023-05-02T16:28:03Z"
                        }
                      }
                    }""".formatted(PROCESS_EXTRACTION_ID, NEW_CONNECTION_ID, FILE_NAME, NEW_BUCKET_NAME, NEW_CONNECTION_ID,
                    outputFileName, BUCKET_NAME))
            ));

        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""
                    {
                      "metadata": {
                          "id": "%s",
                          "created_at": "2023-05-02T16:27:51Z",
                          "project_id": "<project-id>"
                      },
                      "entity": {
                          "document_reference": {
                            "type": "connection_asset",
                            "connection": {
                              "id": "%s"
                            },
                            "location": {
                              "file_name": "%s",
                              "bucket": "%s"
                            }
                          },
                        "results_reference": {
                          "type": "connection_asset",
                          "connection": {
                            "id": "%s"
                          },
                          "location": {
                            "file_name": "%s",
                            "bucket": "%s"
                          }
                        },
                        "results": {
                          "status": "completed",
                          "number_pages_processed": 1,
                          "running_at": "2023-05-02T16:28:03Z",
                          "completed_at": "2023-05-02T16:28:03Z"
                        }
                      }
                    }""".formatted(PROCESS_EXTRACTION_ID, NEW_CONNECTION_ID, FILE_NAME, NEW_BUCKET_NAME, NEW_CONNECTION_ID,
                    outputFileName, BUCKET_NAME))
            ));

        assertDoesNotThrow(() -> textExtractionService.extractAndFetch(FILE_NAME, parameters));
        assertDoesNotThrow(() -> textExtractionService.startExtraction(FILE_NAME, parameters));
        assertDoesNotThrow(() -> textExtractionService.uploadAndStartExtraction(file, parameters));
        assertDoesNotThrow(() -> textExtractionService.uploadExtractAndFetch(file, parameters));
    }

    @Test
    void deleteFileTest() {

        when(mockAuthenticationProvider.asyncToken()).thenReturn(completedFuture("my-super-token"));

        cosServer.resetAll();

        cosServer.stubFor(delete("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("retry")
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/xml")
                .withBody("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Error>
                        <Code>AccessDenied</Code>
                        <Message>Access Denied</Message>
                        <Resource>/andreaproject-donotdelete-pr-xnran4g4ptd1wo/ciao.pdf</Resource>
                        <RequestId>df887c2b-43c3-4933-a3a1-b0e19e7c2231</RequestId>
                        <httpStatusCode>403</httpStatusCode>
                    </Error>""")));

        cosServer.stubFor(delete("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .inScenario("retry")
            .whenScenarioStateIs("retry")
            .willSetStateTo(Scenario.STARTED)
            .willReturn(aResponse().withStatus(204)));

        assertTrue(textExtractionService.deleteFile(BUCKET_NAME, FILE_NAME));
        cosServer.verify(2, deleteRequestedFor(urlEqualTo("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))));
        verify(mockAuthenticationProvider, times(2)).asyncToken();
    }


    private void mockServers(String outputFileName, boolean deleteUploadedFile, boolean deleteOutputFile) {

        // Mock the upload local file operation.
        cosServer.stubFor(put("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200)));

        // Mock extracted file result.
        cosServer.stubFor(get("/%s/%s".formatted(BUCKET_NAME, outputFileName))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .willReturn(aResponse().withStatus(200).withBody("Hello")));


        if (deleteUploadedFile) {
            // Mock delete uploaded file.
            cosServer.stubFor(delete("/%s/%s".formatted(BUCKET_NAME, FILE_NAME))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .willReturn(aResponse().withStatus(200)));
        }

        if (deleteOutputFile) {
            // Mock delete extracted file.
            cosServer.stubFor(delete("/%s/%s".formatted(BUCKET_NAME, outputFileName))
                .withHeader("Authorization", equalTo("Bearer my-super-token"))
                .willReturn(aResponse().withStatus(200)));
        }

        // Mock start extraction.
        watsonxServer.stubFor(post("/ml/v1/text/extractions?version=%s".formatted(API_VERSION))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, BUCKET_NAME,
                    "submitted"))
            ));

        // Mock result extraction.
        watsonxServer.stubFor(get("/ml/v1/text/extractions/%s?version=%s&project_id=%s".formatted(PROCESS_EXTRACTION_ID, API_VERSION, "projectid"))
            .withHeader("Authorization", equalTo("Bearer my-super-token"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(TEXT_EXTRACTION_RESPONSE.formatted(PROCESS_EXTRACTION_ID, FILE_NAME, BUCKET_NAME, outputFileName, BUCKET_NAME,
                    "completed"))
            ));
    }
}
