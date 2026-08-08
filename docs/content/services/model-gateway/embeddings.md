---
id: embeddings
title: Embeddings
---

# Model Gateway - Embeddings

The `ModelGatewayEmbeddingService` generates vector embeddings from text using any third-party embedding model available through the **IBM watsonx.ai Model Gateway** (OpenAI, Azure OpenAI, Mistral, and others). Only providers that expose embedding models can be used here - `ModelGatewayCatalogService` lists what your gateway actually offers, see [Catalog](./catalog/).

> **Setup required:** The Model Gateway must be installed and configured by an administrator before use. See [Model Gateway Prerequisites](/services/model-gateway#prerequisites).

## Quick Start

```java
ModelGatewayEmbeddingService service = ModelGatewayEmbeddingService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("text-embedding-3-small")
    .build();

ModelGatewayEmbeddingResponse response = service.embed("Hello, world!");

List<Float> vector = response.data().get(0).embedding();
// → [0.0023064255, -0.009327292, -0.0028842222, ...]
```

---

## Service Configuration

### Basic Setup

```java
ModelGatewayEmbeddingService service = ModelGatewayEmbeddingService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("text-embedding-3-small")
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `baseUrl` | String / CloudRegion | Yes | watsonx.ai ML endpoint |
| `modelId` | String | Yes | Embedding model identifier (e.g., `"text-embedding-3-small"`) |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided.

### On-premises deployments

`apiKey` configures an IBM Cloud authenticator. On **IBM watsonx.ai software** (on-premises, CP4D) pass a `CP4DAuthenticator` through `authenticator` and use your instance URL as the `baseUrl` - the `CloudRegion` enum does not apply. See [Authentication](/authentication#cp4d-authentication).

```java
ModelGatewayEmbeddingService service = ModelGatewayEmbeddingService.builder()
    .baseUrl("https://cpd.example.com")
    .authenticator(
        CP4DAuthenticator.builder()
            .url("https://cpd.example.com")
            .username(CP4D_USERNAME)
            .apiKey(CP4D_API_KEY)
            .build()
    )
    .modelId("text-embedding-3-small")
    .build();
```

---

## Generating Embeddings

### Single Input

```java
ModelGatewayEmbeddingResponse response = service.embed("Hello, world!");
```

### Multiple Inputs

```java
// varargs
ModelGatewayEmbeddingResponse response = service.embed("Hello", "World", "Goodbye");

// List<String>
ModelGatewayEmbeddingResponse response = service.embed(List.of("Hello", "World"));
```

### With Parameters

Use `ModelGatewayEmbeddingParameters` to configure the optional request options:

```java
ModelGatewayEmbeddingParameters parameters = ModelGatewayEmbeddingParameters.builder()
    .dimensions(512)
    .encodingFormat(EncodingFormat.FLOAT)
    .user("user-123")
    .build();

ModelGatewayEmbeddingResponse response = service.embed(List.of("Hello, world!"), parameters);
```

### With a Request Object

`ModelGatewayEmbeddingRequest` bundles the inputs and the parameters into a single value you can build once and reuse:

```java
ModelGatewayEmbeddingRequest request = ModelGatewayEmbeddingRequest.builder()
    .input("Hello, world!")
    .parameters(parameters)
    .build();

ModelGatewayEmbeddingResponse response = service.embed(request);
```

### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `input` | String... / List\<String\> | Yes | Text to embed (at least one input is required) |
| `parameters` | ModelGatewayEmbeddingParameters | No | Optional request options |

### Embedding Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `dimensions` | Integer | Number of dimensions in the output embedding vector |
| `encodingFormat` | `EncodingFormat` / String | Format of the returned embeddings (default: `FLOAT`) |
| `user` | String | Unique identifier for the end-user (passed through to the upstream provider) |

The `encodingFormat` parameter accepts either the `ModelGatewayEmbeddingParameters.EncodingFormat` enum (preferred) or a raw string:

```java
// preferred — using the enum
.encodingFormat(EncodingFormat.FLOAT)   // → "float"
.encodingFormat(EncodingFormat.BASE64)  // → "base64"

// also accepted — raw string
.encodingFormat("float")
.encodingFormat("base64")
```

| Enum constant | String value | Wire representation |
|---------------|-------------|---------------------|
| `EncodingFormat.FLOAT` | `"float"` | A JSON array of numbers |
| `EncodingFormat.BASE64` | `"base64"` | A Base64 string encoding the same vector as binary `float32` values |

Either way `embedding()` returns a `List<Float>`, so the format you request never changes the code that reads the vector - see [Encoding formats](#encoding-formats).

---

## Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `object()` | String | Always `"list"` |
| `model()` | String | The model used to generate the embeddings |
| `data()` | List\<Embedding\> | The list of generated embedding objects |
| `usage()` | Usage | Token usage information |

### `Embedding` Fields

| Field | Type | Description |
|-------|------|-------------|
| `object()` | String | Always `"embedding"` |
| `index()` | int | Position of this embedding in the input list |
| `embedding()` | List\<Float\> | The embedding vector, one `Float` per dimension |
| `base64()` | String | The raw Base64 payload, or `null` when the `"float"` format was used |

### Encoding formats

`embedding()` always returns a `List<Float>`, whichever `encodingFormat` you requested. With `"base64"` the SDK decodes the payload for you, so the same code reads the vector in both cases:

```java
List<Float> vector = response.data().get(0).embedding();
```

The two formats differ only in what travels over the wire - `"base64"` sends the vector as binary `float32` values instead of JSON numbers, which makes the response smaller. When you need the untouched payload, for example to forward it to another service, `base64()` gives you the original string:

```java
String base64 = response.data().get(0).base64();
```

`base64()` is `null` for the `"float"` format, so it also tells you which format the response came back in.

The returned vector is unmodifiable.

### `Usage` Fields

| Field | Type | Description |
|-------|------|-------------|
| `promptTokens()` | int | Number of tokens in the input |
| `totalTokens()` | int | Total tokens used |

---

## Related Resources

- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway)
- [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui)
- [Managing the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-managing-model)
- [Model Gateway Catalog Documentation](./catalog)
- [Model Gateway Chat Documentation](./chat)
- [Embedding Service Documentation](../../services/embedding-service)
- [Authentication](/authentication)
