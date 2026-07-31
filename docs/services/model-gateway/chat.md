---
layout: default
title: Chat
parent: Model Gateway
nav_order: 1
permalink: /services/model-gateway/chat/
---

# Model Gateway - Chat

The `ModelGatewayService` lets you send chat completions to any third-party model (OpenAI, Anthropic, and others) available through the IBM watsonx.ai Model Gateway.

> **Setup required:** The Model Gateway must be installed and configured by an administrator before use. See [Model Gateway Prerequisites](../#prerequisites).

## Quick Start

```java
ModelGatewayService service = ModelGatewayService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-4o")
    .build();

GatewayChatResponse response = service.chat("What is the capital of Italy?");
System.out.println(response.toAssistantMessage().content());
// → Rome is the capital of Italy.
```

---

## Overview

`ModelGatewayService` enables you to:

- Send synchronous and streaming chat requests to any model available through the gateway.
- Use gateway-specific parameters such as service tier, reasoning effort, audio modalities, caching, and routing configuration.
- Apply `MessageInterceptor` and `ToolInterceptor` for post-processing.
- Read gateway metadata on every response: `serviceTier()`, `systemFingerprint()`, and `cached()`.

---

## Service Configuration

### Basic Setup

```java
ModelGatewayService service = ModelGatewayService.builder()
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
| `parameters` | BaseChatParameters | No | Default parameters applied to every request (accepts `ModelGatewayParameters` or `ChatParameters`) |
| `tools` | List\<Tool\> | No | Default tools available to the model |
| `messageInterceptor` | MessageInterceptor | No | Post-processing hook for the assistant's text content |
| `toolInterceptor` | ToolInterceptor | No | Post-processing hook for function call arguments |
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
ModelGatewayParameters defaults = ModelGatewayParameters.builder()
    .temperature(0.7)
    .maxCompletionTokens(1000)
    .serviceTier(ModelGatewayParameters.ServiceTier.AUTO)
    .build();

ModelGatewayService service = ModelGatewayService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-4o")
    .parameters(defaults)
    .build();
```

Per-request parameters **always take precedence** over service-level defaults. Fields not set on the per-request parameters fall back to the defaults.

---

## Chat

### Simple Chat

```java
GatewayChatResponse response = service.chat("Tell me a joke");
System.out.println(response.toAssistantMessage().content());
```

### Multi-Turn Conversation

```java
var messages = new ArrayList<ChatMessage>();
messages.add(SystemMessage.of("You are a helpful assistant"));
messages.add(UserMessage.text("What is the capital of France?"));

GatewayChatResponse response = service.chat(messages);
messages.add(response.toAssistantMessage());

messages.add(UserMessage.text("What is its population?"));
response = service.chat(messages);
System.out.println(response.toAssistantMessage().content());
```

### With Parameters

```java
ModelGatewayParameters parameters = ModelGatewayParameters.builder()
    .temperature(0.3)
    .maxCompletionTokens(200)
    .build();

GatewayChatResponse response = service.chat(messages, parameters);
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

GatewayChatResponse response = service.chat(messages, parameters, List.of(weatherTool));
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

`ModelGatewayParameters` extends the common `BaseChatParameters` with fields specific to the Model Gateway.

### Builder Reference

#### Inherited from BaseChatParameters

| Parameter | Type | Range | Description |
|-----------|------|-------|-------------|
| `modelId` | String | - | Override the model for this request |
| `maxCompletionTokens` | Integer | ≥ 0 | Maximum tokens in the response |
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
ModelGatewayParameters.builder()
    .serviceTier(ModelGatewayParameters.ServiceTier.AUTO)     // let the gateway choose
    .serviceTier(ModelGatewayParameters.ServiceTier.FLEX)     // flexible, variable latency
    .serviceTier(ModelGatewayParameters.ServiceTier.PRIORITY) // lower latency tier
    .build();
```

### Reasoning Effort

For reasoning models (e.g., `o3`, `o1`), controls how many internal reasoning steps the model uses:

```java
ModelGatewayParameters.builder()
    .reasoningEffort(ModelGatewayParameters.ReasoningEffort.HIGH)
    .build();
```

Accepted values: `LOW`, `MEDIUM`, `HIGH`.

### Router and Caching

The `Router` record wraps a `Cache` configuration. Caching is only honored for **non-streaming** requests:

```java
ModelGatewayParameters.builder()
    .router(new ModelGatewayParameters.Router(
        new ModelGatewayParameters.Cache(
            true,    // enabled
            null,    // no filter
            0.95     // similarity threshold for a cache hit
        )
    ))
    .build();
```

When a cached response is returned, `GatewayChatResponse.cached()` is `true`.

---

## Gateway Response

`GatewayChatResponse` extends `TextChatResponse` (which itself extends `ChatResponse`) and adds three gateway-specific fields:

| Method | Type | Description |
|--------|------|-------------|
| `serviceTier()` | String | Tier actually used to serve the request |
| `systemFingerprint()` | String | Backend snapshot identifier - changes indicate a backend update that may affect determinism |
| `cached()` | Boolean | `true` if the response was served from the semantic cache |

```java
GatewayChatResponse response = service.chat("Hello");

System.out.println("Content:      " + response.toAssistantMessage().content());
System.out.println("Service tier: " + response.serviceTier());
System.out.println("Fingerprint:  " + response.systemFingerprint());
System.out.println("Cached:       " + response.cached());
System.out.println("Total tokens: " + response.usage().totalTokens());
```

---

## ChatProvider Interface

`ModelGatewayService` implements both `GatewayChatProvider` (gateway-typed) and the common `ChatProvider` interface. Assign to `ChatProvider` when you want to write code that works with both `ChatService` and `ModelGatewayService` interchangeably:

```java
// Assign to GatewayChatProvider for gateway-typed responses
GatewayChatProvider gateway = ModelGatewayService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-4o")
    .build();

GatewayChatResponse response = gateway.chat(ChatRequest.builder()
    .messages(UserMessage.text("Hello"))
    .build());

// Assign to the common interface when the caller doesn't care about gateway specifics
ChatProvider provider = gateway;
ChatResponse genericResponse = provider.chat(ChatRequest.builder()
    .messages(UserMessage.text("Hello"))
    .build());
```

---

## Interceptors

Interceptors work identically to how they work in `ChatService` - see the [Chat Service - Interceptors](../../chat-service#interceptors) section for the full description of `MessageInterceptor`, `ToolInterceptor`, and `InterceptorContext`.

```java
ModelGatewayService service = ModelGatewayService.builder()
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

## Watsonx-native Fields are Rejected

`ModelGatewayParameters` intentionally omits fields that only make sense for the native watsonx.ai endpoint - `projectId`, `spaceId`, `crypto`, `guidedChoice`, `guidedRegex`, `guidedGrammar`, `repetitionPenalty`, `lengthPenalty`, and `context`. If you inadvertently pass a `ChatParameters` instance that has any of those fields set, the SDK throws `IllegalArgumentException` at call time rather than silently ignoring the values:

```
IllegalArgumentException: The following watsonx-native parameter(s) set on the request parameters
are not supported by the Model Gateway: [projectId]. Remove them or use ModelGatewayParameters for gateway requests.
```

---

## Model Gateway Administration

Once the gateway is running, administrators can manage it through the UI or the REST API. The operations below require the **Administrator platform** role or **Manage configurations** permission.

> Reference: [Managing the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-managing-model)

### Managing connections and models

To edit or delete an existing provider connection:

1. Open **Administration > Model Gateway > Model provider** tab.
2. Locate the connection with the search field.
3. Click the **Edit** icon to update credentials or models, or click the **Delete** icon to remove it.

> **Note:** If a provider was created programmatically, its associated secret is not populated automatically. You must manually select the secret. Secrets created via API follow the format `mg-<connection-name>-<six-random-chars>`.

### Access policies

By default the gateway can reach all configured providers. Use access policies to scope providers, models, and load balancers to specific user groups.

**Via UI:**

1. Open the **Access control** tab and click **Assign access**.
2. Select an access group.
3. Choose **Model** or **Load balancer** as the resource type, then select the resource.
4. Set the action and permission type, then click **Create**.

Access policies cannot be edited. To change a policy, delete it and recreate it.

**Via API:**

```bash
# Grant read access to a model
curl -X POST "https://cpd-<namespace>.apps.<ocp-domain>/ml/gateway/v1/policies" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "subject": "<group_id>",
    "resource": "model:<uuid>",
    "action": "read",
    "effect": "allow"
  }'

# List all policies
curl -H "Authorization: Bearer ${TOKEN}" \
  "https://cpd-<namespace>.apps.<ocp-domain>/ml/gateway/v1/policies"
```

### Load balancing

Load balancers distribute inference requests across multiple model backends under a single stable alias - useful for high-traffic scenarios.

**Via UI:**

1. Open the **Rules > Load balancer** tab and click **Create load balancer**.
2. Type a name and select a balancer type:

| Type | Behaviour |
|------|-----------|
| `round_robin` | Distributes requests evenly across all backends in sequence |
| `least_connections` | Routes to the backend with the fewest active connections |
| `weighted_round_robin` | Distributes based on per-backend weights |
| `quota_priority` | Routes based on quota limits and priority levels |

3. Click **Select models**, choose the backends, optionally set type-specific parameters, and click **Create**.

**Via API:**

```bash
# List model UUIDs
curl -H "Authorization: Bearer ${TOKEN}" \
  "https://cpd-<namespace>.apps.<ocp-domain>/ml/gateway/v1/models" | jq '.data[] | {id, uuid}'

# Create a round-robin load balancer
curl -X POST "https://cpd-<namespace>.apps.<ocp-domain>/ml/gateway/v1/load-balancers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "name": "primary-router",
    "alias": "chat-balanced",
    "algorithm": "round_robin",
    "backends": [
      {"model_uuid": "11111111-1111-1111-1111-111111111111"},
      {"model_uuid": "22222222-2222-2222-2222-222222222222"}
    ]
  }'
```

Use the alias as the `modelId` in `ModelGatewayService` to transparently distribute load:

```java
ModelGatewayService service = ModelGatewayService.builder()
    .baseUrl("https://cpd-<namespace>.apps.<ocp-domain>")
    .apiKey(WATSONX_API_KEY)
    .modelId("chat-balanced")  // alias of the load balancer
    .build();
```

### Rate limits

Rate limits prevent excessive workloads from exhausting shared capacity. They can be scoped to a tenant, a provider, or a specific model.

**Via UI:**

1. Open the **Rules > Rate limit** tab and click **Create rate limit**.
2. Select the scope: **Model**, **Provider**, or **Tenant**.
3. Select the target resource and click **Next**.
4. Configure request and/or token limits (at least one must be enabled with a rate > 0), then click **Create**.

Rate limit configuration fields:

| Field | Description |
|-------|-------------|
| Request rate | Number of requests (or tokens) allowed per duration |
| Capacity | Maximum burst size (token bucket algorithm) |
| Duration | Time window for the rate (`1m`, `1h`, `1d`) |

**Via API:**

```bash
# Tenant-wide limit: 10 requests/minute, burst up to 60
curl -X POST "https://cpd-<namespace>.apps.<ocp-domain>/ml/gateway/v1/rate-limits" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"type": "tenant", "request": {"capacity": 60, "amount": 10, "duration": "1m"}}'

# Model-scoped limit
curl -X POST "https://cpd-<namespace>.apps.<ocp-domain>/ml/gateway/v1/rate-limits" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"type": "model", "model_uuid": "<uuid>", "request": {"capacity": 15, "amount": 3, "duration": "1m"}}'
```

### Usage monitoring

Monitor token and request consumption across the tenant or per user. Usage monitoring is available via the REST API only.

```bash
# Tenant usage, grouped by model, for a specific day
curl -X GET "https://cpd-<namespace>.apps.<ocp-domain>/ml/v1/usage\
?start_time=1704067200&end_time=1704153600&bucket_width=1h&group_by=model&limit=25" \
  -H "Authorization: Bearer ${TOKEN}"

# Per-user usage
curl -X GET "https://cpd-<namespace>.apps.<ocp-domain>/ml/v1/usage/user\
?start_time=1704067200&end_time=1704153600&bucket_width=1h&group_by=model&limit=25" \
  -H "Authorization: Bearer ${TOKEN}"
```

Key query parameters:

| Parameter | Description |
|-----------|-------------|
| `start_time` / `end_time` | Unix timestamps |
| `bucket_width` | Aggregation interval: `1m`, `1h`, `1d` (default: `1d`) |
| `group_by` | One or more of: `model`, `user_uuid`, `service_provider`, `usage_type`, `load_balancer_alias` |
| `limit` | Max result buckets (1-100, default: 10) |
| `usage_types` | Filter by type: `completion`, `embedding`, `moderation`, `image` |

---

## Related Resources

- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway)
- [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui)
- [Managing the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-managing-model)
- [Inferencing models through the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-inferencing-models-through-model)
- [Chat Service Documentation](../../chat-service)
- [Deployment Service Documentation](../../deployment-service)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/model-gateway)
