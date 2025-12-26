---
layout: default
title: Rerank Service
parent: Services
nav_order: 3
permalink: /services/rerank-service/
---

# Rerank Service

The `RerankService` provides functionality to rerank a list of text candidates against a query using **IBM watsonx.ai reranker models**. It scores and sorts input texts by their relevance to a given query, making it ideal for improving search results, retrieval-augmented generation (RAG) pipelines, and document retrieval systems.

## Quick Start

```java
RerankService rerankService = RerankService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
    .build();

RerankResponse response = rerankService.rerank(
    "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
    List.of(
        "In my younger years, I often reveled in the excitement...", 
        "As a young man, I frequently sought out exhilarating..."
    )
);

response.results().forEach(r -> System.out.printf("[%d] score=%.4f%n", r.index(), r.score()));
// → [0] score=2.9258
// → [1] score=-0.9204
```

> **Note:** To see the list of available reranking models, refer to [supported reranker models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models-embed.html?context=wx&audience=wdp#rerank).

---

## Overview

The `RerankService` enables you to:

- Score and rerank a list of candidate texts against a query.
- Control the number of top results returned with `topN`.
- Truncate long inputs automatically to fit within model token limits.
- Integrate reranking into RAG pipelines for improved retrieval quality.

---

## Service Configuration

### Basic Setup

```java
RerankService rerankService = RerankService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
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
| `modelId` | String | Yes | Reranking model ID |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

---

## Examples

### Basic Reranking

```java
RerankService rerankService = RerankService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("cross-encoder/ms-marco-minilm-l-12-v2")
    .build();

RerankResponse response = rerankService.rerank(
    "As a Youth, I craved excitement while in adulthood I followed Enthusiastic Pursuit.",
    List.of(
        "In my younger years, I often reveled in the excitement...", 
        "As a young man, I frequently sought out exhilarating..."
    )
);

response.results().forEach(r -> System.out.printf("[%d] score=%.4f%n", r.index(), r.score()));
// → [0] score=2.9258
// → [1] score=-0.9204
```

### Returning Only the Top N Results

Use `topN` to limit the response to the most relevant candidates:

```java
RerankParameters parameters = RerankParameters.builder()
    .topN(3)
    .build();

RerankResponse response = rerankService.rerank(
    "Which document is about climate change?",
    List.of(
        "The Amazon rainforest is home to many species...",
        "Global temperatures have risen significantly due to greenhouse gases...",
        "The stock market saw record highs this quarter...",
        "Melting ice caps are a key indicator of climate change...",
        "The new smartphone features an improved camera..."
    ),
    parameters
);

response.results().forEach(r -> System.out.printf("[%d] %.4f%n", r.index(), r.score()));
// → [3] -3.9531
// → [1] -8.0313
// → [0] -11.0469
```

### Truncating Long Inputs

If any of your inputs may exceed the model's token limit, use `truncateInputTokens` to avoid errors. Inputs are truncated from the right, preserving the start of the text.

```java
RerankParameters parameters = RerankParameters.builder()
    .truncateInputTokens(512)
    .build();

RerankResponse response = rerankService.rerank(query, longDocuments, parameters);
```
---

## Rerank Parameters

The `RerankParameters` class allows you to customize the reranking behavior.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `truncateInputTokens` | Integer | Maximum tokens per input. Inputs exceeding this limit are truncated from the right. Must be > 1. |
| `topN` | Integer | Return only the top N ranked results. Must be > 1. |
| `inputs` | Boolean | When `true`, each result includes the original input text in `input().text()`. |
| `query` | Boolean | When `true`, the original query is included in the response via `response.query()`. |
| `modelId` | String | Override the default model for this request. |
| `projectId` | String | Override the default project ID for this request. |
| `spaceId` | String | Override the default space ID for this request. |
| `transactionId` | String | Request tracking ID. |
| `crypto` | Crypto | Encryption configuration. |

---

## RerankResponse

The `RerankResponse` contains the ranked results and usage metadata.

| Field | Type | Description |
|-------|------|-------------|
| `modelId()` | String | The model used for reranking |
| `modelVersion()` | String | The version of the model |
| `createdAt()` | String | Timestamp of when the response was created |
| `inputTokenCount()` | int | Total number of input tokens processed |
| `query()` | String | The original query (only populated when `query(true)` is set) |
| `results()` | List\<RerankResult\> | The ranked results, ordered by score descending |

Each `RerankResult` exposes:

| Field | Type | Description |
|-------|------|-------------|
| `index()` | int | The original position of this input in the request list |
| `score()` | Double | Relevance score assigned by the model (higher = more relevant) |
| `input()` | RerankInputResult | The original input text (only populated when `inputs(true)` is set) |

---

## Related Resources

- [Rerank API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#text-rerank)
- [Supported Reranker Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models-embed.html?context=wx&audience=wdp#rerank)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/rerank)