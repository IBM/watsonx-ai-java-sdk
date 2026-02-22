---
layout: default
title: Text Extraction Service
parent: Services
nav_order: 8
permalink: /services/text-extraction-service/
---

# Text Extraction Service

The `TextExtractionService` provides functionality to extract text from documents stored in IBM **Cloud Object Storage (COS)** using **IBM watsonx.ai**. It converts business documents into simpler formats (`Markdown`, `JSON`, `HTML`, `plain text`) suitable for AI pipelines, and can optionally extract structured key-value pair data from documents.

## Quick Start

```java
TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .cosUrl(CLOUD_OBJECT_STORAGE_URL)
    .documentReference(INPUT_CONNECTION_ID, INPUT_BUCKET_NAME)
    .resultReference(OUTPUT_CONNECTION_ID, OUTPUT_BUCKET_NAME)
    .build();

TextExtractionParameters parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.MD)
    .mode(Mode.HIGH_QUALITY)
    .languages(Language.ENGLISH)
    .build();

String text = service.uploadExtractAndFetch(new File("path/to/file.pdf"), parameters);
System.out.println(text);
// → # Contract
// → ...
```

---

## Overview

The `TextExtractionService` enables you to:

- Extract text from documents into `Markdown`, `JSON`, `HTML`, or `plain text` formats.
- Upload local files or input streams directly to COS before extraction.
- Run extraction synchronously or asynchronously.
- Extract structured key-value pair data with pre-defined or custom schemas.
- Configure OCR settings for language, rotation correction, and processing mode.
- Control output format, DPI, embedded images, and token output.
- Automatically clean up uploaded input and/or output files after processing.

---

## Service Configuration

### Basic Setup

```java
TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .cosUrl("https://s3.us-south.cloud-object-storage.appdomain.cloud") // or use CosUrl
    .documentReference(INPUT_CONNECTION_ID, INPUT_BUCKET_NAME)
    .resultReference(OUTPUT_CONNECTION_ID, OUTPUT_BUCKET_NAME)
    .build();
```

### Using a Separate COS Authenticator

If your Cloud Object Storage uses different credentials than your watsonx.ai service, provide a dedicated `cosAuthenticator`:

```java
TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .cosAuthenticator(IBMCloudAuthenticator.withKey(COS_API_KEY))
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(WATSONX_URL)
    .cosUrl(CLOUD_OBJECT_STORAGE_URL)
    .documentReference(INPUT_CONNECTION_ID, INPUT_BUCKET_NAME)
    .resultReference(OUTPUT_CONNECTION_ID, OUTPUT_BUCKET_NAME)
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `cosAuthenticator` | Authenticator | No | Separate authenticator for COS operations (defaults to main authenticator) |
| `projectId` | String | Conditional | Project ID where extraction will be performed |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `cosUrl` | String/CosUrl | Yes | Cloud Object Storage base URL |
| `documentReference` | CosReference | Yes | Connection ID and bucket containing input documents |
| `resultReference` | CosReference | Yes | Connection ID and bucket where extracted results are stored |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

---

## Examples

### Synchronous Extraction

`uploadExtractAndFetch` uploads a document, runs extraction, and returns the text content in one call. If no output format is specified, it defaults to `Markdown`.

**From a local file:**

```java
String text = service.uploadExtractAndFetch(new File("path/to/file.pdf"));
```

With parameters:

```java
var parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.MD)
    .mode(Mode.HIGH_QUALITY)
    .languages(Language.ENGLISH)
    .build();

String text = service.uploadExtractAndFetch(new File("path/to/file.pdf"), parameters);
```

**From an InputStream** — useful for documents from web uploads or streaming sources:

```java
TextExtractionParameters parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.MD)
    .mode(Mode.HIGH_QUALITY)
    .build();

String text = service.uploadExtractAndFetch(inputStream, "fileName.pdf", parameters);
```

**From a file already in COS** — skip the upload step entirely:

```java
String text = service.extractAndFetch("path/to/cosFile.pdf");
```

**Automatic file cleanup** — use `removeUploadedFile` and `removeOutputFile` to delete COS files asynchronously after extraction:

```java
var parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.MD)
    .removeUploadedFile(true)   // delete input file after extraction
    .removeOutputFile(true)     // delete output file after reading
    .build();

String text = service.uploadExtractAndFetch(new File("path/to/file.pdf"), parameters);
```

> **Note:** `removeUploadedFile` and `removeOutputFile` are only supported with the synchronous variants (`uploadExtractAndFetch` / `extractAndFetch`). They cannot be used with `uploadAndStartExtraction`.

### Asynchronous Extraction

For long-running operations, start the job and poll until it completes:

```java
var parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.MD)
    .mode(Mode.HIGH_QUALITY)
    .languages(Language.ENGLISH)
    .build();

TextExtractionResponse response = service.uploadAndStartExtraction(new File("path/to/file.pdf"), parameters);

String requestId = response.metadata().id();
String status = response.entity().results().status();

while (!status.equals(Status.COMPLETED.value()) && !status.equals(Status.FAILED.value())) {
    Thread.sleep(2000);
    response = service.fetchExtractionRequest(requestId);
    status = response.entity().results().status();
}

if (status.equals(Status.COMPLETED.value())) {
    String outputPath = response.entity().resultsReference().location().fileName();
    String text = service.readFile(OUTPUT_BUCKET_NAME, outputPath);
    System.out.println(text);
} else
    System.err.println("Failed: " + response.entity().results().error().message());
```

> **Note:** Extraction results are retained for **2 days**. After that, `fetchExtractionRequest` will no longer return results for the given ID.

### Multiple Output Formats

Request multiple output formats in a single extraction using `uploadAndStartExtraction`. Set `outputFileName` to a directory path ending with `/` to group all outputs together:

```java
var parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.PLAIN_TEXT, Type.JSON, Type.HTML)
    .mode(Mode.HIGH_QUALITY)
    .outputFileName("output/")
    .build();

TextExtractionResponse response = service.uploadAndStartExtraction(new File("path/to/file.pdf"), parameters);

// Wait for completion, then read each output file
// Files will be: output/plain.txt, output/assembly.json, output/assembly.html
String plainText = service.readFile(RESULTS_BUCKET, "output/plain.txt");
String json      = service.readFile(RESULTS_BUCKET, "output/assembly.json");
String html      = service.readFile(RESULTS_BUCKET, "output/assembly.html");
```

### OCR from Images

Use `Mode.HIGH_QUALITY` for best OCR results on image files:

```java
var parameters = TextExtractionParameters.builder()
    .mode(Mode.HIGH_QUALITY)
    .requestedOutputs(Type.PLAIN_TEXT)
    .build();

String text = service.uploadExtractAndFetch(new File("path/to/image.png"), parameters);
```

### Managing Requests

Use `deleteRequest` to cancel or remove an extraction job. Pass `hardDelete(true)` to also remove the job metadata:

```java
TextExtractionResponse response = service.uploadAndStartExtraction(new File("invoice.pdf"));

boolean deleted = service.deleteRequest(
    response.metadata().id(),
    TextExtractionDeleteParameters.builder()
        .hardDelete(true)
        .build()
);

System.out.println("Deleted: " + deleted); // → true
```

---

## Output Files

### File Naming Conventions

The output file name is derived from the input file name. When requesting a **single output**, the extension is replaced automatically:

| Output Type | Output File Name |
|-------------|------------------|
| `Type.MD` | `<input_name>.md` |
| `Type.HTML` | `<input_name>.html` |
| `Type.PLAIN_TEXT` | `<input_name>.txt` |
| `Type.JSON` | `<input_name>.json` |
| `Type.PAGE_IMAGES` | `page_images/<page>.png` |

When requesting **multiple outputs**, set `outputFileName` to a directory path ending with `/`. All output files are written into that directory using their default names. For example, with `outputFileName("results/")` and outputs `PLAIN_TEXT`, `JSON`, `HTML`:

```
results/plain.txt
results/assembly.json
results/assembly.html
results/embedded_images_assembly/*.png   (if embedded images are enabled)
results/page_images/*.png                (if PAGE_IMAGES is requested)
```

If `outputFileName` is not set, output files are written to the root of the `resultReference` bucket.

### Output Types

| Value | API String | Description |
|-------|------------|-------------|
| `Type.JSON` | `assembly` | Full structured JSON output including KVP data. Required for key-value pair results |
| `Type.MD` | `md` | Markdown (default) |
| `Type.HTML` | `html` | HTML |
| `Type.PLAIN_TEXT` | `plain_text` | Plain text |
| `Type.PAGE_IMAGES` | `page_images` | Individual page images. Cannot be used with `uploadExtractAndFetch` |

### Embedded Images

Controls how images embedded in the document are handled in the extracted output. Applies to Markdown and JSON formats.

| Value | Image in output | Markdown output | JSON output |
|-------|----------------|-----------------|-------------|
| `DISABLED` | No | None | None |
| `ENABLED_PLACEHOLDER` | Yes | Link to image location | Image in `pictures` structure; `picture.text` empty; generic placeholder token IDs in `picture.children_ids` |
| `ENABLED_TEXT` | Yes | Text extracted directly from the image | Image in `pictures`; OCR text in `picture.text`; token IDs in `picture.children_ids` |
| `ENABLED_VERBALIZATION` | Yes | Link + textual description of the image | Image in `pictures`; natural language description in `picture.verbalization` (only for verbalized images); token IDs in `picture.children_ids` |
| `ENABLED_VERBALIZATION_ALL` | Yes | Link + textual description of the image | Same as `ENABLED_VERBALIZATION`, but **all** embedded images are verbalized, not just graphs, charts, and screenshots |

> Images extracted in any mode are stored as `.png` files in the `embedded_images_assembly/` folder within the output location.

---

## Key-Value Pair Extraction

KVP extraction pulls structured field data out of documents alongside the text. It requires `kvpMode(KvpMode.GENERIC_WITH_SEMANTIC)` and `Type.JSON` as output format — results are only included in the JSON output.

### Basic KVP Extraction

Define a schema, attach it to a `TextExtractionSemanticConfig`, and pass it to the parameters:

```java
KvpFields fields = KvpFields.builder()
    .add("invoice_date",   KvpField.of("The date when the invoice was issued.", "2024-07-10"))
    .add("invoice_number", KvpField.of("The unique invoice identifier.", "INV-2024-001"))
    .add("total_amount",   KvpField.of("The total amount due.", "1250.50"))
    .build();

Schema schema = Schema.builder()
    .documentType("Invoice")
    .documentDescription("A vendor-issued invoice listing purchased items, prices, and payment information.")
    .fields(fields)
    .build();

TextExtractionSemanticConfig semanticConfig = TextExtractionSemanticConfig.builder()
    .enableSchemaKvp(true)
    .schemasMergeStrategy(SchemaMergeStrategy.REPLACE)
    .schemas(schema)
    .build();

TextExtractionParameters parameters = TextExtractionParameters.builder()
    .mode(Mode.HIGH_QUALITY)
    .requestedOutputs(Type.JSON)
    .kvpMode(KvpMode.GENERIC_WITH_SEMANTIC)
    .languages(Language.ENGLISH)
    .semanticConfig(semanticConfig)
    .build();

String json = service.uploadExtractAndFetch(new File("invoice.pdf"), parameters);
```

### Extraction Methods

Two methods can be enabled independently or together when using `GENERIC_WITH_SEMANTIC`:

| Method | Parameter | Behaviour |
|--------|-----------|-----------|
| Schema-based | `enableSchemaKvp(true)` | Classifies each page into a schema type and extracts only the defined fields. Higher accuracy for known document types |
| Generic | `enableGenericKvp(true)` | Broad sweep: extracts any labelled data regardless of schema. Useful for unknown document formats |

Both are active by default. If you only want schema-based results, set `enableGenericKvp(false)` to avoid duplicate extractions.

### Schema Merge Strategy

Controls how custom schemas interact with the built-in pre-defined ones:

| Strategy | Behaviour | When to use |
|----------|-----------|-------------|
| `SchemaMergeStrategy.REPLACE` | Only your custom schemas are used; all pre-defined schemas are ignored | You have a known document format with unique fields, or your custom schema conflicts with a pre-defined one |
| `SchemaMergeStrategy.MERGE` | Your custom schemas are combined with the pre-defined ones | You want to supplement pre-defined document types with additional custom schemas |

### Using a Custom Foundation Model

Override the default model (`mistral-small-3-1-24b-instruct-2503`) globally with `defaultModelName`, or per pipeline task with `taskModelNameOverride`:

```java
TextExtractionSemanticConfig semanticConfig = TextExtractionSemanticConfig.builder()
    .defaultModelName("mistral-large-2512")
    .taskModelNameOverride(Map.of(
        "extraction", "meta-llama/llama-4-maverick-17b-128e-instruct-fp8",
        "create_schema", "mistral-large-2512"
    ))
    .enableSchemaKvp(true)
    .schemasMergeStrategy(SchemaMergeStrategy.REPLACE)
    .schemas(schema)
    .build();

TextExtractionParameters parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.JSON)
    .kvpMode(KvpMode.GENERIC_WITH_SEMANTIC)
    .semanticConfig(semanticConfig)
    .build();

String json = service.uploadExtractAndFetch(new File("invoice.pdf"), parameters);
```

Supported keys for `taskModelNameOverride`: `classification_exact`, `extraction`, `create_schema`, `create_schema_page_merger`, `improve_schema_description`, `cluster_schemas`, `merge_schemas`.

---

## Extraction Parameters

`TextExtractionParameters` controls how extraction is performed per request.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `requestedOutputs` | Type | Output format(s) to generate. Defaults to `MD` if not set |
| `mode` | Mode | Processing quality: `STANDARD` (faster) or `HIGH_QUALITY` (preserves all data structures, slower) |
| `ocrMode` | OcrMode | OCR mode: `DISABLED`, `ENABLED`, `FORCED`, or `AUTO` (service decides) |
| `autoRotationCorrection` | Boolean | Automatically correct document rotation before OCR |
| `languages` | Language | Expected languages in the document (ISO 639) |
| `kvpMode` | KvpMode | Key-value pair extraction mode. Disabled by default. Results are only included in `Type.JSON` output |
| `semanticConfig` | TextExtractionSemanticConfig | Semantic configuration for schema-based KVP extraction |
| `createEmbeddedImages` | EmbeddedImageMode | How images embedded in the document are handled in output |
| `outputDpi` | Integer | DPI for extracted page images |
| `outputTokens` | Boolean | Include token bounding boxes in the output |
| `outputFileName` | String | Name or directory prefix for the output file in COS |
| `removeUploadedFile` | Boolean | Delete the input file from COS after extraction (synchronous only) |
| `removeOutputFile` | Boolean | Delete the output file from COS after reading (synchronous only) |
| `documentReference` | CosReference | Override the default input COS location for this request |
| `resultReference` | CosReference | Override the default output COS location for this request |
| `timeout` | Duration | Override the service-level timeout for this request |
| `addCustomProperty` | String, Object | Add arbitrary key-value metadata to the request |
| `projectId` | String | Override the default project ID |
| `spaceId` | String | Override the default space ID |
| `transactionId` | String | Request tracking ID |

### Processing Modes

| Value | Description |
|-------|-------------|
| `Mode.STANDARD` | Faster processing with standard accuracy |
| `Mode.HIGH_QUALITY` | Slower processing with higher accuracy |

### KVP Modes

| Value | Description |
|-------|-------------|
| `KvpMode.DISABLED` | Key-value pair extraction is disabled (default) |
| `KvpMode.GENERIC_WITH_SEMANTIC` | Extract generic and schema-based KVP data. Use with `semanticConfig` to configure the extraction pipeline |

### OCR Modes

| Value | Sent to API | Description |
|-------|-------------|-------------|
| `OcrMode.AUTO` | *(not sent)* | Service automatically selects the best OCR option |
| `OcrMode.DISABLED` | `"disabled"` | OCR is disabled; document must contain native text |
| `OcrMode.ENABLED` | `"enabled"` | OCR is applied when the service determines it is needed |
| `OcrMode.FORCED` | `"forced"` | OCR is always applied regardless of document content |

---

## TextExtractionResponse

Returned by `startExtraction`, `uploadAndStartExtraction`, and `fetchExtractionRequest`.

| Field | Type | Description |
|-------|------|-------------|
| `metadata().id()` | String | Unique identifier for the extraction request |
| `metadata().createdAt()` | String | Timestamp when the request was created |
| `metadata().modifiedAt()` | String | Timestamp of the last update |
| `metadata().projectId()` | String | Project ID associated with the request |
| `entity().results()` | ExtractionResult | The current extraction result |
| `entity().documentReference()` | DataReference | Reference to the input document |
| `entity().resultsReference()` | DataReference | Reference to the output file(s) |
| `entity().parameters()` | Parameters | Parameters used for this extraction |
| `entity().custom()` | Map\<String, Object\> | User-defined custom properties |

### ExtractionResult

| Field | Type | Description |
|-------|------|-------------|
| `status()` | String | Current status: `submitted`, `queued`, `running`, `completed`, or `failed` |
| `runningAt()` | String | Timestamp when processing started |
| `completedAt()` | String | Timestamp when processing completed or failed |
| `numberPagesProcessed()` | Integer | Number of pages processed so far |
| `totalPages()` | Integer | Total number of pages to process |
| `location()` | List\<String\> | Paths of the output files produced in COS |
| `error()` | Error | Error details if status is `failed` |

---

## Related Resources

- [Text Extraction Documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html?context=wx&audience=wdp)
- [Text Extraction Parameters Documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction-params.html?context=wx&audience=wdp)
- [Key-Value Pair Extraction Modes](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction-kvp.html?context=wx&audience=wdp)
- [Text Extraction API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#text-extraction)
- [Text Extraction Sample](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/text-extraction)
