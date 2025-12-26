---
layout: default
title: Embedding Service
parent: Services
nav_order: 2
permalink: /services/embedding-service/
---

# Embedding Service

The `EmbeddingService` provides functionality to generate text embeddings using **IBM watsonx.ai encoder models**. It converts text inputs into dense vector representations that can be used for semantic search, similarity comparison, clustering, and retrieval-augmented generation (RAG).

## Quick Start

```java
EmbeddingService embeddingService = EmbeddingService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-embedding-278m-multilingual")
    .build();

EmbeddingResponse response = embeddingService.embedding("Hello, world!");
System.out.println(response.results().get(0).embedding());
// → [-0.029937625, 0.05433679, 0.013135133, 0.018311847, ...]
```

> **Note:** To see the list of available embedding models, refer to [supported encoder models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models-embed.html?context=wx#embed).

---

## Overview

The `EmbeddingService` enables you to:

- Embed single or multiple text inputs in a single request.
- Process large batches automatically.
- Configure token truncation to handle long inputs gracefully.
- Optionally return the original input text alongside each embedding vector.
- Build semantic search, similarity, and RAG applications.

---

## Service Configuration

### Basic Setup

To start using the Embedding Service, create an `EmbeddingService` instance with the minimum required configuration.

```java
EmbeddingService embeddingService = EmbeddingService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com")
    .modelId("ibm/granite-embedding-278m-multilingual")
    .build();
```

### Using CloudRegion

Instead of manually specifying the `baseUrl`, you can use the `CloudRegion` to automatically configure the correct endpoint.

```java
EmbeddingService embeddingService = EmbeddingService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-embedding-278m-multilingual")
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `projectId` | String | Conditional | Project ID where the model is deployed |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `modelId` | String | Yes | Embedding model ID |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

---

## Examples

### Embedding a Single Input

The simplest use case — pass a single string and retrieve its vector representation.

```java
EmbeddingResponse response = embeddingService.embedding("Embedding this!");
List<Float> vector = response.results().get(0).embedding();
System.out.println("Vector size: " + vector.size());
// → Vector size: 768
```

### Embedding Multiple Inputs

Pass multiple strings in a single call. Results are returned in the same order as the inputs.

```java
EmbeddingResponse response = embeddingService.embedding(
    "First input",
    "Second input",
    "Third input"
);

var firstEmbedding = response.results().get(0);
var secondEmbedding = response.results().get(1);
var thirdEmbedding = response.results().get(2);

System.out.println(firstEmbedding);
// → [0.01608275, 0.033017233, 0.01521849, 0.022984304, ...]

System.out.println(secondEmbedding);
// → [-0.0025639886, 0.018150007, -8.951856E-4, 0.030161599, ...]

System.out.println(thirdEmbedding);
// → [0.024885714, -0.005718433, 0.0036718687, 0.03666839, ...]
```

You can also pass a `List<String>`:

```java
List<String> inputs = List.of("apple", "banana", "cherry");
EmbeddingResponse response = embeddingService.embedding(inputs);
```

### Customizing Generation Parameters

Use `EmbeddingParameters` to control token truncation and whether to include the original input text in the response.

```java
EmbeddingParameters parameters = EmbeddingParameters.builder()
    .truncateInputTokens(512)
    .inputText(true)
    .build();

EmbeddingResponse response = embeddingService.embedding(
    List.of("A very long document that might exceed the model's token limit..."),
    parameters
);

EmbeddingResponse.Result result = response.results().get(0);
System.out.println("Input text: " + result.input());
System.out.println("Vector: " + result.embedding());
```

---

## Embedding Parameters

The `EmbeddingParameters` class allows you to fine-tune how inputs are processed.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `truncateInputTokens` | Integer | Maximum number of tokens per input. Inputs exceeding this limit are truncated from the right (the start is preserved). |
| `inputText` | Boolean | When `true`, each result includes the original input text in the `input()` field. |
| `modelId` | String | Override the default model for this request. |
| `projectId` | String | Override the default project ID for this request. |
| `spaceId` | String | Override the default space ID for this request. |
| `transactionId` | String | Request tracking ID. |
| `crypto` | Crypto | Encryption configuration. |

---

## EmbeddingResponse

The `EmbeddingResponse` contains the generated vectors and usage metadata.

| Field | Type | Description |
|-------|------|-------------|
| `modelId()` | String | The model used to generate the embeddings |
| `createdAt()` | String | Timestamp of when the embeddings were generated |
| `results()` | List\<Result\> | One result per input, in the same order as the request |
| `inputTokenCount()` | Integer | Total number of input tokens processed across all inputs |

Each `Result` in the list exposes:

| Field | Type | Description |
|-------|------|-------------|
| `embedding()` | List\<Float\> | The vector representation of the input text |
| `input()` | String | The original input text (only populated when `inputText(true)` is set) |

---

## Related Resources

- [Embeddings API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#text-embeddings)
- [Supported Encoder Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models-embed.html?context=wx#embed)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/embedding-text)
