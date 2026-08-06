/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.textprocessing.KvpFields;
import com.ibm.watsonx.ai.textprocessing.KvpFields.KvpField;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaException;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemas;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
public class ClusterSchemaIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");

    static final ClusterSchemaService clusterSchemaService = ClusterSchemaService.builder()
        .baseUrl(URL)
        .apiKey(API_KEY)
        .projectId(PROJECT_ID)
        .logRequests(true)
        .logResponses(true)
        .build();

    static final ClusterSchemas PASSPORT = new ClusterSchemas("Passport",
        Schema.builder()
            .documentType("Passport")
            .documentDescription("A government-issued travel document containing personal information and travel visas")
            .fields(KvpFields.builder()
                .add("surname", KvpField.of("The holder's last name", "SMITH"))
                .add("given_names", KvpField.of("The holder's first and middle names", "ALICE MARIE"))
                .add("nationality", KvpField.of("The holder's nationality", "ITALIAN"))
                .add("date_of_birth", KvpField.of("Date of birth in DD MMM YYYY format", "01 JAN 1990"))
                .add("passport_number", KvpField.of("Unique passport identifier", "YA1234567"))
                .build())
            .build());

    static final ClusterSchemas NATIONAL_ID = new ClusterSchemas("National_ID",
        Schema.builder()
            .documentType("National ID Card")
            .documentDescription("A government-issued national identity document for citizens")
            .fields(KvpFields.builder()
                .add("surname", KvpField.of("The holder's family name", "DOE"))
                .add("given_names", KvpField.of("The holder's first and middle names", "JOHN JAMES"))
                .add("date_of_birth", KvpField.of("Date of birth", "15/03/1985"))
                .add("id_number", KvpField.of("Unique national ID number", "AB123456C"))
                .build())
            .build());

    static final ClusterSchemas INVOICE = new ClusterSchemas("Invoice",
        Schema.builder()
            .documentType("Invoice")
            .documentDescription("A commercial document issued by a seller to a buyer for goods or services")
            .fields(KvpFields.builder()
                .add("invoice_number", KvpField.of("Unique invoice identifier", "INV-2024-001"))
                .add("invoice_date", KvpField.of("Date the invoice was issued", "2024-01-15"))
                .add("total_amount", KvpField.of("Total amount due including taxes", "1250.00 EUR"))
                .add("vendor_name", KvpField.of("Name of the company issuing the invoice", "Acme Corp"))
                .add("customer_name", KvpField.of("Name of the customer", "Big Client Ltd"))
                .build())
            .build());

    @Test
    void should_cluster_schemas_and_return_grouped_results() throws Exception {

        var groups = clusterSchemaService.clusterSchemaAndFetch(PASSPORT, NATIONAL_ID, INVOICE);

        assertNotNull(groups);
        assertFalse(groups.isEmpty());

        var allNames = groups.stream()
            .flatMap(cluster -> cluster.stream().map(ClusterSchemas::documentName))
            .toList();
        assertTrue(allNames.contains("Passport"), "Expected Passport in: " + allNames);
        assertTrue(allNames.contains("National_ID"), "Expected National_ID in: " + allNames);
        assertTrue(allNames.contains("Invoice"), "Expected Invoice in: " + allNames);
        assertEquals(3, allNames.size(), "Each schema name should appear exactly once, got: " + allNames);
    }

    @Test
    void should_start_cluster_schema_and_fetch_by_id() throws Exception {

        var response = clusterSchemaService.startClusterSchema(PASSPORT, NATIONAL_ID);
        assertNotNull(response);
        assertNotNull(response.metadata().id());
        assertNotNull(response.entity().results().status());

        // Poll until complete using fetchRequest
        String id = response.metadata().id();
        ClusterSchemaIT.waitUntilComplete(id);

        var completed = clusterSchemaService.fetchRequest(id);
        assertNotNull(completed.entity().results().schemas());

        assertTrue(clusterSchemaService.deleteRequest(id, ClusterSchemaDeleteParameters.builder().hardDelete(true).build()));
    }

    @Test
    void should_delete_cluster_schema_request() throws Exception {

        var response = clusterSchemaService.startClusterSchema(PASSPORT);
        assertNotNull(response.metadata().id());

        assertTrue(clusterSchemaService.deleteRequest(
            response.metadata().id(),
            ClusterSchemaDeleteParameters.builder().hardDelete(true).build()));

        var ex = assertThrows(WatsonxException.class,
            () -> clusterSchemaService.fetchRequest(response.metadata().id()));
        assertTrue(ex.statusCode() == 404);
    }

    @Test
    void should_return_false_when_deleting_non_existing_request() {
        assertFalse(clusterSchemaService.deleteRequest("non-existing-id"));
    }

    @Test
    void should_throw_when_fetching_non_existing_request() {
        var ex = assertThrows(WatsonxException.class,
            () -> clusterSchemaService.fetchRequest("non-existing-id"));
        assertTrue(ex.statusCode() == 404);
    }

    @Test
    void should_cluster_schemas_with_semantic_config() throws Exception {

        var groups = clusterSchemaService.clusterSchemaAndFetch(
            ClusterSchemaParameters.builder()
                .semanticConfig(ClusterSchemaSemanticConfig.builder()
                    .build())
                .build(),
            PASSPORT, NATIONAL_ID);

        assertNotNull(groups);
        assertFalse(groups.isEmpty());
    }

    private static void waitUntilComplete(String id) throws Exception {
        long deadline = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadline) {
            var r = clusterSchemaService.fetchRequest(id);
            var status = r.entity().results().status();
            if ("completed".equals(status) || "failed".equals(status))
                return;
            Thread.sleep(2000);
        }
        throw new ClusterSchemaException("timeout", "Timed out waiting for cluster schema job " + id);
    }
}
