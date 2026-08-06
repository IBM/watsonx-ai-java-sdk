---
id: catalog
title: Catalog
---

# Model Gateway - Catalog

The `ModelGatewayCatalogService` provides read-only access to the list of models configured in the **IBM watsonx.ai Model Gateway**.

## Quick Start

```java
ModelGatewayCatalogService catalog = ModelGatewayCatalogService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .build();

List<ModelGatewayModel> models = catalog.listModels();
models.forEach(m -> System.out.println(m.id() + " (" + m.ownedBy() + ")"));
// → gpt-4o (openai)
// → claude-3-5-sonnet (anthropic)
```

---

## Service Configuration

### Basic Setup

```java
ModelGatewayCatalogService catalog = ModelGatewayCatalogService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `baseUrl` | String / CloudRegion | Yes | watsonx.ai ML endpoint |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided.

---

## Operations

### listModels

Returns all models configured across all providers in the gateway.

```java
List<ModelGatewayModel> models = catalog.listModels();

for (ModelGatewayModel model : models) {
    System.out.println("UUID:     " + model.uuid());
    System.out.println("ID:       " + model.id());
    System.out.println("Alias:    " + model.optionalAlias().orElse("(none)"));
    System.out.println("Provider: " + model.ownedBy());
    System.out.println();
}
```

### getModel

Retrieves a single model by its **UUID or alias**. Both are accepted identifiers.

```java
// By alias
ModelGatewayModel model = catalog.getModel("gpt-4o");

// By UUID
ModelGatewayModel model = catalog.getModel("123e4567-e89b-12d3-a456-426614174000");

System.out.println("ID:       " + model.id());
System.out.println("Provider: " + model.ownedBy());
System.out.println("Created:  " + model.created());
model.optionalMetadata().ifPresent(meta -> {
    System.out.println("Family:   " + meta.modelFamily());
    System.out.println("Region:   " + meta.region());
    System.out.println("Cost/1k:  " + meta.cost());
});
```

---

## ModelGatewayModel

Each model returned by the catalog exposes the following fields.

### Core Fields

| Method | Type | Description |
|--------|------|-------------|
| `uuid()` | String | Unique identifier assigned by the gateway |
| `object()` | String | Always `"model"` |
| `created()` | Long | Unix timestamp (seconds) when this configuration was created |
| `ownedBy()` | String | Provider in the format `<type>:<name>` (e.g., `"openai:my-provider"`) |
| `id()` | String | Official provider-side model identifier (e.g., `"gpt-4o-2024-11-20"`) |
| `alias()` | String | Optional friendly name - use this with `getModel()` instead of the full id |
| `description()` | String | Optional user-defined description |
| `metadata()` | ModelGatewayModel.Metadata | Optional additional configuration - may be `null` |

### ModelGatewayModel.Metadata

| Method | Type | Description |
|--------|------|-------------|
| `cost()` | Double | Cost per 1 000 tokens in USD |
| `modelFamily()` | String | Model series (e.g., `"gpt-4"`, `"claude-3"`) |
| `recommenderLabel()` | String | Label used by the Recommender API |
| `region()` | String | Deployment region (e.g., `"us-east-1"`) |
| `batch()` | Boolean | Whether the model is preferred for batch requests |
| `contextWindow()` | Integer | Maximum tokens the model can process in a single request |

All `Metadata` fields may be `null` when not set by the administrator.

---

## Related Resources

- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway)
- [Managing the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-managing-model)
- [Model Gateway Chat Documentation](./chat)
