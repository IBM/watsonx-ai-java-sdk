---
layout: default
title: Foundation Model Service
parent: Services
nav_order: 6
permalink: /services/foundation-model-service/
---

# Foundation Model Service

The `FoundationModelService` provides functionality to browse and query the **IBM watsonx.ai model catalog**. It allows you to retrieve available foundation models, inspect their capabilities and metadata, and filter results by provider, task, function, lifecycle state, and more.

## Quick Start

```java
FoundationModelService service = FoundationModelService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .build();

FoundationModel model = service.getModel("meta-llama/llama-3-3-70b-instruct").orElseThrow();
System.out.println("Max output tokens: " + model.modelId());
// → Max output tokens: meta-llama/llama-3-3-70b-instruct
System.out.println("Max sequence length: " + model.maxSequenceLength());
// → Max sequence length: 131072
```

> **Note:** Authentication is not required to query the model catalog. The `baseUrl` is the only mandatory parameter.

---

## Overview

The `FoundationModelService` enables you to:

- Retrieve the full list of available foundation models.
- Look up a specific model by its ID.
- Filter models by provider, task, function, tier, lifecycle state, and more.
- Combine multiple filter expressions using logical `and` / `or` operators.
- Paginate through large result sets.
- Retrieve the list of supported tasks.

---

## Service Configuration

### Basic Setup

```java
FoundationModelService service = FoundationModelService.builder()
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `techPreview` | Boolean | No | Include Tech Preview models globally (default: false) |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

---

## Examples

### Retrieving All Models

```java
FoundationModelResponse<FoundationModel> response = service.getModels();
System.out.println("Total models: " + response.totalCount());
// → Total models: 28
```

### Retrieving a Specific Model

Use `getModel()` to look up a single model by its ID. The method returns an `Optional` so you can safely handle the case where the model is not found.

```java
service.getModel("ibm/granite-4-h-small").ifPresent(model -> {
    System.out.println("Model: " + model.modelId());
    // → Model: ibm/granite-4-h-small
    System.out.println("Max output tokens: " + model.maxSequenceLength());
    // → Max output tokens: 131072
});
```

### Filtering Models

Pass a `Filter` directly to `getModels()` for simple filtering:

```java
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.modelId;

var response = service.getModels(Filter.of(modelId("ibm/granite-4-h-small")));
System.out.println(response.totalCount()); // → 1
```

### Using Advanced Parameters

Use `FoundationModelParameters` when you need pagination, Tech Preview models, or combined filtering:

```java
FoundationModelParameters parameters = FoundationModelParameters.builder()
    .limit(50)
    .techPreview(true)
    .filter(Filter.of(provider("IBM")))
    .build();

FoundationModelResponse<FoundationModel> response = service.getModels(parameters);
response.resources().forEach(m -> System.out.println(m.modelId()));
// → ibm/granite-3-2-8b-instruct
// → ibm/granite-3-8b-instruct
// → ibm/granite-4-h-small
// → ...
```

### Retrieving Tasks

```java
FoundationModelResponse<FoundationModelTask> tasks = service.getTasks();
tasks.resources().forEach(task -> System.out.println(task.taskId() + ": " + task.label()));
// → question_answering: Question answering
// → summarization: Summarization
// → ...
```

---

## Filters

The `Filter` class provides a fluent API to build filter expressions for querying the model catalog. Filters are composed from `FilterExpression` instances and can be combined using logical `and` or `or` operators.

> **Tip:** Use a static import for cleaner syntax: `import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.*;`

### Filter Constructors

| Method | Description |
|--------|-------------|
| `Filter.of(expressions...)` | Combines expressions with the default operator (no explicit `and`/`or` suffix) |
| `Filter.and(expressions...)` | Combines expressions with `:and` |
| `Filter.or(expressions...)` | Combines expressions with `:or` |

### Filter Expressions

| Expression | Description |
|------------|-------------|
| `modelId(String)` | Match a specific model ID |
| `provider(String)` | Match by model provider (e.g., `"IBM"`, `"Meta"`) |
| `source(String)` | Match by model source |
| `inputTier(String)` | Match by input pricing tier |
| `tier(String)` | Match by input or output tier |
| `task(String)` | Match by supported task ID (e.g., `"summarization"`) |
| `lifecycle(String)` | Match by lifecycle state (e.g., `"active"`, `"deprecated"`) |
| `function(String)` | Match by supported function (e.g., `"embedding"`, `"rerank"`) |
| `not(expression)` | Negate any expression |

### Filter Examples

```java
import static com.ibm.watsonx.ai.foundationmodel.filter.Filter.Expression.*;

// Models supporting the summarization task from IBM
var filter = Filter.and(provider("IBM"), task("summarization"));

// Models with the rerank function, excluding those that also support embedding
var filter = Filter.and(function("rerank"), not(function("embedding")));

// A specific model by ID
var filter = Filter.of(modelId("ibm/granite-4-h-small"));

// Active models from IBM or Meta
var filter = Filter.or(provider("IBM"), provider("Meta"));
```
---

## Foundation Model Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `start` | Integer | Pagination start token (from `response.next().start()`) |
| `limit` | Integer | Number of results to return (1–200, default: 100) |
| `filter` | Filter | Filter expression to apply |
| `techPreview` | Boolean | Include Tech Preview models for this request |
| `transactionId` | String | Request tracking ID |

---

## FoundationModelResponse

The `FoundationModelResponse<T>` is a generic paginated response used for both models and tasks.

| Field | Type | Description |
|-------|------|-------------|
| `resources()` | List\<T\> | The list of returned items (models or tasks) |
| `totalCount()` | Integer | Total number of matching resources |
| `limit()` | Integer | Number of items returned in this page |
| `first()` | Pagination | Reference to the first page |
| `next()` | Pagination | Reference to the next page, or `null` if on the last page |

The `Pagination` object exposes `start()` and `limit()` as `Optional<Integer>` values extracted from the href URL, making it easy to build follow-up requests.

---

## FoundationModelTask

Each `FoundationModelTask` returned by `getTasks()` contains:

| Field | Type | Description |
|-------|------|-------------|
| `taskId()` | String | Unique task identifier (e.g., `"summarization"`) |
| `label()` | String | Human-readable label for the task |
| `rank()` | Integer | UI ordering rank |
| `description()` | String | Brief description of the task |

---

## FoundationModel

Each `FoundationModel` in the response exposes a rich set of metadata fields.

### Core Fields

| Field | Type | Description |
|-------|------|-------------|
| `modelId()` | String | Unique model identifier (e.g., `"ibm/granite-4-h-small"`) |
| `label()` | String | Human-readable display name |
| `provider()` | String | Model provider (e.g., `"IBM"`, `"Meta"`) |
| `source()` | String | Model source |
| `shortDescription()` | String | Brief description of the model |
| `longDescription()` | String | Detailed description |
| `inputTier()` | String | Input pricing tier |
| `outputTier()` | String | Output pricing tier |
| `numberParams()` | String | Number of parameters (e.g., `"7B"`) |
| `taskIds()` | List\<String\> | List of supported task identifiers |
| `supportedLanguages()` | List\<String\> | List of supported language codes |
| `dataType()` | String | Data type used by the model |
| `architectureType()` | String | Model architecture type |
| `termsUrl()` | String | URL to the model's terms and conditions |

### Convenience Methods

| Method | Type | Description |
|--------|------|-------------|
| `maxOutputTokens()` | Integer | Maximum number of output tokens (shorthand for `modelLimits().maxOutputTokens()`) |
| `maxSequenceLength()` | Integer | Maximum sequence length (shorthand for `modelLimits().maxSequenceLength()`) |

### ModelLimits

Accessible via `model.modelLimits()`, this nested record contains:

| Field | Type | Description |
|-------|------|-------------|
| `maxSequenceLength()` | Integer | Maximum context length in tokens |
| `maxOutputTokens()` | Integer | Maximum number of tokens the model can generate |
| `trainingDataMaxRecords()` | Integer | Maximum training records for fine-tuning |
| `embeddingDimension()` | Integer | Embedding vector dimension (for embedding models) |

### Lifecycle

Accessible via `model.lifecycle()`, each entry describes a lifecycle stage:

| Field | Type | Description |
|-------|------|-------------|
| `id()` | String | Lifecycle state identifier (e.g., `"active"`, `"deprecated"`) |
| `startDate()` | String | Date when this lifecycle state began |
| `alternativeModelIds()` | List\<String\> | Suggested replacement models when deprecated |

### Functions and Tasks

`model.functions()` returns a list of `Function` records, each with an `id()` string (e.g., `"text_generation"`, `"embedding"`, `"rerank"`).

`model.tasks()` returns a list of `Task` records with:

| Field | Type | Description |
|-------|------|-------------|
| `id()` | String | Task identifier |
| `ratings()` | Ratings | Quality ratings for this task |
| `tags()` | List\<String\> | Tags associated with the task |

### Versions

`model.versions()` returns a list of `Version` records:

| Field | Type | Description |
|-------|------|-------------|
| `version()` | String | Version identifier |
| `availableDate()` | String | Date when this version became available |

---

## Related Resources

- [Foundation Models API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#list-foundation-model-specs)