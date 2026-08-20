# Model Gateway Embedding Example

This sample demonstrates how to generate text embeddings through the IBM watsonx.ai **Model Gateway** using the `ModelGatewayEmbeddingService`. A single IBM Cloud API key is used for every provider, and the gateway routes each request to the underlying embedding model.

## Overview

With this sample, you can:

- Configure `ModelGatewayEmbeddingParameters` (dimensions, encoding format)
- Embed multiple input texts in a single request
- Inspect the embedding vectors and token usage returned with the response

## Prerequisites

The Model Gateway component must already be installed and configured in your watsonx.ai instance, with at least one embedding model provider added. This is a one-time setup performed by an administrator, see [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui).

Before running the application, set the following environment variables or create a `.env` file in the project root:

| Variable            | Required | Description |
|---------------------|----------|-------------|
| `WATSONX_API_KEY`   | Yes      | Your watsonx.ai API key |
| `WATSONX_URL`       | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_MODEL_ID`  | Yes      | The gateway embedding model to use (for example `text-embedding-3-small`) |

Example (Linux/macOS):

```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_MODEL_ID=text-embedding-3-small
```

Example (Windows CMD):

```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_MODEL_ID=text-embedding-3-small
```

## How to Run

Use Maven to build and run the sample:

```bash
mvn package exec:java
```

## Notes

- The model ID must be one of the embedding models exposed by your gateway configuration. Use the `ModelGatewayCatalogService` to list what is available.
- The `FLOAT` encoding format returns the embedding vector as a JSON array of numbers. The `BASE64` format returns a compact binary payload that the SDK decodes automatically.

## References

- [Model Gateway Documentation](https://ibm.github.io/watsonx-ai-java-sdk/services/model-gateway/) - the SDK guide for all gateway services.
- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway) - product documentation.
