/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.textprocessing.GroundingHints.FieldData;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaResponse.ClusterSchemaResult;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemas;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationParameters;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationRequest;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationResponse;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionRequest;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionResponse;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig;

public class TextProcessingRoundTripTest {

    @Test
    void should_round_trip_schema_with_snake_case_properties() {

        var EXPECTED = """
            {
              "document_type": "Invoice",
              "document_description": "A commercial invoice document",
              "fields": {
                "invoice_number": {
                  "description": "The invoice number",
                  "example": "INV-1001"
                },
                "total_amount": {
                  "description": "The total amount due",
                  "example": "1250.00",
                  "available_options": ["USD", "EUR"]
                }
              },
              "additional_prompt_instructions": "Focus on the totals section."
            }""";

        var schema = Schema.builder()
            .documentType("Invoice")
            .documentDescription("A commercial invoice document")
            .fields(
                KvpFields.builder()
                    .add("invoice_number", KvpField.of("The invoice number", "INV-1001"))
                    .add("total_amount", KvpField.of("The total amount due", "1250.00", List.of("USD", "EUR")))
                    .build())
            .additionalPromptInstructions("Focus on the totals section.")
            .build();

        var json = Json.toJson(schema);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTrip = Json.fromJson(json, Schema.class);
        assertEquals(schema, roundTrip);
        assertEquals(List.of("USD", "EUR"), roundTrip.fields().get("total_amount").availableOptions());
    }

    @Test
    void should_deserialize_kvp_fields_map_with_arbitrary_field_names() {

        var JSON = """
            {
              "invoice_number": {
                "description": "The invoice number",
                "example": "INV-1001"
              },
              "total_amount": {
                "description": "The total amount due",
                "example": "1250.00",
                "available_options": ["USD", "EUR"]
              }
            }""";

        var expected = KvpFields.builder()
            .add("invoice_number", KvpField.of("The invoice number", "INV-1001"))
            .add("total_amount", KvpField.of("The total amount due", "1250.00", List.of("USD", "EUR")))
            .build();

        var actual = Json.fromJson(JSON, KvpFields.class);

        assertEquals(expected, actual);
        assertEquals(2, actual.fields().size());
    }

    @Test
    void should_round_trip_grounding_hints_with_normalized_bbox_and_page_number() {

        var EXPECTED = """
            {
              "fields": {
                "invoice_number": {
                  "normalized_bbox": [0.12, 0.34, 0.56, 0.78],
                  "page_number": 1
                },
                "total_amount": {
                  "normalized_bbox": [0.1, 0.8, 0.3, 0.9],
                  "page_number": 2
                }
              }
            }""";

        var groundingHints = GroundingHints.builder()
            .add("invoice_number", FieldData.of(List.of(0.12, 0.34, 0.56, 0.78), 1))
            .add("total_amount", FieldData.of(List.of(0.1, 0.8, 0.3, 0.9), 2))
            .build();

        var json = Json.toJson(groundingHints);
        JSONAssert.assertEquals(EXPECTED, json, true);

        var roundTrip = Json.fromJson(json, GroundingHints.class);
        assertEquals(groundingHints, roundTrip);
        assertEquals(1, roundTrip.pageNumber("invoice_number"));
        assertEquals(List.of(0.1, 0.8, 0.3, 0.9), roundTrip.bbox("total_amount"));
    }

    @Test
    void should_snapshot_nested_classification_custom_and_semantic_collections() {
        var values = new ArrayList<>(List.of("original"));
        var properties = new LinkedHashMap<String, Object>();
        properties.put("values", values);
        var config = TextClassificationSemanticConfig.builder().taskModelNameOverride(properties).build();
        var parameters = TextClassificationParameters.builder().addCustomProperty("values", values).semanticConfig(config).build();
        var snapshots = List.of(
            parameters.custom(),
            config.taskModelNameOverride(),
            new TextClassificationRequest(null, null, null, null, properties).custom(),
            new TextClassificationResponse.Entity(null, null, null, properties).custom());

        values.add("later");
        properties.clear();

        for (Map<String, Object> snapshot : snapshots) {
            assertEquals(Map.of("values", List.of("original")), snapshot);
            assertThrows(UnsupportedOperationException.class, () -> ((List<?>) snapshot.get("values")).clear());
            assertThrows(UnsupportedOperationException.class, snapshot::clear);
        }
    }

    @Test
    void should_snapshot_nested_extraction_custom_and_semantic_collections() {
        var values = new ArrayList<>(List.of("original"));
        var properties = new LinkedHashMap<String, Object>();
        properties.put("values", values);
        var config = TextExtractionSemanticConfig.builder().taskModelNameOverride(properties).build();
        var parameters = TextExtractionParameters.builder().addCustomProperty("values", values).semanticConfig(config).build();
        var snapshots = List.of(
            parameters.custom(),
            config.taskModelNameOverride(),
            new TextExtractionRequest(null, null, null, null, null, properties).custom(),
            new TextExtractionResponse.Entity(null, null, null, null, properties).custom());

        values.add("later");
        properties.clear();

        for (Map<String, Object> snapshot : snapshots) {
            assertEquals(Map.of("values", List.of("original")), snapshot);
            assertThrows(UnsupportedOperationException.class, () -> ((List<?>) snapshot.get("values")).clear());
            assertThrows(UnsupportedOperationException.class, snapshot::clear);
        }
    }

    @Test
    void should_snapshot_each_cluster_in_results() {
        var entry = new ClusterSchemas("document", Schema.builder().build());
        var cluster = new ArrayList<>(List.of(entry));
        var clusters = new ArrayList<>(List.of(cluster));
        var result = new ClusterSchemaResult("completed", null, null, new ArrayList<>(clusters), null);

        cluster.clear();
        clusters.clear();

        assertEquals(List.of(List.of(entry)), result.schemas());
        assertThrows(UnsupportedOperationException.class, result.schemas()::clear);
        assertThrows(UnsupportedOperationException.class, result.schemas().get(0)::clear);
    }

}
