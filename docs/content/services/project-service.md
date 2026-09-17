---
id: project-service
title: Project Service
---

# Project Service

The `ProjectService` retrieves **IBM watsonx platform project metadata** via the Platform Projects API. It targets a dedicated endpoint (`https://api.dataplatform.cloud.ibm.com`) that is distinct from the watsonx.ai ML endpoint used by all other services.

## Quick Start

```java
ProjectService service = ProjectService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .build();

Project project = service.findProject("c2d2f907-4dfd-49f3-ac88-3c23a3830728").orElseThrow();
System.out.println(project.name());                 // → adm_project
System.out.println(project.type());                 // → wx
System.out.println(project.scope().bssAccountId()); // → 4d86deaa53914d5894e275ab280afffa
```

---

## Overview

The `ProjectService` enables you to:

- Retrieve full project metadata by project UUID.
- Inspect the account scope, storage configuration, catalog, and compute instances linked to a project.
- Access per-role COS credentials for the project's storage bucket.

---

## Service Configuration

### Basic Setup

```java
ProjectService service = ProjectService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `baseUrl` | String/CloudRegion | Yes | Platform Projects API base URL |
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |

> Either `apiKey` or `authenticator` must be provided.

---

## Examples

### Retrieving a Project

`findProject` returns an `Optional` — present when the project exists, empty when the API returns 404.

```java
// Throw if not found
Project project = service.findProject("c2d2f907-4dfd-49f3-ac88-3c23a3830728").orElseThrow();

System.out.println(project.guid());       // → c2d2f907-4dfd-49f3-ac88-3c23a3830728
System.out.println(project.name());       // → adm_project
System.out.println(project.type());       // → wx
System.out.println(project.createdAt());  // → 2026-09-17T10:10:29.450Z
```

```java
// Handle absence gracefully
service.findProject("c2d2f907-4dfd-49f3-ac88-3c23a3830728")
    .ifPresentOrElse(
        project -> System.out.println("Found: " + project.name()),
        ()      -> System.out.println("Project not found")
    );
```

### Inspecting the Project Scope

```java
Project project = service.findProject(PROJECT_ID).orElseThrow();
ProjectScope scope = project.scope();

System.out.println(scope.bssAccountId());     // → 4d86deaa53914d5894e275ab280afffa
System.out.println(scope.enforceMembers());   // → true
System.out.println(scope.samlInstanceName()); // → IBM w3id
```

### Accessing Storage Credentials

```java
Project project = service.findProject(PROJECT_ID).orElseThrow();
ProjectStorageProperties props = project.storage().properties();

System.out.println(props.bucketName());   // → admproject-donotdelete-pr-ycdgaky8tfbui9
System.out.println(props.bucketRegion()); // → us-south
System.out.println(props.endpointUrl());  // → https://s3.us-south.cloud-object-storage.appdomain.cloud

ProjectStorageCredential editorCreds = props.credentials().get("editor");
System.out.println(editorCreds.accessKeyId());     // → 59312addf5da45f4b5b73d04d44e25fa
System.out.println(editorCreds.secretAccessKey()); // → 7b1f4b020052f7...
```

### Listing Compute Instances

```java
Project project = service.findProject(PROJECT_ID).orElseThrow();
for (ProjectComputeInstance compute : project.compute()) {
    System.out.println(compute.name() + " [" + compute.type() + "] → " + compute.guid());
    // → wml-310002au2v [machine_learning] → 6a278d1e-85a6-4118-9091-9ea87676ca59
}
```

---

## Project

The response object promotes all fields from the API's `metadata` and `entity` envelope to the top level.

### Metadata fields

| Field | Type | Description |
|-------|------|-------------|
| `guid()` | String | Project UUID |
| `url()` | String | Resource URL path |
| `createdAt()` | String | Creation timestamp (ISO 8601) |
| `updatedAt()` | String | Last-updated timestamp (ISO 8601) |

### Entity fields

| Field | Type | Description |
|-------|------|-------------|
| `name()` | String | Human-readable project name |
| `description()` | String | Project description, or `null` |
| `type()` | String | Project type: `wx`, `cpd`, `wca`, `dpx`, `wxbi` |
| `publicProject()` | boolean | Whether the project is publicly accessible |
| `creator()` | String | Username of the project creator, or `null` |
| `creatorIamId()` | String | IAM ID of the project creator, or `null` |
| `generator()` | String | Generator label used to create the project, or `null` |
| `tags()` | List\<String\> | Project tags, or `null` |
| `scope()` | [ProjectScope](#projectscope) | Account scope information, or `null` |
| `storage()` | [ProjectStorage](#projectstorage) | Storage configuration, or `null` |
| `catalog()` | [ProjectCatalog](#projectcatalog) | Associated catalog, or `null` |
| `compute()` | List\<[ProjectComputeInstance](#projectcomputeinstance)\> | Compute instances, or `null` |
| `settings()` | Map\<String, Object\> | Project settings, or `null` |
| `members()` | List\<Map\<String, Object\>\> | Project members, or `null` |
| `tools()` | List\<String\> | Configured tool names, or `null` |
| `github()` | Map\<String, Object\> | GitHub integration config, or `null` |

---

## ProjectScope

| Field | Type | Description |
|-------|------|-------------|
| `bssAccountId()` | String | IBM Cloud BSS account ID |
| `enforceMembers()` | boolean | Whether membership enforcement is active |
| `samlInstanceName()` | String | SAML instance name (e.g. `"IBM w3id"`), or `null` |

---

## ProjectCatalog

| Field | Type | Description |
|-------|------|-------------|
| `guid()` | String | Catalog GUID |
| `publicCatalog()` | boolean | Whether the catalog is publicly accessible |

---

## ProjectStorage

| Field | Type | Description |
|-------|------|-------------|
| `type()` | String | Storage backend type (e.g. `"bmcos_object_storage"`) |
| `guid()` | String | Cloud Object Storage instance GUID |
| `properties()` | ProjectStorageProperties | Bucket properties including credentials |

### ProjectStorageProperties

| Field | Type | Description |
|-------|------|-------------|
| `bucketName()` | String | COS bucket name |
| `bucketRegion()` | String | Bucket region (e.g. `"us-south"`) |
| `endpointUrl()` | String | COS endpoint URL |
| `credentials()` | Map\<String, ProjectStorageCredential\> | Credentials keyed by role: `admin`, `editor`, `viewer` |

### ProjectStorageCredential

| Field | Type | Description |
|-------|------|-------------|
| `apiKey()` | String | IAM API key for this role |
| `serviceId()` | String | IAM service ID |
| `accessKeyId()` | String | HMAC access key ID |
| `secretAccessKey()` | String | HMAC secret access key |
| `resourceKeyCrn()` | String | CRN of the resource key, or `null` |

---

## ProjectComputeInstance

| Field | Type | Description |
|-------|------|-------------|
| `name()` | String | Instance name |
| `guid()` | String | Instance GUID |
| `type()` | String | Instance type (e.g. `"machine_learning"`) |
| `crn()` | String | CRN of the compute instance, or `null` |
| `credentials()` | Map\<String, Object\> | Instance credentials map, or `null` |

---

## Related Resources

- [IBM watsonx Platform Projects API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#platform-projects)
