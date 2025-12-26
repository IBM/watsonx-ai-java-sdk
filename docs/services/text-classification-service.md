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

If your **Cloud Object Storage** uses different credentials than your **watsonx.ai** service, you can provide a dedicated `cosAuthenticator`:

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

// Clean up the uploaded file manually if removeUploadedFile is not set
service.deleteFile(BUCKET_NAME, "invoice.pdf");
```

### Asynchronous Classification

For long-running operations, you can start the classification process and then poll until it is complete.

```java
TextClassificationResponse response = service.uploadAndStartClassification(new File("path/to/invoice.pdf"));

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

### Automatic File Cleanup

Set `removeUploadedFile(true)` to have the service delete the uploaded file asynchronously after classification completes:

```java

var parameters = TextClassificationParameters.builder()
    .languages(Language.ENGLISH)
    .removeUploadedFile(true) // This option deletes the file after classification
    .build();

service.uploadClassifyAndFetch(new File("path/to/invoice.pdf"), parameters);
```

> **Note:** `removeUploadedFile` is only supported with the synchronous variants.

### Classifying from an InputStream

Useful for processing documents from web uploads or streaming sources:

```java
InputStream inputStream = new FileInputStream("invoice.pdf");
ClassificationResult result = service.uploadClassifyAndFetch(inputStream,"invoice.pdf");
System.out.println("Document Type: " + result.documentType());
// → Document Type: Invoice
```

### Classifying a document already uploaded

If the document is already stored in your COS bucket, skip the upload step:

```java
ClassificationResult result = service.classifyAndFetch("invoice.pdf");
System.out.println("Document Type: " + result.documentType());
// → Document Type: Invoice
```

### Custom Schema Classification

Define custom document schemas to classify domain-specific documents. Use `SchemaMergeStrategy.REPLACE` to replace built-in schemas entirely:

```java
KvpFields fields = KvpFields.builder()
    .add("invoice_date",   KvpField.of("The date when the invoice was issued.", "2024-07-10"))
    .add("invoice_number", KvpField.of("The unique number identifying the invoice.", "INV-2024-001"))
    .add("total_amount",   KvpField.of("The total amount to be paid.", "1250.50"))
    .build();

Schema customSchema = Schema.builder()
    .documentDescription("A vendor-issued invoice listing purchased items, prices, and payment information")
    .documentType("My-Invoice")
    .fields(fields)
    .additionalPromptInstructions("The document contains a table with all the data")
    .build();

TextClassificationSemanticConfig semanticConfig = TextClassificationSemanticConfig.builder()
    .schemasMergeStrategy(SchemaMergeStrategy.REPLACE)
    .schemas(customSchema)
    .build();

TextClassificationParameters parameters = TextClassificationParameters.builder()
    .languages(Language.ENGLISH)
    .semanticConfig(semanticConfig)
    .build();

// Matching document
ClassificationResult result = service.uploadClassifyAndFetch(new File("path/to/invoice.pdf"), parameters);
System.out.println(result.documentClassified()); // → true
System.out.println(result.documentType());       // → My-Invoice

// Non-matching document
result = service.uploadClassifyAndFetch(new File("path/to/noinvoice.pdf"), parameters);
System.out.println(result.documentClassified()); // → false
System.out.println(result.documentType());       // → (blank)
```

### Using a Custom Foundation Model

By default the service uses `mistral-small-3-1-24b-instruct-2503`. You can override this with any compatible vision model via `defaultModelName`, or target individual pipeline tasks with `taskModelNameOverride`:

```java
Schema customSchema = Schema.builder()
    .documentType("Invoice")
    .documentDescription("A vendor-issued invoice listing purchased items and payment information.")
    .fields(
        KvpFields.builder()
            .add("invoice_number", KvpField.of("The unique invoice identifier.", "INV-2024-001"))
            .add("total_amount", KvpField.of("The total amount due.", "1250.50"))
            .build()
        )
    .build();

TextClassificationSemanticConfig semanticConfig = TextClassificationSemanticConfig.builder()
    .schemasMergeStrategy(SchemaMergeStrategy.REPLACE)
    .schemas(customSchema)
    .defaultModelName("mistral-large-2512")
    .taskModelNameOverride(
        Map.of(
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
System.out.println("Classified:    " + result.documentClassified());
// → Classified: true
```

### Deleting a Classification Request

Use the `deleteRequest` method to delete a running request. To also remove the job metadata, use the `hardDelete(true)` method. 

```java
TextClassificationResponse response = service.uploadAndStartClassification(new File("invoice.pdf"));

boolean deleted = service.deleteRequest(
    response.metadata().id(),
    TextClassificationDeleteParameters.builder()
        .hardDelete(true)
        .build()
);

// Deleting a non-existent ID returns false.
System.out.println("Deleted: " + deleted); // → Deleted: true
```

---

## Classification Parameters

The `TextClassificationParameters` class controls how classification is performed.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `classificationMode` | ClassificationMode | `EXACT` returns the matched schema name; `BINARY` returns only whether a match was found |
| `ocrMode` | OcrMode | OCR processing mode: `DISABLED`, `ENABLED`, or `FORCED`. Leaving it unset allows the service to select the best option automatically. |
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

---

## Semantic Configuration

`TextClassificationSemanticConfig` lets you customize the classification behavior, control which extraction methods are used, and define custom document schemas.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `schemas` | Schema... | One or more custom document schemas |
| `schemasMergeStrategy` | SchemaMergeStrategy | How custom schemas interact with pre-defined ones (see below) |
| `enableSchemaKvp` | Boolean | Enable schema-based extraction, which targets specific fields using pre-defined or custom schemas |
| `enableGenericKvp` | Boolean | Enable generic extraction, which broadly identifies any key-value pairs without domain-specific knowledge |
| `enableTextHints` | Boolean | Enable text hint signals to improve extraction accuracy |
| `groundingMode` | String | Precision of bounding box location data included with results: `"fast"` (lower precision, faster) or `"precise"` (higher precision, higher compute cost) |
| `forceSchemaName` | String | Skip document classification and directly apply the named schema. Must exactly match a `documentType` from a pre-defined or custom schema. |
| `defaultModelName` | String | Default foundation model to use for classification |
| `taskModelNameOverride` | Map\<String, Object\> | Override the model for specific pipeline tasks. Supported keys: `classification_exact`, `extraction`, `create_schema`, `create_schema_page_merger`, `improve_schema_description`, `cluster_schemas`, `merge_schemas` |

### Controlling Schema Interaction with SchemaMergeStrategy

The `schemasMergeStrategy` field controls how custom schemas interact with the built-in pre-defined schemas:

| Strategy | Description | When to use |
|----------|-------------|-------------|
| `SchemaMergeStrategy.REPLACE` | Ignores all pre-defined schemas and only uses the custom schemas you provide. The document is classified against your custom schema descriptions. | When your document has unique fields best described by a custom schema, or when you want to prevent accidental matching with a similar pre-defined schema (e.g., a custom invoice with business-specific fields). |
| `SchemaMergeStrategy.MERGE` | Combines your custom schemas with the existing pre-defined schemas. | When you want to extend the catalog with new document types while still benefiting from pre-defined schemas. |

### Choosing an Extraction Method

By default, both `enableSchemaKvp` and `enableGenericKvp` are active. You can enable one or both depending on your use case:

**Schema-based extraction** (`enableSchemaKvp: true`) targets specific fields defined in your schemas. It classifies each page into a known document type and extracts only the fields declared in the matching schema. This increases accuracy for known document types.

**Generic extraction** (`enableGenericKvp: true`) performs a broad sweep of the document and extracts any content that can be represented as key-value pairs, regardless of document type. Useful when you have no domain-specific knowledge of the document.

> **Note:** When both methods are active, a value may be extracted twice. If you only want schema-based results, set `enableGenericKvp(false)`.

## Schema Reference

The `Schema` class defines the structure of a custom document type for classification.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `documentType` | String | Short title for the document type (e.g., `"Invoice"`, `"Passport"`) |
| `documentDescription` | String | One or two sentence description to help the model understand the document |
| `fields` | KvpFields | Field-based schema for variable-layout documents. **Mutually exclusive with `pages`** |
| `pages` | KvpPage | Page-based schema for fixed-layout documents. **Mutually exclusive with `fields`** |
| `additionalPromptInstructions` | String | Optional extra instructions appended to the model prompt to guide extraction |

### KvpFields and KvpField

`KvpFields` is a collection of named field definitions. Each `KvpField` describes what to extract:

| Parameter | Type | Description |
|-----------|------|-------------|
| `description` | String | Description of the field to help the model identify it |
| `example` | String | Example value illustrating expected format |
| `availableOptions` | List\<String\> | Optional list of allowed values for enumerated fields |

### Choosing Between fields and pages

| | `fields` | `pages` |
|--|---------|---------|
| **Use for** | Variable-layout documents where fields can appear anywhere | Fixed-layout documents with consistent field positions |
| **How it works** | Model scans the entire document for matching fields | Model targets only the specified bounding box regions |
| **Defined with** | `KvpFields` | `KvpPage` + `KvpSlice` with normalized bbox (0.0–100.0) |

---

## OcrMode Reference

| Value | Sent to API | Description |
|-------|-------------|-------------|
| `OcrMode.AUTO` | *(empty, not sent)* | Service automatically selects the best OCR option |
| `OcrMode.DISABLED` | `"disabled"` | OCR is disabled; document must contain native text |
| `OcrMode.ENABLED` | `"enabled"` | OCR is applied when the service determines it is needed |
| `OcrMode.FORCED` | `"forced"` | OCR is always applied regardless of document content |

---

## TextClassificationResponse

The `TextClassificationResponse` is returned by `startClassification`, `uploadAndStartClassification`, and `fetchClassificationRequest`.

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
