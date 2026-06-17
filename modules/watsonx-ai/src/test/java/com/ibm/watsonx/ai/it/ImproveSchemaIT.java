/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class ImproveSchemaIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final ImproveSchemaService improveSchemaService = ImproveSchemaService.builder()
        .baseUrl(URL)
        .apiKey(API_KEY)
        .projectId(PROJECT_ID)
        .timeout(Duration.ofMinutes(5))
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_improve_a_schema() throws Exception {

        var result = improveSchemaService.improveSchemaAndFetch(
            Schema.builder()
                .documentType("Passport")
                .documentDescription("Passport document to get the schema")
                .fields(
                    KvpFields.builder()
                        .add("name", KvpField.of("name of the user", "Andrea"))
                        .add("lastname", KvpField.of("lastname of the user", "Di Maio"))
                        .build())
                .build()
        );

        assertNotNull(result);
        assertNotNull(result.completedAt());
        assertNotNull(result.runningAt());
        assertNotNull(result.schema());
        assertNotNull(result.schema().documentType());
        assertNotNull(result.schema().documentDescription());
        assertNotNull(result.schema().fields());
        assertNotNull(result.schema().fields().get("name"));
        assertNotNull(result.schema().fields().get("lastname"));
        assertNotNull(result.schema().fields().get("name").example());
        assertNotNull(result.schema().fields().get("lastname").example());
        assertNotNull(result.schema().fields().get("name").description());
        assertNotNull(result.schema().fields().get("lastname").description());
        assertNotEquals(result.schema().documentDescription(), "Passport document to get the schema");
    }

    @Test
    void should_throw_an_exception_when_a_schema_is_not_provided() throws Exception {
        assertThrows(WatsonxException.class, () -> improveSchemaService.improveSchemaAndFetch(Schema.builder().build()));
    }

    @Test
    void should_delete_improve_schema_request() throws Exception {

        var schema = Schema.builder()
            .documentType("Passport")
            .documentDescription("Passport document to get the schema")
            .fields(
                KvpFields.builder()
                    .add("name", KvpField.of("name of the user", "Andrea"))
                    .add("lastname", KvpField.of("lastname of the user", "Di Maio"))
                    .build())
            .build();

        var response = improveSchemaService.startImproveSchema(schema);
        assertTrue(
            improveSchemaService.deleteRequest(
                response.metadata().id(),
                ImproveSchemaDeleteParameters.builder()
                    .hardDelete(true)
                    .build()
            )
        );

        var ex = assertThrows(WatsonxException.class, () -> improveSchemaService.fetchRequest(response.metadata().id()));
        assertEquals(404, ex.statusCode());
    }

    @Test
    void should_throw_an_exception_when_the_job_does_not_exist() {
        var ex = assertThrows(WatsonxException.class, () -> improveSchemaService.fetchRequest("non-existing-id"));
        assertEquals(404, ex.statusCode());
    }
}
