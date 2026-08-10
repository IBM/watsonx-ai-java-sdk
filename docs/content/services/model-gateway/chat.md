---
id: chat
title: Chat
---

# Model Gateway - Chat

The `ModelGatewayChatService` lets you send chat completions to any third-party model (OpenAI, Anthropic, and others) available through the **IBM watsonx.ai Model Gateway**.

> **Setup required:** The Model Gateway must be installed and configured by an administrator before use. See [Model Gateway Prerequisites](/services/model-gateway#prerequisites).

## Quick Start

```java
ModelGatewayChatService service = ModelGatewayChatService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-4o")
    .build();

ModelGatewayChatResponse response = service.chat("What is the capital of Italy?");
System.out.println(response.toAssistantMessage().content());
// → Rome is the capital of Italy.
```

---

## Overview

`ModelGatewayChatService` enables you to:

- Send synchronous and streaming chat requests to any model available through the gateway.
- Use gateway-specific parameters such as service tier, reasoning effort, audio modalities, caching, and routing configuration.
- Apply `MessageInterceptor` and `ToolInterceptor` for post-processing.
- Read gateway metadata on every response: `serviceTier()`, `systemFingerprint()`, and `cached()`.

---

## Service Configuration

### Basic Setup

```java
ModelGatewayChatService service = ModelGatewayChatService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-4o")
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `baseUrl` | String / CloudRegion | Yes | watsonx.ai ML endpoint |
| `modelId` | String | Yes | Third-party model identifier (e.g., `"gpt-4o"`, `"claude-3-5-sonnet"`) |
| `parameters` | ModelGatewayChatParameters | No | Default parameters applied to every request |
| `tools` | List\<Tool\> | No | Default tools available to the model |
| `messageInterceptor` | MessageInterceptor\<ModelGatewayChatRequest\> | No | Post-processing hook for the assistant's text content |
| `toolInterceptor` | ToolInterceptor\<ModelGatewayChatRequest\> | No | Post-processing hook for function call arguments |
| `timeout` | Duration | No | Default request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided.

### Advanced Configuration

Set default parameters and tools that apply to every request:

```java
ModelGatewayChatParameters defaults = ModelGatewayChatParameters.builder()
    .temperature(0.7)
    .maxCompletionTokens(1000)
    .serviceTier(ServiceTier.AUTO)
    .build();

ModelGatewayChatService service = ModelGatewayChatService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-4o")
    .parameters(defaults)
    .build();
```

Per-request parameters **always take precedence** over service-level defaults. Fields not set on the per-request parameters fall back to the defaults.

---

## Chat operations

### Simple Chat

```java
ModelGatewayChatResponse response = service.chat("Tell me a joke");
System.out.println(response.toAssistantMessage().content());
```

### Multi-Turn Conversation

```java
var messages = new ArrayList<ChatMessage>();
messages.add(SystemMessage.of("You are a helpful assistant"));
messages.add(UserMessage.text("What is the capital of France?"));

ModelGatewayChatResponse response = service.chat(messages);
messages.add(response.toAssistantMessage());

messages.add(UserMessage.text("What is its population?"));
response = service.chat(messages);
System.out.println(response.toAssistantMessage().content());
```

### With Parameters

```java
ModelGatewayChatParameters parameters = ModelGatewayChatParameters.builder()
    .temperature(0.3)
    .maxCompletionTokens(200)
    .build();

ModelGatewayChatResponse response = service.chat(messages, parameters);
```

### With Tools

```java
Tool weatherTool = Tool.of(
    "get_weather",
    "Get current weather for a location",
    JsonSchema.object()
        .property("location", JsonSchema.string("City name"))
        .required("location")
        .build()
);

ModelGatewayChatResponse response = service.chat(messages, parameters, List.of(weatherTool));
AssistantMessage assistant = response.toAssistantMessage();

if (assistant.hasToolCalls()) {
    List<ToolMessage> toolMessages = assistant.processTools((name, args) -> {
        return fetchWeather(args.get("location"));
    });
    messages.add(assistant);
    messages.addAll(toolMessages);
    response = service.chat(messages);
}

System.out.println(response.toAssistantMessage().content());
```

---

## Streaming

### Simple Streaming

```java
CompletableFuture<ChatResponse> future = service.chatStreaming(
    "Tell me a story",
    System.out::print
);
future.join();
```

### Streaming with ChatHandler

```java
service.chatStreaming(
    messages,
    new ChatHandler() {
        @Override
        public void onPartialResponse(String text, PartialChatResponse partial) {
            System.out.print(text);
        }

        @Override
        public void onCompleteResponse(ChatResponse response) {
            System.out.println("\nTotal tokens: " + response.usage().totalTokens());
        }

        @Override
        public void onError(Throwable error) {
            System.err.println("Error: " + error.getMessage());
        }
    }
);
```

---

## Model Gateway Parameters

`ModelGatewayChatParameters` extends the common `BaseChatParameters` with fields specific to the Model Gateway.

### Builder Reference

#### Inherited from BaseChatParameters

| Parameter | Type | Range | Description |
|-----------|------|-------|-------------|
| `modelId` | String | - | Override the model for this request |
| `maxCompletionTokens` | Integer | ≥ 0 | Maximum tokens in the response. `0` is treated as a literal zero - it does not mean "model max" as it does for `ChatService` and `DeploymentService`. |
| `temperature` | Double | 0.0 – 2.0 | Sampling randomness (0.0 = deterministic) |
| `topP` | Double | 0.0 – 1.0 | Nucleus sampling threshold |
| `frequencyPenalty` | Double | -2.0 – 2.0 | Discourage frequent tokens |
| `presencePenalty` | Double | -2.0 – 2.0 | Encourage new topics |
| `stop` | List\<String\> | Max 4 | Stop sequences to end generation |
| `seed` | Integer | Any | Random seed for reproducibility |
| `n` | Integer | ≥ 1 | Number of completions to generate |
| `logprobs` | Boolean | - | Return log probabilities |
| `topLogprobs` | Integer | ≥ 1 | Top token log probs (requires `logprobs=true`) |
| `logitBias` | Map\<String, Integer\> | - | Adjust token probabilities |
| `timeLimit` | Duration | Any | Maximum generation time |
| `toolChoiceOption` | ToolChoiceOption | AUTO, REQUIRED, NONE | Tool selection strategy |
| `toolChoice` | String | Tool name | Force a specific tool call |
| `responseFormat` | - | - | Use `responseAsText()`, `responseAsJson()`, `responseAsJsonSchema()` |
| `transactionId` | String | - | Request tracking ID |

#### Gateway-only

| Parameter | Type | Description |
|-----------|------|-------------|
| `serviceTier` | ServiceTier / String | Latency tier: `AUTO`, `DEFAULT`, `FLEX`, `PRIORITY` |
| `reasoningEffort` | ReasoningEffort / String | Reasoning budget for reasoning models: `LOW`, `MEDIUM`, `HIGH` |
| `parallelToolCalls` | Boolean | Enable or disable parallel function calls during tool use |
| `modalities` | List\<String\> | Requested output modalities, e.g. `["text"]`, `["text","audio"]` |
| `audio` | Map\<String, String\> | Audio output parameters |
| `metadata` | Map\<String, String\> | Developer-defined tags for filtering completions |
| `store` | Boolean | Store output for model distillation or evals |
| `prediction` | Prediction | Predicted-output configuration for generation speed-up |
| `streamOptions` | StreamOptions | Streaming options (auto-set when streaming is active) |
| `router` | Router | Routing and cache configuration |
| `user` | String | End-user identifier for abuse monitoring |

### Service Tier

Controls the latency and resource class for a request:

```java
ModelGatewayChatParameters.builder()
    .serviceTier(ServiceTier.AUTO)     // let the gateway choose
    .serviceTier(ServiceTier.FLEX)     // flexible, variable latency
    .serviceTier(ServiceTier.PRIORITY) // lower latency tier
    .build();
```

### Reasoning Effort

For reasoning models (e.g., `o3`, `o1`), controls how many internal reasoning steps the model uses:

```java
ModelGatewayChatParameters.builder()
    .reasoningEffort(ReasoningEffort.HIGH)
    .build();
```

Accepted values: `LOW`, `MEDIUM`, `HIGH`.

### Router and Caching

The `Router` record wraps a `Cache` configuration. Caching is only honored for **non-streaming** requests:

```java
ModelGatewayChatParameters.builder()
    .router(new Router(
        new Cache(
            true,    // enabled
            null,    // no filter
            0.95     // similarity threshold for a cache hit
        )
    ))
    .build();
```

When a cached response is returned, `ModelGatewayChatResponse.cached()` is `true`.

---

## Gateway Response

`ModelGatewayChatResponse` extends `TextChatResponse` (which itself extends `ChatResponse`) and adds three gateway-specific fields:

| Method | Type | Description |
|--------|------|-------------|
| `serviceTier()` | String | Tier actually used to serve the request |
| `systemFingerprint()` | String | Backend snapshot identifier - changes indicate a backend update that may affect determinism |
| `cached()` | Boolean | `true` if the response was served from the semantic cache |

```java
ModelGatewayChatResponse response = service.chat("Hello");

System.out.println("Content:      " + response.toAssistantMessage().content());
System.out.println("Service tier: " + response.serviceTier());
System.out.println("Fingerprint:  " + response.systemFingerprint());
System.out.println("Cached:       " + response.cached());
System.out.println("Total tokens: " + response.usage().totalTokens());
```

---

## Interceptors

Interceptors work identically to how they work in `ChatService`. See the [Chat Service - Interceptors](../../services/chat-service#interceptors) section for the full description of `MessageInterceptor`, `ToolInterceptor`, and `InterceptorContext`.

```java
ModelGatewayChatService service = ModelGatewayChatService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-4o")
    .messageInterceptor((ctx, message) -> message == null ? "" : message.strip())
    .toolInterceptor((ctx, functionCall) -> {
        var args = functionCall.arguments();
        return args != null && args.startsWith("\"")
            ? functionCall.withArguments(Json.fromJson(args, String.class))
            : functionCall;
    })
    .build();
```

---

## Related Resources

- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway)
- [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui)
- [Managing the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-managing-model)
- [Inferencing models through the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-inferencing-models-through-model)
- [Chat Service Documentation](../../services/chat-service)
- [Deployment Service Documentation](../../services/deployment-service)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/model-gateway)
