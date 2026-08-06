---
id: cluster-schema-service
title: Cluster
---

# Cluster Schema Service

The `ClusterSchemaService` groups a set of custom document schemas into semantically similar clusters. It sends the schemas to the watsonx.ai clustering API and returns each cluster as a list of `ClusterSchemas` objects, letting you discover which document types are semantically related.

## Quick Start

```java
ClusterSchemaService service = ClusterSchemaService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .build();

ClusterSchemas passport = new ClusterSchemas("Passport",
    Schema.builder()
        .documentType("Passport")
        .documentDescription("A government-issued travel document")
        .fields(KvpFields.builder()
            .add("surname",     KvpField.of("The holder's last name", "SMITH"))
            .add("given_names", KvpField.of("The holder's first names", "ALICE MARIE"))
            .build())
        .build());

ClusterSchemas nationalId = new ClusterSchemas("National_ID",
    Schema.builder()
        .documentType("National ID Card")
        .documentDescription("A government-issued national identity document")
        .fields(KvpFields.builder()
            .add("surname",     KvpField.of("The holder's family name", "DOE"))
            .add("given_names", KvpField.of("The holder's first names", "JOHN JAMES"))
            .build())
        .build());

List<List<ClusterSchemas>> groups = service.clusterSchemaAndFetch(passport, nationalId);

for (List<ClusterSchemas> cluster : groups) {
    System.out.println("Cluster:");
    cluster.forEach(s -> System.out.println("  - " + s.documentName()));
}
// → Cluster:
// →   - Passport
// →   - National_ID
```

---

## Overview

The `ClusterSchemaService` enables you to:

- Group semantically similar document schemas into clusters automatically.
- Identify which document types share common fields and structure.
- Prepare input for schema consolidation workflows before merging.
- Optionally override the default foundation model used for clustering.

---

## Service Configuration

### Basic Setup

```java
ClusterSchemaService service = ClusterSchemaService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `projectId` | String | Conditional | Project ID where clustering will be performed |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

---

## Examples

### Cluster and Fetch in One Call

`clusterSchemaAndFetch` submits the request, polls until completion, and returns the grouped result directly:

```java
List<List<ClusterSchemas>> groups = service.clusterSchemaAndFetch(passport, nationalId, invoice);

for (List<ClusterSchemas> cluster : groups) {
    System.out.println("Cluster: " +
        cluster.stream().map(ClusterSchemas::documentName).collect(Collectors.joining(", ")));
}
// → Cluster: Passport, National_ID
// → Cluster: Invoice
```

### With a List of Schemas

Pass a `List` instead of varargs when the schemas are already collected:

```java
List<ClusterSchemas> schemas = buildSchemas(); // your own list
List<List<ClusterSchemas>> groups = service.clusterSchemaAndFetch(schemas);
```

### With Custom Parameters

Override project/space context or specify a custom semantic model:

```java
ClusterSchemaParameters parameters = ClusterSchemaParameters.builder()
    .projectId("other-project-id")
    .semanticConfig(
        ClusterSchemaSemanticConfig.builder()
            .defaultModelName("mistralai/mistral-medium-2505")
            .build()
    )
    .build();

List<List<ClusterSchemas>> groups =
    service.clusterSchemaAndFetch(parameters, passport, nationalId);
```

### Async: Start Then Poll

Use `startClusterSchema` to submit the job without waiting, then retrieve results later:

```java
// Submit the job
ClusterSchemaResponse response = service.startClusterSchema(passport, nationalId);
String jobId = response.metadata().id();

// … do other work …

// Retrieve the result
ClusterSchemaResponse result = service.fetchRequest(jobId);
System.out.println("Status: " + result.entity().results().status());

if (Status.COMPLETED.value().equals(result.entity().results().status())) {
    result.entity().results().schemas()
        .forEach(cluster -> System.out.println("Cluster: " +
            cluster.stream().map(ClusterSchemas::documentName)
                   .collect(Collectors.joining(", "))));
}
```

### Managing Requests

Cancel or remove a cluster schema job:

```java
ClusterSchemaResponse response = service.startClusterSchema(passport, nationalId);

boolean deleted = service.deleteRequest(
    response.metadata().id(),
    ClusterSchemaDeleteParameters.builder()
        .hardDelete(true)
        .build()
);

System.out.println("Deleted: " + deleted);
// → Deleted: true
```

> Deleting a non-existent ID returns `false`.

---

## Cluster Schema Parameters

`ClusterSchemaParameters` controls optional per-request overrides.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `schemas` | List&lt;ClusterSchemas&gt; | Override the list of schemas to cluster for this request |
| `semanticConfig` | ClusterSchemaSemanticConfig | Semantic model configuration |
| `projectId` | String | Override the default Project ID |
| `spaceId` | String | Override the default Space ID |
| `transactionId` | String | Request tracking ID |

### Using a Custom Foundation Model

Override the default clustering model with `defaultModelName`:

```java
ClusterSchemaSemanticConfig semanticConfig = ClusterSchemaSemanticConfig.builder()
    .defaultModelName("ibm/granite-4-h-small")
    .build();

ClusterSchemaParameters parameters = ClusterSchemaParameters.builder()
    .semanticConfig(semanticConfig)
    .build();

List<List<ClusterSchemas>> groups =
    service.clusterSchemaAndFetch(parameters, passport, nationalId);
```

---

## ClusterSchemaResponse

Returned by `startClusterSchema` and `fetchRequest`.

| Field | Type | Description |
|-------|------|-------------|
| `metadata().id()` | String | Unique identifier for the cluster schema request |
| `metadata().createdAt()` | String | Timestamp when the request was created |
| `metadata().projectId()` | String | Project ID associated with the request |
| `entity().parameters()` | Parameters | Parameters used for this clustering |
| `entity().results()` | ClusterSchemaResult | The current clustering result |

### ClusterSchemaResult

| Field | Type | Description |
|-------|------|-------------|
| `status()` | String | Current status: `Status.SUBMITTED`, `Status.RUNNING`, `Status.COMPLETED`, or `Status.FAILED` (use `Status.COMPLETED.value()` to compare) |
| `runningAt()` | String | Timestamp when processing started |
| `completedAt()` | String | Timestamp when processing completed or failed |
| `schemas()` | List&lt;List&lt;ClusterSchemas&gt;&gt; | The clusters - each inner list contains the schemas grouped together |
| `error()` | Error | Error details if status is `failed` |

### ClusterSchemas

Each entry in a cluster:

| Field | Type | Description |
|-------|------|-------------|
| `documentName()` | String | The name identifying this schema entry |
| `schema()` | Schema | The full schema definition |

---

## Related Resources

- [Create Schema Service](./create-schema-service) - Auto-generate schemas from documents
- [Improve Schema Service](./improve-schema-service) - Refine existing schemas
- [Merge Schema Service](./merge-schema-service) - Combine multiple schemas into one
- [Cluster Schema API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#cluster-schema)
