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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class MergeSchemaIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final MergeSchemaService service = MergeSchemaService.builder()
        .baseUrl(URL)
        .apiKey(API_KEY)
        .projectId(PROJECT_ID)
        .timeout(Duration.ofMinutes(5))
        .logRequests(true)
        .logResponses(true)
        .build();

    static final List<Schema> schemas = List.of(
        Schema.builder()
            .documentType("Passport")
            .documentDescription("Passport document to get the schema")
            .fields(
                KvpFields.builder()
                    .add("name", KvpField.of("name of the user", "Andrea"))
                    .build())
            .build(),
        Schema.builder()
            .documentType("National ID Card")
            .documentDescription("National ID Cards are government-issued identification documents")
            .fields(
                KvpFields.builder()
                    .add("id", KvpField.of("Holder legal name as shown on the ID", "45DNFX"))
                    .build())
            .build()
    );

    @Test
    void should_merge_a_schemas() throws Exception {

        var result = service.mergeSchemaAndFetch(schemas);

        assertNotNull(result);
        assertNotNull(result.completedAt());
        assertNotNull(result.runningAt());
        assertNotNull(result.schema());
        assertNotNull(result.schema().documentType());
        assertNotNull(result.schema().documentDescription());
        assertNotNull(result.schema().fields());
        assertNotNull(result.schema().fields().get("name"));
        assertNotNull(result.schema().fields().get("id"));
        assertNotNull(result.schema().fields().get("name").example());
        assertNotNull(result.schema().fields().get("id").example());
        assertNotNull(result.schema().fields().get("name").description());
        assertNotNull(result.schema().fields().get("id").description());
        assertNotEquals(result.schema().documentDescription(), "Passport document to get the schema");
    }

    @Test
    void should_throw_an_exception_when_schemas_are_not_provided() {
        assertThrows(IllegalArgumentException.class, () -> service.mergeSchemaAndFetch(List.of(Schema.builder().build())));
        assertThrows(IllegalArgumentException.class, () -> service.mergeSchemaAndFetch(List.of()));
    }

    @Test
    void should_delete_merge_schema_request() throws Exception {

        var schema = Schema.builder()
            .documentType("Passport")
            .documentDescription("Passport document to get the schema")
            .fields(
                KvpFields.builder()
                    .add("name", KvpField.of("name of the user", "Andrea"))
                    .add("lastname", KvpField.of("lastname of the user", "Di Maio"))
                    .build())
            .build();

        var response = service.startMergeSchema(schemas);
        assertTrue(
            service.deleteRequest(
                response.metadata().id(),
                MergeSchemaDeleteParameters.builder()
                    .hardDelete(true)
                    .build()
            )
        );

        var ex = assertThrows(WatsonxException.class, () -> service.fetchRequest(response.metadata().id()));
        assertEquals(404, ex.statusCode());
    }

    @Test
    void should_throw_an_exception_when_the_job_does_not_exist() {
        var ex = assertThrows(WatsonxException.class, () -> service.fetchRequest("non-existing-id"));
        assertEquals(404, ex.statusCode());
    }
}
