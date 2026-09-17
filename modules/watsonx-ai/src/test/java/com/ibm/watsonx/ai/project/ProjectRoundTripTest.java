/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.core.Json;

public class ProjectRoundTripTest {

    static final String PROJECT_JSON = """
        {
          "metadata": {
            "guid": "c2d2f907-4dfd-49f3-ac88-3c23a3830728",
            "url": "/v2/projects/c2d2f907-4dfd-49f3-ac88-3c23a3830728",
            "created_at": "2026-09-17T10:10:29.450Z",
            "updated_at": "2026-09-17T12:01:24.631Z"
          },
          "entity": {
            "name": "adm_project",
            "generator": "wx-portal-projects",
            "description": "",
            "public": false,
            "type": "wx",
            "creator": "andrea.dimaio@it.ibm.com",
            "creator_iam_id": "IBMid-310002AU2V",
            "scope": {
              "bss_account_id": "4d86deaa53914d5894e275ab280afffa",
              "enforce_members": true,
              "saml_instance_name": "IBM w3id"
            },
            "catalog": {
              "public": false,
              "guid": "01a0aed8-a7f7-77d4-8d0a-7e3cd09653c2"
            },
            "compute": [
              {
                "name": "wml-310002au2v",
                "guid": "6a278d1e-85a6-4118-9091-9ea87676ca59",
                "type": "machine_learning",
                "crn": "crn:v1:bluemix:public:pm-20:us-south:a/abc:::",
                "credentials": {}
              }
            ]
          }
        }
        """;

    static final String PROJECT_FULL_JSON = """
        {
          "metadata": {
            "guid": "full-guid",
            "url": "/v2/projects/full-guid",
            "created_at": "2026-01-01T00:00:00.000Z",
            "updated_at": "2026-01-02T00:00:00.000Z"
          },
          "entity": {
            "name": "full_project",
            "generator": "test-gen",
            "description": "A full project",
            "public": true,
            "type": "cpd",
            "creator": "user@ibm.com",
            "creator_iam_id": "IBMid-ABC123",
            "scope": {
              "bss_account_id": "acct-001",
              "enforce_members": false
            },
            "tags": ["tag1", "tag2"],
            "storage": {
              "type": "bmcos_object_storage",
              "guid": "storage-guid-001",
              "properties": {
                "bucket_name": "my-bucket",
                "bucket_region": "us-south",
                "endpoint_url": "https://s3.us-south.cloud-object-storage.appdomain.cloud",
                "credentials": {
                  "admin": {
                    "api_key": "ak",
                    "service_id": "sid",
                    "access_key_id": "akid",
                    "secret_access_key": "sak",
                    "resource_key_crn": "crn:v1:..."
                  }
                }
              }
            },
            "catalog": {
              "public": true,
              "guid": "cat-guid-001"
            },
            "compute": [],
            "settings": { "key": "value" },
            "members": [ { "id": "m1", "role": "admin" } ],
            "tools": ["notebook", "pipeline"],
            "github": { "repo": "https://github.com/ibm/test" }
          }
        }
        """;

    @Test
    void should_deserialize_project_from_api_envelope() {
        var project = Json.fromJson(PROJECT_JSON, Project.class);

        assertEquals("c2d2f907-4dfd-49f3-ac88-3c23a3830728", project.guid());
        assertEquals("/v2/projects/c2d2f907-4dfd-49f3-ac88-3c23a3830728", project.url());
        assertEquals("2026-09-17T10:10:29.450Z", project.createdAt());
        assertEquals("2026-09-17T12:01:24.631Z", project.updatedAt());

        assertEquals("adm_project", project.name());
        assertEquals("", project.description());
        assertEquals("wx", project.type());
        assertFalse(project.publicProject());
        assertEquals("andrea.dimaio@it.ibm.com", project.creator());
        assertEquals("IBMid-310002AU2V", project.creatorIamId());
        assertEquals("wx-portal-projects", project.generator());

        var scope = project.scope();
        assertEquals("4d86deaa53914d5894e275ab280afffa", scope.bssAccountId());
        assertEquals(true, scope.enforceMembers());

        var catalog = project.catalog();
        assertEquals("01a0aed8-a7f7-77d4-8d0a-7e3cd09653c2", catalog.guid());
        assertFalse(catalog.publicCatalog());

        var compute = project.compute();
        assertEquals(1, compute.size());
        assertEquals("wml-310002au2v", compute.get(0).name());
        assertEquals("machine_learning", compute.get(0).type());
    }

    @Test
    void should_deserialize_full_project_with_storage_tags_and_extras() {
        var project = Json.fromJson(PROJECT_FULL_JSON, Project.class);

        assertEquals("full-guid", project.guid());
        assertTrue(project.publicProject());
        assertEquals(List.of("tag1", "tag2"), project.tags());

        var storage = project.storage();
        assertNotNull(storage);
        assertEquals("bmcos_object_storage", storage.type());
        assertEquals("storage-guid-001", storage.guid());

        var props = storage.properties();
        assertNotNull(props);
        assertEquals("my-bucket", props.bucketName());
        assertEquals("us-south", props.bucketRegion());
        assertEquals("https://s3.us-south.cloud-object-storage.appdomain.cloud", props.endpointUrl());

        var cred = props.credentials().get("admin");
        assertNotNull(cred);
        assertEquals("ak", cred.apiKey());
        assertEquals("sid", cred.serviceId());
        assertEquals("akid", cred.accessKeyId());
        assertEquals("sak", cred.secretAccessKey());
        assertEquals("crn:v1:...", cred.resourceKeyCrn());

        assertNotNull(project.settings());
        assertEquals("value", project.settings().get("key"));

        assertNotNull(project.members());
        assertEquals(1, project.members().size());
        assertEquals("m1", project.members().get(0).get("id"));

        assertEquals(List.of("notebook", "pipeline"), project.tools());

        assertNotNull(project.github());
        assertEquals("https://github.com/ibm/test", project.github().get("repo"));
    }

    @Test
    void should_round_trip_project_through_json() {
        var first = Json.fromJson(PROJECT_JSON, Project.class);
        var second = Json.fromJson(PROJECT_JSON, Project.class);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void should_round_trip_full_project_through_json() {
        var first = Json.fromJson(PROJECT_FULL_JSON, Project.class);
        var second = Json.fromJson(PROJECT_FULL_JSON, Project.class);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void should_handle_null_optional_fields() {
        var project = Project.builder()
            .guid("abc-123")
            .name("minimal")
            .type("cpd")
            .build();

        assertEquals("abc-123", project.guid());
        assertEquals("minimal", project.name());
        assertEquals("cpd", project.type());
        assertFalse(project.publicProject());
        assertNull(project.tags());
        assertNull(project.storage());
        assertNull(project.compute());
        assertNull(project.members());
        assertNull(project.settings());
        assertNull(project.tools());
        assertNull(project.github());
        assertNull(project.description());
        assertNull(project.creator());
        assertNull(project.creatorIamId());
        assertNull(project.generator());
        assertNull(project.scope());
        assertNull(project.catalog());
        assertNull(project.url());
        assertNull(project.createdAt());
        assertNull(project.updatedAt());
    }

    @Test
    void should_have_meaningful_toString() {
        var project = Project.builder()
            .guid("guid-1")
            .name("my-project")
            .build();

        var str = project.toString();
        assertTrue(str.contains("guid-1"));
        assertTrue(str.contains("my-project"));
        assertTrue(str.contains("Project"));
    }

    @Test
    void should_not_be_equal_to_null_or_different_type() {
        var project = Project.builder().guid("g1").build();
        assertNotEquals(project, null);
        assertNotEquals(project, "string");
    }

    @Test
    void should_be_equal_to_itself() {
        var project = Project.builder().guid("g1").build();
        assertEquals(project, project);
    }

    @Test
    void should_not_be_equal_when_fields_differ() {
        var a = Project.builder().guid("g1").name("alpha").build();
        var b = Project.builder().guid("g2").name("alpha").build();
        assertNotEquals(a, b);
    }

    @Test
    void should_not_be_equal_when_collections_differ() {
        var a = Project.builder()
            .guid("g1")
            .tags(List.of("x"))
            .settings(Map.of("k", "v"))
            .members(List.of(Map.of("id", "m1")))
            .tools(List.of("t1"))
            .github(Map.of("repo", "r"))
            .build();
        var b = Project.builder()
            .guid("g1")
            .tags(List.of("y"))
            .settings(Map.of("k", "other"))
            .members(List.of(Map.of("id", "m2")))
            .tools(List.of("t2"))
            .github(Map.of("repo", "s"))
            .build();
        assertNotEquals(a, b);
    }

    @Test
    void should_be_equal_when_all_fields_match() {
        var a = Project.builder()
            .guid("g1").url("/v2/projects/g1")
            .createdAt("2026-01-01").updatedAt("2026-01-02")
            .name("proj").description("desc").type("wx")
            .publicProject(true).creator("user").creatorIamId("iam-1")
            .generator("gen").tags(List.of("t1"))
            .settings(Map.of("k", "v")).members(List.of(Map.of("id", "m1")))
            .tools(List.of("t1")).github(Map.of("repo", "r"))
            .build();
        var b = Project.builder()
            .guid("g1").url("/v2/projects/g1")
            .createdAt("2026-01-01").updatedAt("2026-01-02")
            .name("proj").description("desc").type("wx")
            .publicProject(true).creator("user").creatorIamId("iam-1")
            .generator("gen").tags(List.of("t1"))
            .settings(Map.of("k", "v")).members(List.of(Map.of("id", "m1")))
            .tools(List.of("t1")).github(Map.of("repo", "r"))
            .build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void should_have_immutable_collections() {
        var project = Json.fromJson(PROJECT_FULL_JSON, Project.class);
        assertThrows(UnsupportedOperationException.class, () -> project.tags().add("new"));
        assertThrows(UnsupportedOperationException.class, () -> project.settings().put("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> project.members().add(Map.of()));
        assertThrows(UnsupportedOperationException.class, () -> project.tools().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> project.github().put("x", "y"));
        assertThrows(UnsupportedOperationException.class,
            () -> project.storage().properties().credentials().put("x", null));
        assertThrows(UnsupportedOperationException.class,
            () -> project.compute().add(null));
    }

    @Test
    void should_deserialize_storage_without_credentials() {
        var json = """
            {
              "metadata": { "guid": "s1" },
              "entity": {
                "name": "p",
                "type": "wx",
                "storage": {
                  "type": "bmcos_object_storage",
                  "guid": "sguid",
                  "properties": {
                    "bucket_name": "b",
                    "bucket_region": "us-south",
                    "endpoint_url": "https://s3.example.com"
                  }
                }
              }
            }
            """;
        var project = Json.fromJson(json, Project.class);
        assertNotNull(project.storage());
        assertNotNull(project.storage().properties());
        assertNull(project.storage().properties().credentials());
    }

    @Test
    void should_deserialize_storage_without_properties() {
        var json = """
            {
              "metadata": { "guid": "s2" },
              "entity": {
                "name": "p",
                "type": "wx",
                "storage": {
                  "type": "bmcos_object_storage",
                  "guid": "sguid"
                }
              }
            }
            """;
        var project = Json.fromJson(json, Project.class);
        assertNotNull(project.storage());
        assertNull(project.storage().properties());
    }

    @Test
    void should_not_be_equal_when_one_field_is_null_and_other_is_not() {
        var base = Project.builder().guid("g").url("u").createdAt("c").updatedAt("u2")
            .name("n").description("d").type("t").creator("cr").creatorIamId("ci")
            .generator("gen").tags(List.of("t")).settings(Map.of("k", "v"))
            .members(List.of(Map.of("id", "m"))).tools(List.of("tl")).github(Map.of("r", "v"))
            .build();

        assertNotEquals(base, Project.builder().build());
        assertNotEquals(Project.builder().build(), base);
        assertNotEquals(base, Project.builder().guid("g").build());
        assertNotEquals(Project.builder().guid("g").build(), base);
        assertNotEquals(base, Project.builder().guid("g").url("u").build());
        assertNotEquals(Project.builder().guid("g").url("u").build(), base);
        assertNotEquals(base, Project.builder().guid("g").url("u").createdAt("c").build());
        assertNotEquals(Project.builder().guid("g").url("u").createdAt("c").build(), base);
    }

    @Test
    void should_not_be_equal_when_null_this_vs_non_null_other() {
        var empty = Project.builder().build();

        assertNotEquals(empty, Project.builder().name("n").build());
        assertNotEquals(empty, Project.builder().description("d").build());
        assertNotEquals(empty, Project.builder().type("t").build());
        assertNotEquals(empty, Project.builder().creator("cr").build());
        assertNotEquals(empty, Project.builder().creatorIamId("ci").build());
        assertNotEquals(empty, Project.builder().generator("gen").build());
        assertNotEquals(empty, Project.builder().tags(List.of("t")).build());
        assertNotEquals(empty, Project.builder().settings(Map.of("k", "v")).build());
        assertNotEquals(empty, Project.builder().members(List.of(Map.of())).build());
        assertNotEquals(empty, Project.builder().tools(List.of("t")).build());
        assertNotEquals(empty, Project.builder().github(Map.of("k", "v")).build());
        assertNotEquals(empty, Project.builder()
            .scope(new ProjectScope("acct", false, null)).build());
        assertNotEquals(empty, Project.builder()
            .storage(new ProjectStorage("type", "guid", null)).build());
        assertNotEquals(empty, Project.builder()
            .catalog(new ProjectCatalog("cat-guid", false)).build());
        assertNotEquals(empty, Project.builder()
            .compute(List.of()).build());
    }

    @Test
    void should_deserialize_storage_with_null_credential_entry() {
        var json = """
            {
              "metadata": { "guid": "s3" },
              "entity": {
                "name": "p",
                "type": "wx",
                "storage": {
                  "type": "bmcos_object_storage",
                  "guid": "sguid",
                  "properties": {
                    "bucket_name": "b",
                    "bucket_region": "us-south",
                    "endpoint_url": "https://s3.example.com",
                    "credentials": {
                      "admin": null
                    }
                  }
                }
              }
            }
            """;
        var project = Json.fromJson(json, Project.class);
        assertNotNull(project.storage());
        assertNotNull(project.storage().properties());
        assertNotNull(project.storage().properties().credentials());
    }

    @Test
    void should_not_be_equal_when_non_null_object_fields_differ() {
        var scopeA = new ProjectScope("acct-a", true, null);
        var scopeB = new ProjectScope("acct-b", false, null);
        var storageA = new ProjectStorage("type-a", "guid-a", null);
        var storageB = new ProjectStorage("type-b", "guid-b", null);
        var catalogA = new ProjectCatalog("cat-a", true);
        var catalogB = new ProjectCatalog("cat-b", false);

        assertNotEquals(
            Project.builder().scope(scopeA).build(),
            Project.builder().scope(scopeB).build());
        assertNotEquals(
            Project.builder().storage(storageA).build(),
            Project.builder().storage(storageB).build());
        assertNotEquals(
            Project.builder().catalog(catalogA).build(),
            Project.builder().catalog(catalogB).build());
        assertNotEquals(
            Project.builder().compute(List.of()).build(),
            Project.builder().compute(List.of(
                new ProjectComputeInstance("n", "g", "t", "c", null))).build());
        assertNotEquals(
            Project.builder().settings(Map.of("a", "1")).build(),
            Project.builder().settings(Map.of("b", "2")).build());
        assertNotEquals(
            Project.builder().members(List.of(Map.of("id", "a"))).build(),
            Project.builder().members(List.of(Map.of("id", "b"))).build());
        assertNotEquals(
            Project.builder().tools(List.of("tool-a")).build(),
            Project.builder().tools(List.of("tool-b")).build());
        assertNotEquals(
            Project.builder().github(Map.of("r", "a")).build(),
            Project.builder().github(Map.of("r", "b")).build());
        assertNotEquals(
            Project.builder().publicProject(true).build(),
            Project.builder().publicProject(false).build());
    }

    @Test
    void should_not_be_equal_when_string_fields_differ() {
        var base = Project.builder()
            .guid("g").url("u").createdAt("c").updatedAt("u2")
            .name("n").description("d").type("t").creator("cr")
            .creatorIamId("ci").generator("gen")
            .build();

        assertNotEquals(base, Project.builder()
            .guid("g").url("u").createdAt("c").updatedAt("DIFF")
            .name("n").description("d").type("t").creator("cr")
            .creatorIamId("ci").generator("gen").build());
        assertNotEquals(base, Project.builder()
            .guid("g").url("u").createdAt("c").updatedAt("u2")
            .name("DIFF").description("d").type("t").creator("cr")
            .creatorIamId("ci").generator("gen").build());
        assertNotEquals(base, Project.builder()
            .guid("g").url("u").createdAt("c").updatedAt("u2")
            .name("n").description("DIFF").type("t").creator("cr")
            .creatorIamId("ci").generator("gen").build());
        assertNotEquals(base, Project.builder()
            .guid("g").url("u").createdAt("c").updatedAt("u2")
            .name("n").description("d").type("DIFF").creator("cr")
            .creatorIamId("ci").generator("gen").build());
        assertNotEquals(base, Project.builder()
            .guid("g").url("u").createdAt("c").updatedAt("u2")
            .name("n").description("d").type("t").creator("DIFF")
            .creatorIamId("ci").generator("gen").build());
        assertNotEquals(base, Project.builder()
            .guid("g").url("u").createdAt("c").updatedAt("u2")
            .name("n").description("d").type("t").creator("cr")
            .creatorIamId("DIFF").generator("gen").build());
        assertNotEquals(base, Project.builder()
            .guid("g").url("u").createdAt("c").updatedAt("u2")
            .name("n").description("d").type("t").creator("cr")
            .creatorIamId("ci").generator("DIFF").build());
    }
}
