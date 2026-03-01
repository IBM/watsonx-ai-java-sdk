---
layout: default
title: Batch Service
parent: Services
nav_order: 13
permalink: /services/batch-service/
---

# Batch Service

The `BatchService` provides functionality to submit and manage batch jobs using the **IBM watsonx.ai Batches API**. A batch job processes multiple requests from a `JSONL` input file.

## Relationship with FileService

`BatchService` and `FileService` work in tandem. Input files must be uploaded to IBM Cloud Object Storage before a batch job can reference them, and output files must be retrieved from COS once the job completes. `BatchService` handles both steps automatically when a `FileService` instance is provided:

- **File upload** — when submitting via `Path`, `File`, or `InputStream`, `BatchService` calls `FileService.upload()` internally and assigns the resulting `file_id` to the request.
- **Output retrieval** — when using `submitAndFetch()`, `BatchService` calls `FileService.retrieve()` internally once the job completes and deserializes each output line into the requested type.

If you already have a `file_id` from a previous upload, you can submit directly without these automatic steps using `BatchCreateRequest.inputFileId()`.

---

## Quick Start

```java
FileService fileService = FileService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .build();

BatchService batchService = BatchService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .endpoint("/v1/chat/completions")
    .fileService(fileService)
    .build();

List<BatchResult<ChatResponse>> results = batchService.submitAndFetch(
    Path.of("requests.jsonl"),
    ChatResponse.class
);

results.forEach(r ->
    System.out.println(r.customId() + ": " + r.response().body().toAssistantMessage().content())
);
```

---

## Overview

The `BatchService` enables you to:

- Submit batch jobs from a `Path`, `File`, `InputStream`, or a pre-uploaded `file_id`.
- Wait for job completion and retrieve deserialized results with `submitAndFetch()`.
- List, retrieve, and cancel batch jobs.

---

## Service Configuration

### Basic Setup

```java
BatchService batchService = BatchService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .endpoint("/v1/chat/completions")
    .fileService(fileService)
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `projectId` | String | Conditional | Project ID where batch jobs will run |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `endpoint` | String | Yes | Default API endpoint for batch inference (e.g., `/v1/chat/completions`) |
| `fileService` | FileService | Conditional | Required when submitting via file upload or using `submitAndFetch()` |
| `timeout` | Duration | No | Request and polling timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

---

## Input File Format

Input files must be in [JSONL](https://jsonlines.org/) format — one JSON object per line. Each line represents a single inference request and must include a `custom_id` to correlate results with inputs.

```json
{"custom_id": "a", "method": "POST", "url": "/v1/chat/completions", "body": {"model": "ibm/granite-4-h-small", "messages": [{"role": "user", "content": [{"type": "text", "text": "Capital of Italy"}]}], "max_completion_tokens": 0, "time_limit": 30000, "temperature": 0}}
{"custom_id": "b", "method": "POST", "url": "/v1/chat/completions", "body": {"model": "ibm/granite-4-h-small", "messages": [{"role": "user", "content": [{"type": "text", "text": "Capital of France"}]}], "max_completion_tokens": 0, "time_limit": 30000, "temperature": 0}}
{"custom_id": "c", "method": "POST", "url": "/v1/chat/completions", "body": {"model": "ibm/granite-4-h-small", "messages": [{"role": "user", "content": [{"type": "text", "text": "Capital of Germany"}]}], "max_completion_tokens": 0, "time_limit": 30000, "temperature": 0}}
```

---

## Examples

### Submit and Fetch Results

`submitAndFetch()` uploads the file, submits the job, polls until completion, and returns the deserialized results. It requires a `FileService` to be configured on the builder.

**From a Path:**

```java
var results = batchService.submitAndFetch(Path.of("requests.jsonl"), ChatResponse.class);
results.forEach(r ->
    System.out.println(r.customId() + ": " + r.response().body().toAssistantMessage().content())
);
// → a: Rome
// → b: Paris
// → c: Berlin
```

**From an InputStream:**

```java
InputStream is = new FileInputStream("requests.jsonl");
List<BatchResult<ChatResponse>> results = batchService.submitAndFetch(is, "requests.jsonl", ChatResponse.class);
```

**From a pre-uploaded `file_id`** — when you have already uploaded the file via `FileService`:

```java
FileData fileData = fileService.upload(Path.of("requests.jsonl"));

List<BatchResult<ChatResponse>> results = batchService.submitAndFetch(
    BatchCreateRequest.builder()
        .inputFileId(fileData.id())
        .build(),
    ChatResponse.class
);
```

### Submit Without Waiting

Use `submit()` to start the job and return immediately with the job metadata, without blocking for completion:

```java
BatchData batchData = batchService.submit(Path.of("requests.jsonl"));
System.out.println("Job ID: " + batchData.id());
System.out.println("Status: " + batchData.status());
// → Job ID: batch-AQIDkP4L...
// → Status: validating
```

Then poll manually and retrieve the output once complete:

```java
while (true) {
    batchData = batchService.retrieve(batchData.id());

    if (batchData.status().equals(Status.COMPLETED.value())) {
        String output = fileService.retrieve(batchData.outputFileId());
        System.out.println(output);
        break;
    } else if (batchData.status().equals(Status.FAILED.value())) {
        System.err.println("Batch failed: " + batchData.errors());
        break;
    }

    Thread.sleep(2000);
}
```

### Customizing a Request

Use `BatchCreateRequest` to override the endpoint, set a custom completion window, or attach metadata:

```java
BatchData batchData = batchService.submit(
    BatchCreateRequest.builder()
        .inputFileId(fileData.id())
        .endpoint("/v1/chat/completions") // overrides the service-level default
        .completionWindow("1h")           // defaults to "24h" if not set
        .metadata(Map.of("job", "nightly-run"))
        .projectId("override-project-id")
        .transactionId("my-transaction-id")
        .build()
);
```

### Listing Batch Jobs

```java
// All jobs (default limit: 20)
BatchListResponse response = batchService.list();

// With a custom limit
BatchListResponse response = batchService.list(
    BatchListRequest.builder()
        .limit(10)
        .build()
);

response.data().forEach(b -> System.out.println(b.id() + " – " + b.status()));
System.out.println("Has more: " + response.hasMore());
```

### Cancelling a Batch Job

Cancelling transitions the job to `cancelling` and eventually to `cancelled`. Partial results, if available, are preserved in the output file.

```java
BatchData batchData = batchService.cancel("batch-abc123");
System.out.println(batchData.status()); // → cancelling
```

---

## BatchData

Returned by `submit()`, `retrieve()`, `cancel()`, and contained in `BatchListResponse.data()`.

| Field | Type | Description |
|-------|------|-------------|
| `id()` | String | Unique batch job identifier |
| `object()` | String | Always `"batch"` |
| `endpoint()` | String | API endpoint used for inference (e.g., `/v1/chat/completions`) |
| `inputFileId()` | String | Identifier of the uploaded input file |
| `completionWindow()` | String | Time window for completion (e.g., `"24h"`) |
| `status()` | String | Current job status — see [Status](#status) |
| `outputFileId()` | String | Identifier of the output file; available once completed |
| `errorFileId()` | String | Identifier of the error file, if any requests failed |
| `errors()` | FileErrors | Validation or processing errors, if any |
| `requestCounts()` | RequestCounts | Summary of total, completed, and failed request counts |
| `metadata()` | Map\<String, String\> | User-defined key-value metadata |
| `createdAt()` | Long | Unix timestamp when the job was created |
| `inProgressAt()` | Long | Unix timestamp when processing started |
| `finalizingAt()` | Long | Unix timestamp when the job entered the finalizing phase |
| `completedAt()` | Long | Unix timestamp when the job completed successfully |
| `failedAt()` | Long | Unix timestamp when the job failed |
| `expiresAt()` | Long | Unix timestamp when the job will expire |
| `expiredAt()` | Long | Unix timestamp when the job expired |
| `cancellingAt()` | Long | Unix timestamp when cancellation was requested |
| `cancelledAt()` | Long | Unix timestamp when the job was fully cancelled |

## BatchResult

Returned per-item by `submitAndFetch()`. Each entry corresponds to one line in the input JSONL file.

| Field | Type | Description |
|-------|------|-------------|
| `id()` | String | Unique identifier of this result entry |
| `customId()` | String | The `custom_id` from the original input line — use this to correlate results with inputs |
| `response()` | Response\<T\> | HTTP response wrapper containing status code, request ID, and deserialized body |
| `processedAt()` | Long | Unix timestamp when this request was processed |

`response().statusCode()` contains the HTTP status for this individual request. `response().body()` is deserialized into the class passed to `submitAndFetch()` (e.g., `ChatResponse.class`).

---

## Status

The `Status` enum covers the terminal states used internally for polling. The full set of status values returned by the API:

| Value | Description |
|-------|-------------|
| `validating` | Input file is being validated |
| `in_progress` | Job is actively processing requests |
| `finalizing` | Processing complete, output is being assembled |
| `completed` | Job finished successfully — output file is available |
| `failed` | Job failed — check `errors()` and `errorFileId()` |

---

## Related Resources

- [Batches API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#create-batch)
- [File Service](../file-service)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/batch)
