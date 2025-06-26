/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters;
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

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class TextExtractionTest {

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
              "parameteres": {
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
      new Metadata("abc123", "2025-06-25T10:15:30Z", "3fc54cf1-252f-424b-b52d-5cdd9814987f", "3fc54cf1-252f-424b-b52d-5cdd9814987f");

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
      .build();

    JSONAssert.assertEquals(Json.toJson(parameters), Json.toJson(params.toParameters()), true);
  }
}
