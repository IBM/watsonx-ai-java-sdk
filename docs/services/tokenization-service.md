---
layout: default
title: Tokenization Service
parent: Services
nav_order: 5
permalink: /services/tokenization-service/
---

# Tokenization Service

The `TokenizationService` provides functionality to tokenize text using **IBM watsonx.ai foundation models**. It converts a text string into a sequence of tokens, returning the token count and optionally the individual token strings. Both synchronous and asynchronous invocation are supported.

## Quick Start

```java
TokenizationService service = TokenizationService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .build();

TokenizationResponse response = service.tokenize("Tell me a joke");
System.out.println("Token count: " + response.result().tokenCount());
// → Token count: 4
```

---

## Overview

The `TokenizationService` enables you to:

- Count the number of tokens a text string produces for a given model.
- Retrieve the individual token strings with `returnTokens(true)`.
- Run tokenization synchronously or asynchronously via `CompletableFuture`.

---

## Service Configuration

### Basic Setup

```java
TokenizationService service = TokenizationService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .modelId("ibm/granite-4-h-small")
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `projectId` | String | Conditional | Project ID where tokenization will run |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `modelId` | String | Yes | Foundation model ID to use for tokenization |
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

### Count Tokens

The simplest usage — get the token count for a text string without retrieving the individual tokens:

```java
TokenizationResponse response = service.tokenize("Write a tagline for an alumni association: Together we");
System.out.println("Token count: " + response.result().tokenCount());
// → Token count: 11
```

### Retrieve Individual Tokens

Set `returnTokens(true)` to get the list of token strings in addition to the count:

```java
TokenizationParameters parameters = TokenizationParameters.builder()
    .returnTokens(true)
    .build();

TokenizationResponse response = service.tokenize("Write a tagline for an alumni association: Together we", parameters);
System.out.println("Count:  " + response.result().tokenCount());
System.out.println("Tokens: " + response.result().tokens());
// → Count: 11
// → Tokens: [Write, a, tag, line, for, an, alumni, associ, ation:, Together, we]
```

---

## Tokenization Parameters

The `TokenizationParameters` class controls the tokenization behavior per request.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `returnTokens` | Boolean | If `true`, the response includes the list of individual token strings in addition to the count. Default: `false` (count only) |
| `modelId` | String | Override the service-level model ID for this request |
| `projectId` | String | Override the default project ID |
| `spaceId` | String | Override the default space ID |
| `transactionId` | String | Request tracking ID sent as a header for tracing |
| `crypto` | String | Key reference for encrypting the inference request (e.g., IBM Key Protect CRN) |

> `toTokenizationRequestParameters()` returns `null` if `returnTokens` is not set, so no `parameters` block is included in the request body.

---

## TokenizationResponse

| Field | Type | Description |
|-------|------|-------------|
| `modelId()` | String | Identifier of the model used for tokenization |
| `result()` | Result | The tokenization result |
| `result().tokenCount()` | int | Total number of tokens produced |
| `result().tokens()` | List\<String\> | Individual token strings. `null` if `returnTokens` was not set to `true` |

---

## Related Resources

- [Tokenization API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#text-tokenization)
- [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/tokenization)
