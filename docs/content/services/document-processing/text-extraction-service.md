---
id: text-extraction-service
title: Text Extraction Service
---

# Text Extraction Service

The `TextExtractionService` provides functionality to extract text from documents using **IBM watsonx.ai**. It converts business documents into simpler formats (`Markdown`, `JSON`, `HTML`, `plain text`) suitable for AI pipelines, and can optionally extract structured key-value pair data from documents.

Documents can be provided via **IBM Cloud Object Storage** (using a `CosReference`) or from the **default container** managed by your watsonx.ai deployment (using a `ContainerReference`).

## Quick Start

```java
TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .cosUrl(CLOUD_OBJECT_STORAGE_URL)
    .documentReference(CosReference.of(INPUT_CONNECTION_ID, INPUT_BUCKET_NAME))
    .resultReference(CosReference.of(OUTPUT_CONNECTION_ID, OUTPUT_BUCKET_NAME))
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
    .documentReference(CosReference.of(INPUT_CONNECTION_ID, INPUT_BUCKET_NAME))
    .resultReference(CosReference.of(OUTPUT_CONNECTION_ID, OUTPUT_BUCKET_NAME))
    .build();
```

### Using Container References

When documents are already stored in the default container created by your watsonx.ai deployment, use `ContainerReference` instead of `CosReference`. No COS URL or credentials are required.

`ContainerReference.container()` is a path-free marker. The file path is supplied at call time as the first argument of `startExtraction` or `extractAndFetch`. This is the recommended approach when the service is configured once and used across multiple files.

```java
TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .documentReference(ContainerReference.container())
    .resultReference(ContainerReference.container())
    .build();

// The path passed here becomes the container path for both document and output
TextExtractionResponse response = service.startExtraction("invoices/q1-invoice.pdf");

// Fetch result and retrieve the extracted text
String text = service.extractAndFetch("invoices/q1-invoice.pdf");
```

When using `ContainerReference`, upload methods (`uploadFile`, `uploadAndStartExtraction`, `uploadExtractAndFetch`) upload the file directly to the **project's default COS bucket** (resolved automatically from the project metadata). The platform then reads the file from that bucket during extraction.

Per-call overrides work across types. A service built with `CosReference` defaults can use a `ContainerReference` for an individual call by setting it in `TextExtractionParameters`, and vice versa.

#### COS access when using ContainerReference

When a `ContainerReference` is configured as the default, direct COS operations (upload, read, delete, and per-call `CosReference` overrides via parameters) require the service to know the project's default COS bucket and endpoint URL. These are resolved automatically from the project metadata:

- **Standard cloud regions** - if `baseUrl(CloudRegion)` was used, the service calls the Projects API automatically on first use. No extra configuration is required.
- **Custom or on-premise deployments** - if a plain URL string was passed to `baseUrl(...)`, the service cannot derive the Projects API endpoint automatically. Pass an explicit `ProjectService` instance:

```java
ProjectService projectService = ProjectService.builder()
    .apiKey(WATSONX_API_KEY)
    .baseUrl("https://api.your-deployment.example.com")
    .build();

TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://ml.your-deployment.example.com")
    .documentReference(ContainerReference.container())
    .resultReference(ContainerReference.container())
    .projectService(projectService)
    .build();
```

The resolved endpoint URL and bucket are cached after the first lookup. When only `spaceId` is configured (no `projectId`), lazy COS resolution is not supported; set `cosUrl` explicitly in that case.

### Using a Separate COS Authenticator

If your Cloud Object Storage uses different credentials than your watsonx.ai service, provide a dedicated `cosAuthenticator`:

```java
TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .cosAuthenticator(IBMCloudAuthenticator.withKey(COS_API_KEY))
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(WATSONX_URL)
    .cosUrl(CLOUD_OBJECT_STORAGE_URL)
    .documentReference(CosReference.of(INPUT_CONNECTION_ID, INPUT_BUCKET_NAME))
    .resultReference(CosReference.of(OUTPUT_CONNECTION_ID, OUTPUT_BUCKET_NAME))
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
| `cosUrl` | String/CosUrl | Conditional | Cloud Object Storage base URL. Required when `documentReference` is a `CosReference`. Not required for `ContainerReference`, unless only `spaceId` is set (no `projectId`) |
| `projectService` | ProjectService | No | Explicit `ProjectService` for resolving the default COS bucket when using `ContainerReference` with a non-standard base URL. Auto-resolved from `CloudRegion` otherwise |
| `documentReference` | DocumentReference | Yes | Input document location - a `CosReference` (connection ID + bucket) or `ContainerReference` (container path) |
| `resultReference` | DocumentReference | Yes | Output location - a `CosReference` (connection ID + bucket) or `ContainerReference` (container path) |
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

**From an InputStream** (useful for documents from web uploads or streaming sources):

```java
TextExtractionParameters parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.MD)
    .mode(Mode.HIGH_QUALITY)
    .build();

String text = service.uploadExtractAndFetch(inputStream, "fileName.pdf", parameters);
```

**From a file already in COS** - skip the upload step entirely:

```java
String text = service.extractAndFetch("path/to/cosFile.pdf");
```

**From a file already in the container** - use `extractAndFetch` with a container-configured service. The extracted content is retrieved automatically from the project's default COS bucket:

```java
TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .documentReference(ContainerReference.container())
    .resultReference(ContainerReference.container())
    .build();

// Runs extraction and retrieves the result from the project's default COS bucket
String text = service.extractAndFetch("invoices/q1-invoice.pdf");
```

**Automatic file cleanup:** use `removeUploadedFile` and `removeOutputFile` to delete the input and output files asynchronously after extraction:

```java
var parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.MD)
    .removeUploadedFile(true)   // delete input file after extraction
    .removeOutputFile(true)     // delete output file after reading
    .build();

String text = service.uploadExtractAndFetch(new File("path/to/file.pdf"), parameters);
```

> **Note:** `removeUploadedFile` and `removeOutputFile` are only supported with the synchronous variants (`uploadExtractAndFetch` / `extractAndFetch`). Passing either with `uploadAndStartExtraction` throws `IllegalArgumentException`.
>
> **Caution:** when calling `extractAndFetch(String, ...)` with `removeUploadedFile(true)` on a pre-existing document (no upload was performed), the service will delete the document at the path you passed. Use this option with care.

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

Multiple output formats are supported with both `CosReference` and `ContainerReference`. Set `outputFileName` to a directory path ending with `/` to group all outputs together.

**COS-backed service:**

```java
var parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.PLAIN_TEXT, Type.JSON, Type.HTML)
    .mode(Mode.HIGH_QUALITY)
    .outputFileName("output/")
    .build();

TextExtractionResponse response = service.uploadAndStartExtraction(new File("path/to/file.pdf"), parameters);

// Wait for completion, then read each output file
// Files will be: output/plain.txt, output/assembly.json, output/assembly.html
String plainText = service.readFile(OUTPUT_BUCKET_NAME, "output/plain.txt");
String json      = service.readFile(OUTPUT_BUCKET_NAME, "output/assembly.json");
String html      = service.readFile(OUTPUT_BUCKET_NAME, "output/assembly.html");
```

**Container-backed service** (reads results from the project's default COS bucket):

```java
TextExtractionService service = TextExtractionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .documentReference(ContainerReference.container())
    .resultReference(ContainerReference.container())
    .build();

var parameters = TextExtractionParameters.builder()
    .requestedOutputs(Type.PLAIN_TEXT, Type.JSON, Type.HTML)
    .outputFileName("output/")
    .build();

service.uploadFile(new File("path/to/file.pdf"));
TextExtractionResponse response = service.startExtraction("file.pdf", parameters);

// Wait for completion, then read each output file
// bucketName is ignored when resultReference is a ContainerReference - the project bucket is used
String plainText = service.readFile(null, "output/plain.txt");
String json      = service.readFile(null, "output/assembly.json");
String html      = service.readFile(null, "output/assembly.html");
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

```text
results/plain.txt
results/assembly.json
results/assembly.html
results/embedded_images_assembly/*.png   (if embedded images are enabled)
results/page_images/*.png                (if PAGE_IMAGES is requested)
```

If `outputFileName` is not set for a single-output extraction, the output file is written to the root of the `resultReference` location.

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
| `ENABLED_PLACEHOLDER` | Yes | Link to image location | Image in `pictures` structure. `picture.text` empty. Generic placeholder token IDs in `picture.children_ids` |
| `ENABLED_TEXT` | Yes | Text extracted directly from the image | Image in `pictures`. OCR text in `picture.text`. Token IDs in `picture.children_ids` |
| `ENABLED_VERBALIZATION` | Yes | Link + textual description of the image | Image in `pictures`. Natural language description in `picture.verbalization` (only for verbalized images). Token IDs in `picture.children_ids` |
| `ENABLED_VERBALIZATION_ALL` | Yes | Link + textual description of the image | Same as `ENABLED_VERBALIZATION`, but **all** embedded images are verbalized, not just graphs, charts, and screenshots |

> Images extracted in any mode are stored as `.png` files in the `embedded_images_assembly/` folder within the output location.

---

## Key-Value Pair Extraction

KVP extraction pulls structured field data out of documents alongside the text. It requires `kvpMode(KvpMode.GENERIC_WITH_SEMANTIC)` and `Type.JSON` as output format, because results are only included in the JSON output.

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
| `SchemaMergeStrategy.REPLACE` | Only your custom schemas are used. All pre-defined schemas are ignored | You have a known document format with unique fields, or your custom schema conflicts with a pre-defined one |
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
| `documentReference` | DocumentReference | Override the default input location for this request - accepts `CosReference` or `ContainerReference` |
| `resultReference` | DocumentReference | Override the default output location for this request - accepts `CosReference` or `ContainerReference` |
| `timeout` | Duration | Override the service-level timeout for this request |
| `addCustomProperty` | String, Object | Add arbitrary key-value metadata to the request |
| `projectId` | String | Override the default Project ID |
| `spaceId` | String | Override the default Space ID |
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
| `KvpMode.INVOICE` | Extract key-value pairs using the built-in invoice template |
| `KvpMode.UBILL` | Extract key-value pairs using the built-in utility-bill template |
| `KvpMode.GENERIC_WITH_SEMANTIC` | Extract generic and schema-based KVP data. Use with `semanticConfig` to configure the extraction pipeline |

### OCR Modes

| Value | Sent to API | Description |
|-------|-------------|-------------|
| `OcrMode.AUTO` | *(not sent)* | Service automatically selects the best OCR option |
| `OcrMode.DISABLED` | `"disabled"` | OCR is disabled. The document must contain native text |
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

`ExtractionResult` is returned by `entity().results()` and carries status and progress information about the extraction job.

| Field | Type | Description |
|-------|------|-------------|
| `status()` | String | Current status: `submitted`, `uploading`, `running`, `downloading`, `downloaded`, `completed`, `failed`, or `ai_processing` |
| `runningAt()` | String | Timestamp when processing started |
| `completedAt()` | String | Timestamp when processing completed or failed |
| `numberPagesProcessed()` | Integer | Number of pages processed so far |
| `totalPages()` | Integer | Total number of pages to process |
| `location()` | List\<String\> | Paths of the output files produced in COS (populated when status is `completed`) |
| `error()` | Error | Error details if status is `failed` |

> **Note:** `entity().results().location()` (a `List<String>` of output file paths on `ExtractionResult`) is distinct from `entity().resultsReference().location()` (a `CosDataLocation` object with `fileName()`, `bucket()`, and `path()` fields on the `DataReference`). The async example above uses `resultsReference().location().fileName()` to find the output file path when a single output format is requested.

---

## Related Resources

- [Text Extraction Documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html?context=wx&audience=wdp)
- [Text Extraction Parameters Documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction-params.html?context=wx&audience=wdp)
- [Key-Value Pair Extraction Modes](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction-kvp.html?context=wx&audience=wdp)
- [Text Extraction API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#text-extraction)
- [Text Extraction Sample](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/text-extraction)
