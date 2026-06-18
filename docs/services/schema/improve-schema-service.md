---
layout: default
title: Improve
parent: Schema Services
nav_order: 2
permalink: /services/schema/improve-schema-service
---

# Improve Schema Service

The `ImproveSchemaService` provides functionality to enhance and refine existing document schemas. It takes a schema and improves field descriptions, fields, and optimizes the schema structure for better extraction accuracy.

## Quick Start

```java
ImproveSchemaService service = ImproveSchemaService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .build();

// Define an existing schema
Schema existingSchema = Schema.builder()
    .documentType("Passport")
    .documentDescription("Passport document")
    .fields(
        KvpFields.builder()
            .add("name", KvpField.of("name of the user", "Alan"))
            .add("lastname", KvpField.of("lastname of the user", "Wake"))
            .build()
    )
    .build();

// Improve the schema
ImproveSchemaResult result = service.improveSchemaAndFetch(existingSchema);
System.out.println("Original: " + existingSchema.documentDescription());
System.out.println("Improved: " + result.schema().documentDescription());
// → Original: Passport document
// → Improved: A Passport document serves as an official government-issued 
//             identification for international travel, verifying the holder's 
//             identity and nationality...
```

---

## Overview

The `ImproveSchemaService` enables you to:

- **Enhance field descriptions** with more detailed and accurate information
- **Optimize schema structure** for better extraction performance
- **Refine document descriptions** with comprehensive context

---

## Service Configuration

### Basic Setup

```java
ImproveSchemaService service = ImproveSchemaService.builder()
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
| `projectId` | String | Conditional | Project ID where schema improvement will be performed |
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

### Basic Schema Improvement

The simplest way to improve a schema is to use the `improveSchemaAndFetch` method:

```java
Schema existingSchema = Schema.builder()
    .documentType("Passport")
    .documentDescription("Passport document to get the schema")
    .fields(
        KvpFields.builder()
            .add("name", KvpField.of("name of the user", "Alan"))
            .add("lastname", KvpField.of("lastname of the user", "Wake"))
            .build()
    )
    .build();

ImproveSchemaResult result = service.improveSchemaAndFetch(existingSchema);

System.out.println("Improved Schema:");
System.out.println("  Document Type: " + result.schema().documentType());
System.out.println("  Description:   " + result.schema().documentDescription());
System.out.println("  Fields:        " + result.schema().fields().size());
```

### With Custom Parameters

Configure the improvement process with custom parameters:

```java
ImproveSchemaSemanticConfig semanticConfig = ImproveSchemaSemanticConfig.builder()
    .defaultModelName("mistralai/mistral-medium-2505")
    .build();

ImproveSchemaParameters parameters = ImproveSchemaParameters.builder()
    .semanticConfig(semanticConfig)
    .timeout(Duration.ofMinutes(5))
    .build();

ImproveSchemaResult result = service.improveSchemaAndFetch(existingSchema, parameters);
```

### With Additional Prompt Instructions

You can provide additional instructions to guide the improvement process:

```java
Schema schema = Schema.builder()
    .documentType("Invoice")
    .documentDescription("Invoice document")
    .additionalPromptInstructions("Focus on European VAT formats")
    .fields(
        KvpFields.builder()
            .add("invoice_number", KvpField.of("Invoice number", "INV-001"))
            .build()
    )
    .build();

ImproveSchemaResult result = service.improveSchemaAndFetch(schema);

// Instructions are preserved in improved schema
System.out.println(result.schema().additionalPromptInstructions());
// → "Focus on European VAT formats"
```

### Managing Requests

Delete an improvement job if needed:

```java
ImproveSchemaResponse response = service.startImproveSchema(existingSchema);

boolean deleted = service.deleteRequest(
    response.metadata().id(),
    ImproveSchemaDeleteParameters.builder()
        .hardDelete(true)
        .build()
);

System.out.println("Deleted: " + deleted);
// → Deleted: true
```

> Deleting a non-existent ID returns `false`.

---

## Schema Improvement Parameters

`ImproveSchemaParameters` controls how schema improvement is performed per request.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `semanticConfig` | ImproveSchemaSemanticConfig | Semantic model configuration for improvement |
| `timeout` | Duration | Override the service-level timeout for this request (synchronous only) |
| `projectId` | String | Override the default project ID |
| `spaceId` | String | Override the default space ID |
| `transactionId` | String | Request tracking ID |

### Using a Custom Foundation Model

Override the default model with `defaultModelName`:

```java
ImproveSchemaSemanticConfig semanticConfig = ImproveSchemaSemanticConfig.builder()
    .defaultModelName("ibm/granite-4-h-small")
    .build();

ImproveSchemaParameters parameters = ImproveSchemaParameters.builder()
    .semanticConfig(semanticConfig)
    .build();

ImproveSchemaResult result = service.improveSchemaAndFetch(existingSchema, parameters);
```

---

## ImproveSchemaResponse

Returned by `startImproveSchema` and `fetchRequest`.

| Field | Type | Description |
|-------|------|-------------|
| `metadata().id()` | String | Unique identifier for the schema improvement request |
| `metadata().createdAt()` | String | Timestamp when the request was created |
| `metadata().modifiedAt()` | String | Timestamp of the last update |
| `metadata().projectId()` | String | Project ID associated with the request |
| `entity().results()` | ImproveSchemaResult | The current schema improvement result |
| `entity().parameters()` | Parameters | Parameters used for this schema improvement |

### ImproveSchemaResult

| Field | Type | Description |
|-------|------|-------------|
| `status()` | String | Current status: `submitted`, `running`, `completed`, or `failed` |
| `runningAt()` | String | Timestamp when processing started |
| `completedAt()` | String | Timestamp when processing completed or failed |
| `schema()` | Schema | The improved document schema with enhanced field definitions |
| `groundingHints()` | GroundingHints | Field localization data (if available from original schema) |
| `error()` | Error | Error details if status is `failed` |

### Schema

| Field | Type | Description |
|-------|------|-------------|
| `documentType()` | String | The document type (e.g., "Invoice", "Contract", "Passport") |
| `documentDescription()` | String | Enhanced natural language description of the document |
| `fields()` | KvpFields | Map of field names to improved field definitions |
| `additionalPromptInstructions()` | String | Additional instructions preserved from original schema |

### KvpField

Each field in the improved schema contains:

| Field | Type | Description |
|-------|------|-------------|
| `description()` | String | Enhanced natural language description of the field |
| `example()` | String | Example value for the field |

---

## Related Resources

- [Text Extraction Service](../text-extraction-service) - Extract data using improved schemas
- [Text Classification Service](../text-classification-service) - Classify documents using schemas
- [Improve Schema API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#improve-schema)