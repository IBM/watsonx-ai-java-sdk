---
layout: default
title: Text Classification Service
parent: Services
nav_order: 7
permalink: /services/text-classification-service/
---

# Text Classification Service

The `TextClassificationService` provides functionality to classify documents stored in **IBM Cloud Object Storage (COS)** using **IBM watsonx.ai**. It identifies whether a document matches pre-defined or custom schema definitions, enabling automated document routing and pre-processing before resource-intensive key-value pair extraction.

## Quick Start

```java
TextClassificationService service = TextClassificationService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .cosUrl(CLOUD_OBJECT_STORAGE_URL)
    .documentReference(CONNECTION_ID, BUCKET_NAME)
    .build();

ClassificationResult result = service.uploadClassifyAndFetch(new File("path/to/invoice.pdf"));
System.out.println("Document Type: " + result.documentType());
System.out.println("Classified:    " + result.documentClassified());
// → Document Type: Invoice
// → Classified: true
```

---

## Overview

The `TextClassificationService` enables you to:

- Classify documents against pre-defined and custom schemas.
- Upload local files or input streams directly to COS before classification.
- Run classification synchronously (upload + classify + fetch in one call) or asynchronously.
- Define custom document schemas with field definitions and semantic configuration.
- Configure OCR settings for language, rotation correction, and processing mode.
- Manage the full lifecycle of classification requests (start, fetch, delete).
- Automatically clean up uploaded files after processing.

---

## Service Configuration

### Basic Setup

```java
TextClassificationService service = TextClassificationService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .cosUrl("https://s3.us-south.cloud-object-storage.appdomain.cloud") // or use CosUrl
    .documentReference(CONNECTION_ID, BUCKET_NAME)
    .build();
```

### Using a Separate COS Authenticator

If your **Cloud Object Storage** uses different credentials than your **watsonx.ai** service, provide a dedicated `cosAuthenticator`:

```java
TextClassificationService service = TextClassificationService.builder()
    .apiKey(WATSONX_API_KEY)
    .cosAuthenticator(IBMCloudAuthenticator.withKey(COS_API_KEY))
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .cosUrl("https://s3.us-south.cloud-object-storage.appdomain.cloud") // or use CosUrl
    .documentReference(CONNECTION_ID, BUCKET_NAME)
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `cosAuthenticator` | Authenticator | No | Separate authenticator for COS operations (defaults to main authenticator) |
| `projectId` | String | Conditional | Project ID where classification will be performed |
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

### Synchronous Classification

The simplest way to classify a document is to use the `uploadClassifyAndFetch` method. This uploads a file and runs classification in one call.

**From a local file:**

```java
ClassificationResult result = service.uploadClassifyAndFetch(new File("invoice.pdf"));
System.out.println("Status:          " + result.status());
System.out.println("Document Type:   " + result.documentType());
System.out.println("Classified:      " + result.documentClassified());
System.out.println("Pages Processed: " + result.numberPagesProcessed());
// → Status:          completed
// → Document Type:   Invoice
// → Classified:      true
// → Pages Processed: 1
```

**From an InputStream** — useful for documents from web uploads or streaming sources:

```java
InputStream inputStream = new FileInputStream("invoice.pdf");
ClassificationResult result = service.uploadClassifyAndFetch(inputStream, "invoice.pdf");
System.out.println("Document Type: " + result.documentType());
// → Document Type: Invoice
```

**From a file already in COS** — skip the upload step entirely:

```java
ClassificationResult result = service.classifyAndFetch("invoice.pdf");
System.out.println("Document Type: " + result.documentType());
// → Document Type: Invoice
```

**Automatic file cleanup** — set `removeUploadedFile(true)` to delete the uploaded file from COS asynchronously after classification completes:

```java
var parameters = TextClassificationParameters.builder()
    .languages(Language.ENGLISH)
    .removeUploadedFile(true)
    .build();

service.uploadClassifyAndFetch(new File("path/to/invoice.pdf"), parameters);
```

> **Note:** `removeUploadedFile` is only supported with the synchronous variants. For other cases, call `service.deleteFile(BUCKET_NAME, fileName)` manually after processing.

### Asynchronous Classification

For long-running operations, start the job and poll until it completes:

```java
TextClassificationResponse response = service.uploadAndStartClassification(new File("invoice.pdf"));

String requestId = response.metadata().id();
String status = response.entity().results().status();

while (!status.equals(Status.COMPLETED.value()) && !status.equals(Status.FAILED.value())) {
    Thread.sleep(2000);
    response = service.fetchClassificationRequest(requestId);
    status = response.entity().results().status();
}

if (status.equals(Status.COMPLETED.value()))
    System.out.println("Document Type: " + response.entity().results().documentType());
else
    System.err.println("Failed: " + response.entity().results().error().message());

service.deleteFile(BUCKET_NAME, "invoice.pdf");
// → Document Type: Invoice
```

### Managing Requests

Use `deleteRequest` to cancel or remove a classification job. Pass `hardDelete(true)` to also remove the job metadata:

```java
TextClassificationResponse response = service.uploadAndStartClassification(new File("invoice.pdf"));

boolean deleted = service.deleteRequest(
    response.metadata().id(),
    TextClassificationDeleteParameters.builder()
        .hardDelete(true)
        .build()
);

System.out.println("Deleted: " + deleted);
// → Deleted: true
```

> Deleting a non-existent ID returns `false`.

---

## Custom Schemas

By default, the service classifies documents against a set of pre-defined schemas. When your documents have domain-specific structures, you can define custom schemas and control how they interact with the built-in ones.

### Defining a Schema

A `Schema` describes a document type. Use `fields` for variable-layout documents (fields can appear anywhere on the page) or `pages` for fixed-layout documents with consistent field positions. The two are mutually exclusive.

```java
KvpFields fields = KvpFields.builder()
    .add("invoice_date",   KvpField.of("The date when the invoice was issued.", "2024-07-10"))
    .add("invoice_number", KvpField.of("The unique number identifying the invoice.", "INV-2024-001"))
    .add("total_amount",   KvpField.of("The total amount to be paid.", "1250.50"))
    .build();

Schema customSchema = Schema.builder()
    .documentType("My-Invoice")
    .documentDescription("A vendor-issued invoice listing purchased items, prices, and payment information")
    .fields(fields)
    .additionalPromptInstructions("The document contains a table with all the data")
    .build();
```

Each `KvpField` accepts a description and an example value. You can also pass `availableOptions` to restrict a field to a set of allowed values.

### Schema Merge Strategy

`schemasMergeStrategy` controls how custom schemas interact with the built-in pre-defined ones:

| Strategy | Description | When to use |
|----------|-------------|-------------|
| `SchemaMergeStrategy.REPLACE` | Ignores all pre-defined schemas; classifies only against your custom schemas | When your documents have unique fields or you want to prevent accidental matching with a similar pre-defined schema |
| `SchemaMergeStrategy.MERGE` | Combines your custom schemas with the existing pre-defined ones | When you want to extend the catalog with new document types while still benefiting from pre-defined schemas |

```java
TextClassificationSemanticConfig semanticConfig = TextClassificationSemanticConfig.builder()
    .schemasMergeStrategy(SchemaMergeStrategy.REPLACE)
    .schemas(customSchema)
    .build();

TextClassificationParameters parameters = TextClassificationParameters.builder()
    .languages(Language.ENGLISH)
    .semanticConfig(semanticConfig)
    .build();

// Matching document
ClassificationResult result = service.uploadClassifyAndFetch(new File("invoice.pdf"), parameters);
System.out.println(result.documentClassified()); // → true
System.out.println(result.documentType());       // → My-Invoice

// Non-matching document
result = service.uploadClassifyAndFetch(new File("noinvoice.pdf"), parameters);
System.out.println(result.documentClassified()); // → false
System.out.println(result.documentType());       // → (blank)
```

### Extraction Methods

Two extraction methods can be enabled independently or together:

**Schema-based extraction** (`enableSchemaKvp: true`) classifies each page into a known document type and extracts only the fields declared in the matching schema. Use this when you have domain-specific knowledge of the document structure — it increases accuracy for known document types.

**Generic extraction** (`enableGenericKvp: true`) performs a broad sweep and extracts any content that can be represented as key-value pairs, regardless of document type. Use this when you have no prior knowledge of the document structure.

Both are active by default. If you only want schema-based results, set `enableGenericKvp(false)` to avoid duplicate extractions.

### Choosing Between `fields` and `pages`

| | `fields` | `pages` |
|--|---------|---------|
| **Use for** | Variable-layout documents where fields can appear anywhere | Fixed-layout documents with consistent field positions |
| **How it works** | Model scans the entire document for matching fields | Model targets only the specified bounding box regions |
| **Defined with** | `KvpFields` | `KvpPage` + `KvpSlice` with normalized bbox (0.0–100.0) |

### Using a Custom Foundation Model

By default the service uses `mistral-small-3-1-24b-instruct-2503`. Override it globally with `defaultModelName`, or per pipeline task with `taskModelNameOverride`:

```java
TextClassificationSemanticConfig semanticConfig = TextClassificationSemanticConfig.builder()
    .schemasMergeStrategy(SchemaMergeStrategy.REPLACE)
    .schemas(
        Schema.builder()
            .documentType("Invoice")
            .documentDescription("A vendor-issued invoice listing purchased items and payment information.")
            .fields(
                KvpFields.builder()
                    .add("invoice_number", KvpField.of("The unique invoice identifier.", "INV-2024-001"))
                    .add("total_amount", KvpField.of("The total amount due.", "1250.50"))
                    .build()
            )
            .build()
    )
    .defaultModelName("mistral-large-2512")
    .taskModelNameOverride(Map.of(
        "classification_exact", "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
        "extraction", "mistral-large-2512"
    ))
    .build();

ClassificationResult result = service.uploadClassifyAndFetch(
    new File("invoice.pdf"),
    TextClassificationParameters.builder()
        .languages(Language.ENGLISH)
        .semanticConfig(semanticConfig)
        .build()
);

System.out.println("Document Type: " + result.documentType());
// → Document Type: Invoice
```

Supported keys for `taskModelNameOverride`: `classification_exact`, `extraction`, `create_schema`, `create_schema_page_merger`, `improve_schema_description`, `cluster_schemas`, `merge_schemas`.

---

## Classification Parameters

`TextClassificationParameters` controls how classification is performed per request.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `classificationMode` | ClassificationMode | `EXACT` returns the matched schema name; `BINARY` returns only whether a match was found |
| `ocrMode` | OcrMode | OCR processing mode: `DISABLED`, `ENABLED`, or `FORCED`. Leaving unset lets the service choose automatically |
| `autoRotationCorrection` | Boolean | Automatically correct document rotation before OCR |
| `languages` | Language... | Expected languages in the document (ISO 639) |
| `semanticConfig` | TextClassificationSemanticConfig | Custom schema and semantic classification settings |
| `removeUploadedFile` | Boolean | Delete the uploaded file from COS after classification (synchronous only) |
| `documentReference` | CosReference | Override the default COS connection and bucket for this request |
| `timeout` | Duration | Override the service-level timeout for this request |
| `addCustomProperty` | String, Object | Add arbitrary key-value metadata to the request |
| `projectId` | String | Override the default project ID |
| `spaceId` | String | Override the default space ID |
| `transactionId` | String | Request tracking ID |

### Classification Modes

| Mode | Description |
|------|-------------|
| `ClassificationMode.EXACT` | Returns the exact schema name the document is classified to |
| `ClassificationMode.BINARY` | Returns only whether the document matches any known schema |

### OCR Modes

| Value | Sent to API | Description |
|-------|-------------|-------------|
| `OcrMode.AUTO` | *(not sent)* | Service automatically selects the best OCR option |
| `OcrMode.DISABLED` | `"disabled"` | OCR is disabled; document must contain native text |
| `OcrMode.ENABLED` | `"enabled"` | OCR is applied when the service determines it is needed |
| `OcrMode.FORCED` | `"forced"` | OCR is always applied regardless of document content |

---

## TextClassificationResponse

Returned by `startClassification`, `uploadAndStartClassification`, and `fetchClassificationRequest`.

| Field | Type | Description |
|-------|------|-------------|
| `metadata().id()` | String | Unique identifier for the classification request |
| `metadata().createdAt()` | String | Timestamp when the request was created |
| `metadata().modifiedAt()` | String | Timestamp of the last update |
| `metadata().projectId()` | String | Project ID associated with the request |
| `entity().results()` | ClassificationResult | The current classification result |
| `entity().documentReference()` | DataReference | Reference to the input document in COS |
| `entity().parameters()` | Parameters | Parameters used for this classification |
| `entity().custom()` | Map\<String, Object\> | User-defined custom properties |

### ClassificationResult

| Field | Type | Description |
|-------|------|-------------|
| `status()` | String | Current status: `queued`, `running`, `completed`, or `failed` |
| `runningAt()` | String | Timestamp when processing started |
| `completedAt()` | String | Timestamp when processing completed or failed |
| `numberPagesProcessed()` | Integer | Number of pages processed |
| `documentClassified()` | Boolean | Whether the document matched a schema |
| `documentType()` | String | The identified schema/document type (empty if not classified) |
| `error()` | Error | Error details if status is `failed` |

---

## Language Reference

The `Language` enum provides ISO 639 language codes. Pass one or more to `languages()`:

```java
TextClassificationParameters.builder()
    .languages(Language.ENGLISH, Language.FRENCH, Language.GERMAN)
    .build();
```

---

## Related Resources

- [Text Classification Documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-classification.html?context=wx&audience=wdp)
- [Text Classification Parameters Documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-classification-params.html?context=wx&audience=wdp)
- [Key-Value Pair Extraction Modes](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction-kvp.html?context=wx&audience=wdp)
- [Text Classification API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#text-classification)
- [Text Classification Sample](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/text-classification)
