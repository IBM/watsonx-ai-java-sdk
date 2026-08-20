# Model Gateway Image Example

This sample demonstrates how to generate images through the IBM watsonx.ai **Model Gateway** using the `ModelGatewayImageService`. A single IBM Cloud API key is used for every provider, and the gateway routes each request to the underlying image generation model.

## Overview

With this sample, you can:

- Configure `ModelGatewayImageParameters` (quality, size, number of images)
- Send a text prompt and receive generated images
- Inspect the response metadata (quality, size, output format, token usage)

## Prerequisites

The Model Gateway component must already be installed and configured in your watsonx.ai instance, with at least one image generation model provider added. This is a one-time setup performed by an administrator, see [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui).

Before running the application, set the following environment variables or create a `.env` file in the project root:

| Variable            | Required | Description |
|---------------------|----------|-------------|
| `WATSONX_API_KEY`   | Yes      | Your watsonx.ai API key |
| `WATSONX_URL`       | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_MODEL_ID`  | Yes      | The gateway image model to use (for example `gpt-image-1`) |

Example (Linux/macOS):

```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_MODEL_ID=gpt-image-1
```

Example (Windows CMD):

```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_MODEL_ID=gpt-image-1
```

## How to Run

Use Maven to build and run the sample:

```bash
mvn package exec:java
```

## Notes

- The model ID must be one of the image generation models exposed by your gateway configuration. Use the `ModelGatewayCatalogService` to list what is available.
- By default the response contains a URL pointing to the generated image. Set `responseFormat(ResponseFormat.B64_JSON)` in the parameters to receive the image as an inline Base64 payload instead.

## References

- [Model Gateway Documentation](https://ibm.github.io/watsonx-ai-java-sdk/services/model-gateway/) - the SDK guide for all gateway services.
- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway) - product documentation.
