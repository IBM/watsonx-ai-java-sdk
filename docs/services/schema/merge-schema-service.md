---
layout: default
title: Merge
parent: Schema Services
nav_order: 3
permalink: /services/schema/merge-schema-service
---

# Merge Schema Service

The `MergeSchemaService` provides functionality to combine multiple document schemas into a single unified schema. It intelligently merges field definitions, descriptions, and structures from different schemas to create a comprehensive schema that covers all input documents.

## Quick Start

```java
MergeSchemaService service = MergeSchemaService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .build();

// Define schemas to merge
List<Schema> schemas = List.of(
    Schema.builder()
        .documentType("Passport")
        .documentDescription("Passport document")
        .fields(
            KvpFields.builder()
                .add("name", KvpField.of("Holder's name", "John"))
                .build()
        )
        .build(),
    Schema.builder()
        .documentType("National ID Card")
        .documentDescription("National ID Card document")
        .fields(
            KvpFields.builder()
                .add("id", KvpField.of("ID number", "ABC123"))
                .build()
        )
        .build()
);

// Merge the schemas
MergeSchemaResult result = service.mergeSchemaAndFetch(schemas);
System.out.println("Merged Type: " + result.schema().documentType());
System.out.println("Merged Description: " + result.schema().documentDescription());
// → Merged Type: Identification Document
// → Merged Description: Identification documents including Passports and 
//                       National ID Cards, which are government-issued...
```

---

## Overview

The `MergeSchemaService` enables you to:

- **Combine multiple schemas** into a single unified schema
- **Merge field definitions** from different document types
- **Create comprehensive descriptions** that encompass all input schemas
- **Optimize schema structure** for better extraction across document variants

---

## Service Configuration

### Basic Setup

```java
MergeSchemaService service = MergeSchemaService.builder()
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
| `projectId` | String | Conditional | Project ID where schema merge will be performed |
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

### Basic Schema Merge

The simplest way to merge schemas is to use the `mergeSchemaAndFetch` method:

```java
List<Schema> schemas = List.of(
    Schema.builder()
        .documentType("Invoice")
        .documentDescription("Commercial invoice")
        .fields(
            KvpFields.builder()
                .add("invoice_number", KvpField.of("Invoice number", "INV-001"))
                .add("total", KvpField.of("Total amount", "1000.00"))
                .build()
        )
        .build(),
    Schema.builder()
        .documentType("Receipt")
        .documentDescription("Payment receipt")
        .fields(
            KvpFields.builder()
                .add("receipt_number", KvpField.of("Receipt number", "REC-001"))
                .add("amount", KvpField.of("Payment amount", "500.00"))
                .build()
        )
        .build()
);

MergeSchemaResult result = service.mergeSchemaAndFetch(schemas);

System.out.println("Merged Schema:");
System.out.println("Document Type: " + result.schema().documentType());
System.out.println("Description:   " + result.schema().documentDescription());
System.out.println("Fields:        " + result.schema().fields().size());
```

### With Custom Parameters

Configure the merge process with custom parameters:

```java
MergeSchemaSemanticConfig semanticConfig = MergeSchemaSemanticConfig.builder()
    .defaultModelName("mistralai/mistral-medium-2505")
    .build();

MergeSchemaParameters parameters = MergeSchemaParameters.builder()
    .semanticConfig(semanticConfig)
    .timeout(Duration.ofMinutes(5))
    .build();

MergeSchemaResult result = service.mergeSchemaAndFetch(schemas, parameters);
```

### Managing Requests

Delete a merge job if needed:

```java
MergeSchemaResponse response = service.startMergeSchema(schemas);

boolean deleted = service.deleteRequest(
    response.metadata().id(),
    MergeSchemaDeleteParameters.builder()
        .hardDelete(true)
        .build()
);

System.out.println("Deleted: " + deleted);
// → Deleted: true
```

> Deleting a non-existent ID returns `false`.

---

## Schema Merge Parameters

`MergeSchemaParameters` controls how schema merging is performed per request.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `semanticConfig` | MergeSchemaSemanticConfig | Semantic model configuration for merging |
| `timeout` | Duration | Override the service-level timeout for this request (synchronous only) |
| `projectId` | String | Override the default project ID |
| `spaceId` | String | Override the default space ID |
| `transactionId` | String | Request tracking ID |

### Using a Custom Foundation Model

Override the default model with `defaultModelName`:

```java
MergeSchemaSemanticConfig semanticConfig = MergeSchemaSemanticConfig.builder()
    .defaultModelName("ibm/granite-4-h-small")
    .build();

MergeSchemaParameters parameters = MergeSchemaParameters.builder()
    .semanticConfig(semanticConfig)
    .build();

MergeSchemaResult result = service.mergeSchemaAndFetch(schemas, parameters);
```

---

## MergeSchemaResponse

Returned by `startMergeSchema` and `fetchRequest`.

| Field | Type | Description |
|-------|------|-------------|
| `metadata().id()` | String | Unique identifier for the schema merge request |
| `metadata().createdAt()` | String | Timestamp when the request was created |
| `metadata().modifiedAt()` | String | Timestamp of the last update |
| `metadata().projectId()` | String | Project ID associated with the request |
| `entity().results()` | MergeSchemaResult | The current schema merge result |
| `entity().parameters()` | Parameters | Parameters used for this schema merge |

### MergeSchemaResult

| Field | Type | Description |
|-------|------|-------------|
| `status()` | String | Current status: `submitted`, `running`, `completed`, or `failed` |
| `runningAt()` | String | Timestamp when processing started |
| `completedAt()` | String | Timestamp when processing completed or failed |
| `schema()` | Schema | The merged document schema with unified field definitions |
| `groundingHints()` | GroundingHints | Field localization data (if available from original schemas) |
| `error()` | Error | Error details if status is `failed` |

### Schema

| Field | Type | Description |
|-------|------|-------------|
| `documentType()` | String | The merged document type (e.g., "Identification Document") |
| `documentDescription()` | String | Comprehensive description covering all input schemas |
| `fields()` | KvpFields | Map of field names to merged field definitions |
| `additionalPromptInstructions()` | String | Additional instructions (if provided in input schemas) |

### KvpField

Each field in the merged schema contains:

| Field | Type | Description |
|-------|------|-------------|
| `description()` | String | Unified description of the field across all schemas |
| `example()` | String | Example value for the field |

---

## Related Resources

- [Text Extraction Service](../text-extraction-service) - Extract data using merged schemas
- [Text Classification Service](../text-classification-service) - Extract data using merged schemas
- [Merge Schema API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#merge-schema)