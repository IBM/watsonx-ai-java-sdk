---
layout: default
title: Create
parent: Schema Services
nav_order: 1
permalink: /services/schema/create-schema-service
---

# Create Schema Service

The `CreateSchemaService` provides functionality to automatically generate document schemas from files. It analyzes documents and extracts structured field definitions, enabling automated schema creation for downstream [TextExtraction](../text-extraction-service) and [TextClassification](../text-classification-service) services.

## Quick Start

```java
CreateSchemaService service = CreateSchemaService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .cosUrl(CosUrl.US_SOUTH)
    .documentReference(CONNECTION_ID, BUCKET_NAME)
    .build();

var properties = CreateSchemaParameters.builder()
    .languages(Language.ENGLISH)
    .mode(Mode.HIGH_QUALITY)
    .removeUploadedFile(true)
    .timeout(Duration.ofMinutes(10))
    .build();

CreateSchemaResult result = 
    service.uploadCreateSchemaAndFetch(new File("path/to/invoice.pdf"), properties);

System.out.println("Document Type: " + result.schema().documentType());
System.out.println("Description:   " + result.schema().documentDescription());
System.out.println("Schema: "        + result.schema());
// → Document Type: Invoice
// → Description:   A vendor-issued invoice listing purchased items, prices, and payment information.
// → Schema:  Complete schema
```

---

## Overview

The `CreateSchemaService` enables you to:

- Automatically generate document schemas with field definitions from documents.
- Upload local files or input streams directly to COS before schema creation.
- Extract grounding hints with bounding box coordinates for field localization.
- Configure OCR settings for language, rotation correction, and processing mode.
- Control extraction quality, page limits, and semantic model configuration.

---

## Service Configuration

### Basic Setup

```java
CreateSchemaService service = CreateSchemaService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .cosUrl(CosUrl.US_SOUTH)
    .documentReference(CONNECTION_ID, BUCKET_NAME)
    .build();
```

### Using a Separate COS Authenticator

If your **Cloud Object Storage** uses different credentials than your **watsonx.ai** service, provide a dedicated `cosAuthenticator`:

```java
CreateSchemaService service = CreateSchemaService.builder()
    .apiKey(WATSONX_API_KEY)
    .cosAuthenticator(IBMCloudAuthenticator.withKey(COS_API_KEY)) // separate COS authenticator
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .cosUrl(CosUrl.US_SOUTH)
    .documentReference(CONNECTION_ID, BUCKET_NAME)
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `cosAuthenticator` | Authenticator | No | Separate authenticator for COS operations (defaults to main authenticator) |
| `projectId` | String | Conditional | Project ID where schema creation will be performed |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `cosUrl` | String/CosUrl | Yes | Cloud Object Storage base URL |
| `documentReference` | CosReference | Yes | Connection ID and bucket name for input documents |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

---

## Examples

The simplest way to create a schema is to use the `uploadCreateSchemaAndFetch` method. This uploads a file and generates the schema in one call.

**From a local file:**

```java
CreateSchemaResult result = service.uploadCreateSchemaAndFetch(new File("invoice.pdf"));
System.out.println("Document Type:   " + result.schema().documentType());
System.out.println("Description:     " + result.schema().documentDescription());
System.out.println("Schema: "          + result.schema());
// → Document Type:   Invoice
// → Description:     A vendor-issued invoice ...
// → Schema:          Complete schema
```

With parameters:

```java
var parameters = CreateSchemaParameters.builder()
    .mode(Mode.HIGH_QUALITY)
    .languages(Language.ENGLISH)
    .enableGrounding(true)
    .maxPagesToProcess(10)
    .build();

CreateSchemaResult result = service.uploadCreateSchemaAndFetch(new File("invoice.pdf"), parameters);
```

**Automatic file cleanup**

Set `removeUploadedFile(true)` to delete the uploaded file from COS asynchronously after schema creation completes:

```java
var parameters = CreateSchemaParameters.builder()
    .mode(Mode.HIGH_QUALITY)
    .languages(Language.ENGLISH)
    .removeUploadedFile(true)
    .build();

service.uploadCreateSchemaAndFetch(new File("invoice.pdf"), parameters);
```

> **Note:** `removeUploadedFile` is only supported with the synchronous variants. For other cases, call `service.deleteFile(BUCKET_NAME, fileName)` manually after processing.

### Grounding Hints

When `enableGrounding(true)` is set, you get field localization data with bounding box coordinates:

```java
var parameters = CreateSchemaParameters.builder()
    .mode(Mode.HIGH_QUALITY)
    .enableGrounding(true)
    .build();

CreateSchemaResult result = service.uploadCreateSchemaAndFetch(new File("invoice.pdf"), parameters);
GroundingHints hints = result.groundingHints();
List<Double> bbox = hints.bbox("invoice_number");
// Returns [x1, y1, x2, y2] where (x1,y1) is top-left, (x2,y2) is bottom-right
```

### Using Generated Schema with Text Extraction

One of the most powerful use cases for Create Schema Service is to automatically generate schemas and use them directly with the [TextExtraction Service](../text-extraction-service) for structured data extraction.

**Complete workflow example:**

```java
// Step 1: Set up Create Schema Service
CreateSchemaService createSchemaService = CreateSchemaService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .cosUrl(CosUrl.US_SOUTH)
    .documentReference(CONNECTION_ID, BUCKET_NAME)
    .build();

// Step 2: Generate schema from a sample document
CreateSchemaResult schemaResult = createSchemaService.uploadCreateSchemaAndFetch(
    new File("invoice.pdf"),
    CreateSchemaParameters.builder()
        .languages(Language.ENGLISH)
        .mode(Mode.HIGH_QUALITY)
        .timeout(Duration.ofMinutes(10))
        .removeUploadedFile(true)
        .build()
);

// Step 3: Set up Text Extraction Service
TextExtractionService textExtractionService = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .cosUrl(CosUrl.US_SOUTH)
    .documentReference(CONNECTION_ID, BUCKET_NAME)
    .resultReference(CONNECTION_ID, RESULT_BUCKET_NAME)
    .build();

// Step 4: Use the generated schema for extraction
TextExtractionParameters extractionParams = TextExtractionParameters.builder()
    .languages(Language.ENGLISH)
    .mode(Mode.HIGH_QUALITY)
    .timeout(Duration.ofMinutes(10))
    .removeUploadedFile(true)
    .kvpMode(KvpMode.GENERIC_WITH_SEMANTIC)
    .semanticConfig(
        TextExtractionSemanticConfig.builder()
            .schemas(schemaResult.schema())  // Use the auto-generated schema
            .build()
    )
    .build();

// Step 5: Extract data from documents using the schema
String extractedData = textExtractionService.uploadExtractAndFetch(
    new File("invoice.pdf"),
    extractionParams
);

System.out.println("Extracted Data:\n" + extractedData);
// → Extracted Data:
// → ## VENDOR NAME
// → Invoice Number: INV-12345
// → Date: 2024-01-15
// → Total Amount: $1,234.56
// → ...
```

**Key benefits of this approach:**

- **Automatic field detection**: No need to manually define schema fields
- **Consistent extraction**: The same schema can be reused across similar documents
- **Reduced setup time**: Generate schemas from sample documents in minutes
- **Improved accuracy**: Schemas are optimized for your specific document types

**Best practices:**

1. **Use representative samples**: Generate schemas from documents that contain all expected fields
2. **Enable grounding**: Use `enableGrounding(true)` to verify field locations
3. **Reuse schemas**: Save generated schemas and reuse them for batch processing
4. **Validate results**: Review the generated schema before using it in production

### Managing Requests

Use `deleteRequest` to cancel or remove a schema creation job. Pass `hardDelete(true)` to also remove the job metadata:

```java
CreateSchemaResponse response = service.uploadAndStartCreateSchema(new File("invoice.pdf"));

boolean deleted = service.deleteRequest(
    response.metadata().id(),
    CreateSchemaDeleteParameters.builder()
        .hardDelete(true)
        .build()
);

System.out.println("Deleted: " + deleted);
// → Deleted: true
```

> Deleting a non-existent ID returns `false`.

---

## Schema Creation Parameters

`CreateSchemaParameters` controls how schema creation is performed per request.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `mode` | Mode | Processing quality: `STANDARD` (faster) or `HIGH_QUALITY` (more accurate, slower) |
| `ocrMode` | OcrMode | OCR processing mode: `DISABLED`, `ENABLED`, `FORCED`, or `AUTO`. Leaving unset lets the service choose automatically |
| `autoRotationCorrection` | Boolean | Automatically correct document rotation before OCR |
| `languages` | Language... | Expected languages in the document (ISO 639) |
| `additionalPromptInstructions` | String | Custom instructions to guide schema generation |
| `enableGrounding` | Boolean | Include bounding box coordinates for field localization |
| `maxPagesToProcess` | Integer | Maximum number of pages to analyze for schema creation |
| `semanticConfig` | CreateSchemaSemanticConfig | Semantic model configuration |
| `removeUploadedFile` | Boolean | Delete the uploaded file from COS after schema creation (synchronous only) |
| `documentReference` | CosReference | Override the default COS connection and bucket for this request |
| `timeout` | Duration | Override the service-level timeout for this request (synchronous only) |
| `projectId` | String | Override the default project ID |
| `spaceId` | String | Override the default space ID |
| `transactionId` | String | Request tracking ID |

### Processing Modes

| Value | Description |
|-------|-------------|
| `Mode.STANDARD` | Faster processing with standard accuracy |
| `Mode.HIGH_QUALITY` | Slower processing with higher accuracy and better field detection |

### OCR Modes

| Value | Sent to API | Description |
|-------|-------------|-------------|
| `OcrMode.AUTO` | *(not sent)* | Service automatically selects the best OCR option |
| `OcrMode.DISABLED` | `"disabled"` | OCR is disabled; document must contain native text |
| `OcrMode.ENABLED` | `"enabled"` | OCR is applied when the service determines it is needed |
| `OcrMode.FORCED` | `"forced"` | OCR is always applied regardless of document content |

### Using a Custom Foundation Model

Override the default model (`mistral-small-3-1-24b-instruct-2503`) with `defaultModelName`:

```java
CreateSchemaSemanticConfig semanticConfig = CreateSchemaSemanticConfig.builder()
    .defaultModelName("mistralai/mistral-medium-2505")
    .build();

CreateSchemaParameters parameters = CreateSchemaParameters.builder()
    .mode(Mode.HIGH_QUALITY)
    .semanticConfig(semanticConfig)
    .build();

CreateSchemaResult result = 
    service.uploadCreateSchemaAndFetch(new File("invoice.pdf"), parameters);
```

---

## CreateSchemaResponse

Returned by `startCreateSchema`, `uploadAndStartCreateSchema`, and `fetchRequest`.

| Field | Type | Description |
|-------|------|-------------|
| `metadata().id()` | String | Unique identifier for the schema creation request |
| `metadata().createdAt()` | String | Timestamp when the request was created |
| `metadata().modifiedAt()` | String | Timestamp of the last update |
| `metadata().projectId()` | String | Project ID associated with the request |
| `entity().results()` | CreateSchemaResult | The current schema creation result |
| `entity().documentReference()` | DataReference | Reference to the input document in COS |
| `entity().parameters()` | Parameters | Parameters used for this schema creation |

### CreateSchemaResult

| Field | Type | Description |
|-------|------|-------------|
| `status()` | String | Current status: `submitted`, `running`, `completed`, or `failed` |
| `runningAt()` | String | Timestamp when processing started |
| `completedAt()` | String | Timestamp when processing completed or failed |
| `numberPagesProcessed()` | Integer | Number of pages processed |
| `totalPages()` | Integer | Total number of pages in the document |
| `schema()` | Schema | The generated document schema with field definitions |
| `groundingHints()` | GroundingHints | Field localization data with bounding boxes (if enabled) |
| `error()` | Error | Error details if status is `failed` |

### Schema

| Field | Type | Description |
|-------|------|-------------|
| `documentType()` | String | The identified document type (e.g., "Invoice", "Contract") |
| `documentDescription()` | String | A natural language description of the document |
| `fields()` | KvpFields | Map of field names to field definitions |

### KvpField

Each field in the schema contains:

| Field | Type | Description |
|-------|------|-------------|
| `description()` | String | Natural language description of the field |
| `example()` | String | Example value for the field |

---

## Related Resources

- [Text Extraction Service](../text-extraction-service) - Extract data using generated schemas
- [Text Classification Service](../text-classification-service) - Classify documents using schemas
- [Create Schema API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#create-schema)