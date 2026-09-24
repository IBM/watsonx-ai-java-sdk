/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.core.Json;

public class DataReferenceRoundTripTest {

    @Test
    void should_serialize_and_deserialize_connection_asset_reference() throws Exception {

        var ref = new DataReference(
            DataReference.TYPE_CONNECTION_ASSET,
            new CosDataConnection("my-conn-id"),
            new CosDataLocation("invoices/q1.pdf", "my-bucket", null));

        String json = Json.toJson(ref);
        JSONAssert.assertEquals("""
            {
              "type": "connection_asset",
              "connection": { "id": "my-conn-id" },
              "location": { "file_name": "invoices/q1.pdf", "bucket": "my-bucket" }
            }""", json, false);

        DataReference deserialized = Json.fromJson(json, DataReference.class);
        assertEquals(ref, deserialized);
    }

    @Test
    void should_serialize_and_deserialize_container_reference() throws Exception {

        var ref = new DataReference(
            DataReference.TYPE_CONTAINER,
            null,
            new CosDataLocation(null, null, "invoices/q1.pdf"));

        String json = Json.toJson(ref);
        JSONAssert.assertEquals("""
            {
              "type": "container",
              "location": { "path": "invoices/q1.pdf" }
            }""", json, false);

        assertFalse(json.contains("connection"), "connection must be absent for container type");
        assertFalse(json.contains("file_name"), "file_name must be absent for container type");
        assertFalse(json.contains("bucket"), "bucket must be absent for container type");

        DataReference deserialized = Json.fromJson(json, DataReference.class);
        assertEquals(ref, deserialized);
    }

    @Test
    void should_produce_container_data_reference_using_objectName_when_path_free() throws Exception {

        var ref = ContainerReference.container();
        assertNull(ref.path());

        var dataRef = ref.toDataReference("invoices/q1.pdf");

        String json = Json.toJson(dataRef);
        JSONAssert.assertEquals("""
            {
              "type": "container",
              "location": { "path": "invoices/q1.pdf" }
            }""", json, false);

        assertFalse(json.contains("connection"), "connection must be absent for container type");
        assertFalse(json.contains("file_name"), "file_name must be absent for container type");
        assertFalse(json.contains("bucket"), "bucket must be absent for container type");
    }

}
