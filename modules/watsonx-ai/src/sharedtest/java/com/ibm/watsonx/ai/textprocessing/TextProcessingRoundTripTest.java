/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.textprocessing.GroundingHints.FieldData;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;

@DisabledInNativeImage
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
}
