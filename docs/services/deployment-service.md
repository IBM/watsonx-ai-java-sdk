---
layout: default
title: Deployment Service
parent: Services
nav_order: 11
permalink: /services/deployment-service/
---

# Deployment Service

The `DeploymentService` allows you to interact with **models deployed in IBM watsonx.ai deployment spaces**. Instead of referencing a `modelId`, every request targets a `deploymentId` — the identifier of an already-deployed asset. The service supports the same operations as `ChatService` and `TimeSeriesService` (chat, streaming chat, text generation, streaming text generation, time series forecasting), plus the ability to inspect a deployment's metadata via `findById`.

## What is a Deployment Space?

A **deployment space** is an IBM watsonx.ai workspace that contains deployable assets, their deployments, and associated environments. Assets (foundation models, prompt-tuned models, prompt templates) are **promoted from projects** into a deployment space before they can be deployed. A single asset can be deployed to multiple spaces — for example, a test space and a production space.

Once deployed, each deployment is identified by a unique `deploymentId`. You use this ID in every `DeploymentService` request instead of a `modelId`.

## Quick Start

```java
DeploymentService deploymentService = DeploymentService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .build();

var chatRequest = ChatRequest.builder()
    .deploymentId(DEPLOYMENT_ID)
    .messages(UserMessage.text("Hello!"))
    .build();

var response = deploymentService.chat(chatRequest);
System.out.println(response.toAssistantMessage().content());
```

---

## Overview

The `DeploymentService` enables you to:

- Send synchronous and streaming **chat** requests to a deployed model.
- Run **time series forecasting** against a deployed TTM model, with optional `futureData` for exogenous features.
- Retrieve **deployment metadata** (`findById`) including status, inference endpoints, asset type, and hardware configuration.

---

## Service Configuration

### Basic Setup

```java
DeploymentService deploymentService = DeploymentService.builder()
    .baseUrl(CloudRegion.DALLAS)   // or use a URL string
    .apiKey(WATSONX_API_KEY)
    .build();
```

No `projectId`, `spaceId`, or `modelId` is required — all routing is done through the `deploymentId` in each request.

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai ML endpoint |
| `timeout` | Duration | No | Default request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `defaultParameters` | ChatParameters | No | Fallback chat parameters applied to every chat request |
| `messageInterceptor` | MessageInterceptor | No | Post-processing hook for the assistant's text content |
| `toolInterceptor` | ToolInterceptor | No | Post-processing hook for function call arguments |

> Either `apiKey` or `authenticator` must be provided. `projectId`, `spaceId`, and `modelId` are **ignored** — if set on a request's parameters object, a warning is logged.

---

## Chat

### Synchronous Chat

```java
var chatRequest = ChatRequest.builder()
    .deploymentId(DEPLOYMENT_ID)
    .messages(
        SystemMessage.of("You are a helpful assistant."),
        UserMessage.text("Hello, how are you?"),
    ).build();

ChatResponse response = deploymentService.chat(chatRequest);
```

### Streaming Chat

```java
var chatRequest = ChatRequest.builder()
    .deploymentId(DEPLOYMENT_ID)
    .messages(UserMessage.text("Tell me a joke."))
    .build();

CompletableFuture<ChatResponse> future = deploymentService.chatStreaming(chatRequest,
    new ChatHandler() {
        @Override
        public void onPartialResponse(String partial, PartialChatResponse partialResponse) {
            System.out.print(partial);
        }

        @Override
        public void onCompleteResponse(ChatResponse response) {
            System.out.println("\n[Done]");
        }

        @Override
        public void onError(Throwable error) {
            error.printStackTrace();
        }
    }
);

future.join(); // wait for completion
```

---

## Time Series Forecasting

The `DeploymentService` supports time series forecasting via `forecast()`, with one key addition over `TimeSeriesService`: **`futureData`** — exogenous features known in advance for the forecast horizon (e.g. holidays, scheduled events).

```java
InputSchema schema = InputSchema.builder()
    .timestampColumn("date")
    .addIdColumn("ID1")
    .build();

ForecastData historicalData = ForecastData.create()
    .addAll("date", "2020-01-01T00:00:00", "2020-01-01T01:00:00", "2020-01-05T01:00:00")
    .addAll("ID1", "D1", "D1", "D1")
    .addAll("TARGET1", 1.46, 2.34, 4.55);

ForecastData futureData = ForecastData.create()
    .add("date", "2021-01-01T00:00:00")
    .add("ID1", "D1")
    .add("TARGET1", 5);

TimeSeriesParameters parameters = TimeSeriesParameters.builder()
    .futureData(futureData)
    .build();

TimeSeriesRequest request = TimeSeriesRequest.builder()
    .deploymentId(DEPLOYMENT_ID)
    .inputSchema(schema)
    .data(historicalData)
    .parameters(parameters)
    .build();

ForecastResponse result = deploymentService.forecast(request);
System.out.println("Output data points: " + result.outputDataPoints());
```

> `futureData` is **only supported** by `DeploymentService`. When using `TimeSeriesService` directly, it is not available.

---

## Finding a Deployment

Use `findById` to inspect a deployment's metadata, status, and inference endpoints:

```java
var request = FindByIdRequest.builder()
    .deploymentId(DEPLOYMENT_ID)
    .spaceId(SPACE_ID)             
    .build();

DeploymentResource resource = deploymentService.findById(request);
```

### FindByIdRequest Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deploymentId` | String | Yes | The unique deployment identifier |
| `projectId` | String | Conditional | Project ID where the deployment resides |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `transactionId` | String | No | Request tracking ID |

> Either `projectId` or `spaceId` must be provided.

### DeploymentResource

| Field | Type | Description |
|-------|------|-------------|
| `metadata().id()` | String | Deployment unique identifier |
| `metadata().name()` | String | Human-readable name |
| `metadata().description()` | String | Deployment description |
| `metadata().createdAt()` | String | Creation timestamp |
| `metadata().modifiedAt()` | String | Last modification timestamp |
| `metadata().spaceId()` | String | Space where the deployment lives |
| `metadata().projectId()` | String | Project where the deployment lives |
| `metadata().tags()` | List\<String\> | Tags |
| `entity().deployedAssetType()` | String | Type of deployed asset (`prompt_tune`, `foundation_model`, `custom_foundation_model`) |
| `entity().baseModelId()` | String | The underlying foundation model |
| `entity().status().state()` | String | Deployment state (e.g., `ready`, `failed`) |
| `entity().status().inference()` | List\<Inference\> | List of inference endpoints |
| `entity().status().message()` | Message | Status message with `level()` and `text()` |
| `entity().status().failure()` | ApiErrorResponse | Error details if state is `failed` |
| `entity().asset()` | ModelRel | Model asset reference with `id()` and `rev()` |
| `entity().promptTemplate()` | SimpleRel | Prompt template reference (if applicable) |
| `entity().hardwareSpec()` | HardwareSpec | Hardware specification (`id`, `name`, `numNodes`) |
| `entity().online().parameters()` | Map\<String, Object\> | Online deployment parameters (e.g., `serving_name`) |
| `entity().custom()` | Map\<String, Object\> | User-defined metadata |

---

## Related Resources

- [Deployment Spaces Documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/wmls/wmls-deploy-overview.html)
- [Deployments API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#create-deployment)
- [ChatService Documentation](../chat-service)
- [Time Series Service Documentation](../time-series-service)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/deployment)
