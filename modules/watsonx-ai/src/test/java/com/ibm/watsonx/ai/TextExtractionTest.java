/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.core.exeception.model.WatsonxError;
import com.ibm.watsonx.ai.textextraction.CosReference;
import com.ibm.watsonx.ai.textextraction.FileUtils;
import com.ibm.watsonx.ai.textextraction.TextExtractionDeleteParameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionFetchParameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.CosUrl;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.EmbeddedImageMode;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.KvpMode;
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

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class TextExtractionTest {

  @Mock
  HttpClient mockHttpClient;

  @Mock
  HttpRequest mockHttpRequest;

  @Mock
  HttpResponse<String> mockHttpResponse;

  @Mock
  AuthenticationProvider mockAuthenticationProvider;

  @RegisterExtension
  WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
    .build();

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
                  "output_tokens_and_bbox": true,
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
      .requestedOutputs(Type.PLAIN_TEXT, Type.HTML)
      .mode(Mode.STANDARD)
      .ocrMode(OcrMode.ENABLED)
      .languages("en", "it")
      .autoRotationCorrection(true)
      .createEmbeddedImages(EmbeddedImageMode.ENABLED_TEXT)
      .outputDpi(150)
      .outputTokensAndBbox(true)
      .kvpMode(KvpMode.GENERIC_WITH_SEMANTIC)
      .semanticConfig(
        new SemanticConfig.Builder()
          .targetImageWidth(1024)
          .enableTextHints(true)
          .enableGenericKvp(true)
          .schemas(List.of(
            new Schema.Builder()
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
                      "file_name": "myfile.txt",
                      "bucket": "bucket"
                  }
              },
              "parameters": {
                  "requested_outputs": [
                      "plain_text"
                  ],
                  "mode": "standard",
                  "languages": [
                      "it"
                  ],
                  "create_embedded_images": "disabled",
                  "output_dpi": 72,
                  "output_tokens_and_bbox": true
              },
              "results": {
                  "status": "submitted",
                  "number_pages_processed": 0
              }
          }
      }""";

    wireMock.stubFor(post("/ml/v1/text/extractions?version=2025-04-23")
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
                    "file_name": "myfile.txt",
                    "bucket": "bucket"
                }
            }
        }"""))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(EXPECTED)
      )
    );

    var httpPort = wireMock.getPort();
    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");

    var textExtractionService = TextExtractionService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .projectId("<project-id>")
      .url(URI.create("http://localhost:%s".formatted(httpPort)))
      .cosUrl(CosUrl.AU_SYD)
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
      .build();

    var response = textExtractionService.startExtraction("myfile.pdf");
    JSONAssert.assertEquals(EXPECTED, Json.toJson(response), true);
  }

  @Test
  void test_start_extraction_with_parameters() {

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    wireMock.stubFor(post("/ml/v1/text/extractions?version=2025-04-23")
      .withHeader("Authorization", equalTo("Bearer my-super-token"))
      .withHeader("Content-Type", equalTo("application/json"))
      .withHeader("Accept", equalTo("application/json"))
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
      .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
      .cosUrl(CosUrl.BR_SAO)
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
      .build();

    var parameters = TextExtractionParameters.builder()
      .projectId("new-project-id")
      .spaceId("new-space-id")
      .documentReference(CosReference.of("my-new-connection", "my-new-bucket"))
      .resultReference(CosReference.of("my-new-connection-2", "my-new-bucket-2"))
      .requestedOutputs(Type.MD)
      .addCustomProperty("key", "value")
      .build();

    var response = textExtractionService.startExtraction("myfile.pdf", parameters);
    assertNotNull(response);
  }

  @Test
  void test_text_extraction_page_images() {

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    wireMock.stubFor(post("/ml/v1/text/extractions?version=2025-04-23")
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
      .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
      .build();

    var parameters = TextExtractionParameters.builder()
      .requestedOutputs(Type.PAGE_IMAGES)
      .build();

    var response = textExtractionService.startExtraction("0.png", parameters);
    assertNotNull(response);
  }

  @Test
  void test_text_extraction_multiple_outputs() {

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    wireMock.stubFor(post("/ml/v1/text/extractions?version=2025-04-23")
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
      .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
      .cosUrl(CosUrl.CA_TOR)
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
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
                      "output_tokens_and_bbox": true,
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

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");

    wireMock.stubFor(get("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=2025-04-23&project_id="
      + URLEncoder.encode("<project_id>", Charset.defaultCharset()))
      .withHeader("Authorization", equalTo("Bearer my-super-token"))
      .withHeader("Accept", equalTo("application/json"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(RESULT)
      ));

    var textExtractionService = TextExtractionService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .projectId("<project_id>")
      .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
      .cosUrl(CosUrl.EU_DE)
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
      .build();

    var response = textExtractionService.fetchExtractionRequest("b3b85a66-7324-470c-b62e-75579eecf045");
    JSONAssert.assertEquals(RESULT, Json.toJson(response), true);

    var projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());
    var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

    wireMock.stubFor(get("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=2025-04-23&project_id=%s".formatted(projectId))
      .withHeader("Authorization", equalTo("Bearer my-super-token"))
      .withHeader("Accept", equalTo("application/json"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody("{}")
      ));

    var parameters = TextExtractionFetchParameters.builder()
      .projectId("new-project-id")
      .build();

    response = textExtractionService.fetchExtractionRequest("b3b85a66-7324-470c-b62e-75579eecf045", parameters);
    assertNotNull(response);

    textExtractionService = TextExtractionService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .spaceId("<space_id>")
      .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
      .cosUrl(CosUrl.JP_OSA)
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
      .build();

    wireMock.stubFor(get("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=2025-04-23&space_id=%s".formatted(spaceId))
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
    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");

    wireMock.stubFor(delete("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=2025-04-23&project_id=" + projectId)
      .withHeader("Authorization", equalTo("Bearer my-super-token"))
      .willReturn(aResponse()
        .withStatus(204)
      ));

    var textExtractionService = TextExtractionService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .projectId("<project_id>")
      .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
      .cosUrl(CosUrl.EU_ES)
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
      .build();

    assertTrue(textExtractionService.delete("b3b85a66-7324-470c-b62e-75579eecf045"));

    wireMock.stubFor(delete("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=2025-04-23&project_id=" + projectId)
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

    assertFalse(textExtractionService.delete("b3b85a66-7324-470c-b62e-75579eecf045"));

    projectId = URLEncoder.encode("new-project-id", Charset.defaultCharset());
    var spaceId = URLEncoder.encode("new-space-id", Charset.defaultCharset());

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    wireMock
      .stubFor(delete("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=2025-04-23&project_id=" + projectId + "&hard_delete=true")
        .withHeader("Authorization", equalTo("Bearer my-super-token"))
        .willReturn(aResponse()
          .withStatus(204)
        ));

    var parameters = TextExtractionDeleteParameters.builder()
      .projectId("new-project-id")
      .hardDelete(true)
      .build();

    assertTrue(textExtractionService.delete("b3b85a66-7324-470c-b62e-75579eecf045", parameters));

    textExtractionService = TextExtractionService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .spaceId("<space_id>")
      .url(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
      .cosUrl(CosUrl.JP_TOK)
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
      .build();

    wireMock.stubFor(delete("/ml/v1/text/extractions/b3b85a66-7324-470c-b62e-75579eecf045?version=2025-04-23&space_id=%s".formatted(spaceId))
      .withHeader("Authorization", equalTo("Bearer my-super-token"))
      .willReturn(aResponse()
        .withStatus(204)
      ));

    parameters = TextExtractionDeleteParameters.builder()
      .spaceId("new-space-id")
      .build();

    assertTrue(textExtractionService.delete("b3b85a66-7324-470c-b62e-75579eecf045", parameters));
  }

  @Test
  void test_execeptions() throws Exception {

    when(mockAuthenticationProvider.getToken()).thenReturn("my-super-token");
    when(mockHttpClient.send(any(), any(BodyHandler.class)))
      .thenThrow(IOException.class);

    var textExtractionService = TextExtractionService.builder()
      .authenticationProvider(mockAuthenticationProvider)
      .httpClient(mockHttpClient)
      .projectId("<project_id>")
      .url(URI.create("http://localhost"))
      .cosUrl(CosUrl.EU_GB)
      .documentReference("<connection_id>", "bucket")
      .resultReference("<connection_id>", "bucket")
      .build();

    assertThrows(RuntimeException.class, () -> textExtractionService.startExtraction("file.txt"));
    assertThrows(RuntimeException.class, () -> textExtractionService.fetchExtractionRequest("id"));
    assertThrows(RuntimeException.class, () -> textExtractionService.delete("id"));

    var error = new WatsonxError(
      400, "error", List.of(new WatsonxError.Error("X", "X", "X")));

    var json = Json.toJson(error);

    when(mockHttpResponse.statusCode()).thenReturn(400);
    when(mockHttpResponse.body()).thenReturn(json);
    when(mockHttpClient.send(any(), any(BodyHandler.class)))
      .thenReturn(mockHttpResponse);


    var ex = assertThrows(WatsonxException.class, () -> textExtractionService.delete("id"));
    JSONAssert.assertEquals(json, ex.getMessage(), true);
  }

  @Test
  void test_file_utils() {

    assertEquals("file.json", FileUtils.addExtension("file.json", Type.JSON));
    assertEquals("file.html", FileUtils.addExtension("file.json", Type.HTML));
    assertEquals("/", FileUtils.addExtension("/", Type.PAGE_IMAGES));
  }
}
