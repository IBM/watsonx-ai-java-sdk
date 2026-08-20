---
id: image-generation
title: Image Generation
---

# Model Gateway - Image Generation

The `ModelGatewayImageService` generates images from text prompts using any image model available through the **IBM watsonx.ai Model Gateway** (DALL-E 3, gpt-image-1, and others). Only providers that expose image models can be used here. To see what your gateway actually offers, ask `ModelGatewayCatalogService`, see [Catalog](./catalog/).

> **Setup required:** The Model Gateway must be installed and configured by an administrator before use. See [Model Gateway Prerequisites](/services/model-gateway#prerequisites).

## Quick Start

```java
ModelGatewayImageService service = ModelGatewayImageService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-image-1")
    .build();

ModelGatewayImageResponse response = service.generate("A futuristic city at sunset");
String b64 = response.data().get(0).b64Json();
```

---

## Service Configuration

### Basic Setup

```java
ModelGatewayImageService service = ModelGatewayImageService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .modelId("gpt-image-1")
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `baseUrl` | String / CloudRegion | Yes | watsonx.ai ML endpoint |
| `modelId` | String | Yes | Image model identifier (e.g., `"gpt-image-1"`) |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided.

### On-premises deployments

`apiKey` configures an IBM Cloud authenticator. On **IBM watsonx.ai software** (on-premises, CP4D) pass a `CP4DAuthenticator` through `authenticator` and use your instance URL as the `baseUrl`. The `CloudRegion` enum does not apply there. See [Authentication](/authentication#cp4d-authentication).

```java
ModelGatewayImageService service = ModelGatewayImageService.builder()
    .baseUrl("https://cpd.example.com")
    .authenticator(
        CP4DAuthenticator.builder()
            .url("https://cpd.example.com")
            .username(CP4D_USERNAME)
            .apiKey(CP4D_API_KEY)
            .build()
    )
    .modelId("gpt-image-1")
    .build();
```

---

## Generating Images

### From a Prompt String

```java
ModelGatewayImageResponse response = service.generate("A serene mountain landscape");
```

### With Parameters

Use `ModelGatewayImageParameters` to configure the optional request options:

```java
ModelGatewayImageParameters parameters = ModelGatewayImageParameters.builder()
    .n(1)
    .size(Size.SIZE_1024X1024)
    .quality(Quality.HIGH)
    .responseFormat(ResponseFormat.B64_JSON)
    .style(Style.VIVID)
    .outputFormat(OutputFormat.PNG)
    .background(Background.TRANSPARENT)
    .moderation(Moderation.LOW)
    .user("user-123")
    .build();

ModelGatewayImageResponse response = service.generate("A serene mountain landscape", parameters);
```

### With a Request Object

`ModelGatewayImageRequest` bundles the prompt and the parameters into a single value you can build once and reuse:

```java
ModelGatewayImageRequest request = ModelGatewayImageRequest.builder()
    .prompt("A serene mountain landscape")
    .parameters(parameters)
    .build();

ModelGatewayImageResponse response = service.generate(request);
```

### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `prompt` | String | Yes | Text description of the desired image. |
| `parameters` | ModelGatewayImageParameters | No | Optional request options |

### Image Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `background` | `Background` / String | `auto` | Background transparency: `transparent`, `opaque`, or `auto`. Transparency requires an `outputFormat` that supports it, so `png` or `webp` |
| `moderation` | `Moderation` / String | `auto` | Content moderation level: `low` for less restrictive filtering, or `auto` |
| `n` | Integer | 1 | Number of images to generate, from 1 to 10 |
| `outputCompression` | Integer | 100 | Compression level from 0 to 100, for the `webp` and `jpeg` formats only |
| `outputFormat` | `OutputFormat` / String | `jpeg` | File format: `png`, `jpeg`, `webp`, or `auto` |
| `partialImages` | Integer | 0 | Number of partial images streamed before the final result, from 0 to 3. With 0 the image arrives in a single event |
| `quality` | `Quality` / String | `auto` | Image quality: `auto`, `high`, `medium`, `low`, `hd`, or `standard` |
| `responseFormat` | `ResponseFormat` / String | `url` | Return format: `url` or `b64_json` |
| `size` | `Size` / String | `1024x1024` | Dimensions of the generated image |
| `style` | `Style` / String | `vivid` | Visual style: `vivid` for hyper-real and dramatic images, `natural` for more natural ones |
| `user` | String | | Unique identifier for the end-user, passed through to the upstream provider to help it detect abuse |

---

## Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `created()` | long | UNIX timestamp in seconds of when the model response was created |
| `data()` | `List<ImageData>` | Generated image objects, between 1 and 10 of them |
| `background()` | String | Background setting used, never `auto` |
| `outputFormat()` | String | Output format used, never `auto` |
| `quality()` | String | Quality level used, never `auto` |
| `size()` | String | Size used, never `auto` |
| `usage()` | `Usage` | Token usage, or `null` if not returned. On OpenAI only `gpt-image-1` reports it |

### `ImageData` Fields

| Field | Type | Description |
|-------|------|-------------|
| `url()` | String | Image URL, or `null` when `b64_json` format was requested. Unsupported by `gpt-image-1` |
| `b64Json()` | String | Base64-encoded image data, or `null` when `url` format was used |
| `revisedPrompt()` | String | Revised prompt, if the model modified it. On OpenAI only `dall-e-3` returns it |

The returned `data()` list is unmodifiable.

### Response formats

`responseFormat` decides which of the two `ImageData` fields is populated. With `b64_json` the image bytes travel inline and you decode them yourself:

```java
byte[] image = Base64.getDecoder().decode(response.data().get(0).b64Json());
Files.write(Path.of("image.png"), image);
```

With `url` the provider stores the image and returns a link to it. On OpenAI that link stays valid for 60 minutes after generation, so download the image before you need it again:

```java
String url = response.data().get(0).url();
```

The field you did not request comes back `null`, so it also tells you which format the response came back in. Not every model honours the setting: OpenAI supports `responseFormat` only on `dall-e-2` and `dall-e-3`, while `gpt-image-1` always returns Base64.

### `Usage` Fields

| Field | Type | Description |
|-------|------|-------------|
| `inputTokens()` | long | Tokens in the input prompt, images and text together |
| `outputTokens()` | long | Output tokens generated by the model |
| `totalTokens()` | long | Total tokens used |
| `inputTokensDetails()` | `InputTokensDetails` | Breakdown by token type |

### `InputTokensDetails` Fields

| Field | Type | Description |
|-------|------|-------------|
| `textTokens()` | long | Text tokens in the prompt |
| `imageTokens()` | long | Image tokens in the prompt |

---

## Related Resources

- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway)
- [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui)
- [Managing the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-managing-model)
- [Model Gateway Catalog Documentation](./catalog)
- [Model Gateway Chat Documentation](./chat)
- [Model Gateway Embedding Documentation](./embeddings)
- [Authentication](/authentication)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/model-gateway-image)
