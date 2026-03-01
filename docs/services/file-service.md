---
layout: default
title: File Service
parent: Services
nav_order: 12
permalink: /services/file-service/
---

# File Service

The `FileService` provides functionality to upload, list, and retrieve files using the **IBM watsonx.ai Files APIs**. Files uploaded through this service are primarily used as input for batch processing jobs.

## Quick Start

```java
FileService fileService = FileService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .build();

FileData fileData = fileService.upload(Path.of("mydata.jsonl"));
System.out.println("File ID: " + fileData.id());
// → File ID: file-AQIDkP4L...
```

---

## Overview

The `FileService` enables you to:

- Upload files from a local path, `File`, or `InputStream`.
- List uploaded files with optional filtering by purpose, sort order, and pagination.
- Retrieve the raw content of an uploaded file by its identifier.

### Relationship with BatchService

`FileService` is the foundation layer for batch processing. While it can be used standalone, it is most commonly used together with [`BatchService`](batch-service), which depends on it for two operations:

- **Upload** — `BatchService` calls `FileService.upload()` internally when you submit a job via `Path`, `File`, or `InputStream`.
- **Retrieval** — `BatchService` calls `FileService.retrieve()` internally when using `submitAndFetch()` to read the output file once the job completes.

You can also use `FileService` directly to manage files independently of the batch lifecycle — for example, to inspect an output file manually or to pre-upload a file before submitting multiple jobs against it.

---

## Service Configuration

### Basic Setup

```java
FileService fileService = FileService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `projectId` | String | Conditional | Project ID where files will be managed |
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

### Uploading a File

The service accepts a `Path`, a `File`, or an `InputStream`. All uploads default to `Purpose.BATCH`.

**From a Path:**

```java
FileData fileData = fileService.upload(Path.of("mydata.jsonl"));
System.out.println(fileData.id());       // → file-AQIDkP4L...
System.out.println(fileData.filename()); // → mydata.jsonl
System.out.println(fileData.purpose());  // → batch
```

**From an InputStream:**

```java
InputStream is = new FileInputStream("mydata.jsonl");
FileData fileData = fileService.upload(is, "mydata.jsonl");
```

**With full control via `FileUploadRequest`** — override `project_id`, `space_id`, `purpose`, and `transaction_id`:

```java
FileData fileData = fileService.upload(
    FileUploadRequest.builder()
        .inputStream(is)
        .fileName("mydata.jsonl")
        .purpose(Purpose.BATCH)
        .projectId("override-project-id")
        .spaceId("override-space-id")
        .transactionId("my-transaction-id")
        .build()
);
```

### Listing Files

Call `list()` with no arguments to retrieve all uploaded files:

```java
FileListResponse response = fileService.list();
response.data().forEach(f -> System.out.println(f.id() + " – " + f.filename()));
```

Use `FileListRequest` to filter and paginate results:

```java
FileListResponse response = fileService.list(
    FileListRequest.builder()
        .limit(10)
        .order(Order.DESC)
        .purpose(Purpose.BATCH)
        .after("file-abc123") // cursor from previous response
        .build()
);

System.out.println("Has more: " + response.hasMore());
System.out.println("Last ID:  " + response.lastId());
```

### Retrieving File Content

Retrieve the raw content of a file by its identifier:

```java
String content = fileService.retrieve("file-AQIDkP4L...");
System.out.println(content);
// → {"custom_id": "a", "method": "POST", ...}
```

With full control via `FileRetrieveRequest` — override `project_id`, `space_id`, and `transaction_id`:

```java
String content = fileService.retrieve(
    FileRetrieveRequest.builder()
        .fileId("file-AQIDkP4L...")
        .projectId("my-project-id")
        .spaceId("my-space-id")
        .transactionId("my-transaction-id")
        .build()
);
```

---

## FileData

Returned by `upload()` and contained in `FileListResponse.data()`.

| Field | Type | Description |
|-------|------|-------------|
| `id()` | String | Unique file identifier |
| `object()` | String | Always `"file"` |
| `bytes()` | Integer | Size of the file in bytes |
| `createdAt()` | Long | Unix timestamp when the file was created |
| `expiresAt()` | Long | Unix timestamp when the file expires |
| `filename()` | String | Original file name |
| `purpose()` | String | Purpose of the file (e.g., `"batch"`) |

## FileListResponse

Returned by `list()`.

| Field | Type | Description |
|-------|------|-------------|
| `object()` | String | Always `"list"` |
| `data()` | List\<FileData\> | List of files matching the request |
| `firstId()` | String | ID of the first file in the current page |
| `lastId()` | String | ID of the last file — use as `after` cursor for the next page |
| `hasMore()` | Boolean | Whether more files are available beyond this page |

---

## Reference

### Purpose

| Value | API String | Description |
|-------|------------|-------------|
| `Purpose.BATCH` | `"batch"` | File intended for use as input in a batch job |

### Order

| Value | API String | Description |
|-------|------------|-------------|
| `Order.ASC` | `"asc"` | Sort by `created_at` ascending |
| `Order.DESC` | `"desc"` | Sort by `created_at` descending |

---

## Related Resources

- [Files APIs Reference](https://cloud.ibm.com/apidocs/watsonx-ai#upload-batch-file)
- [Batch Service](../batch-service)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/batch)