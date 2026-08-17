# Model Gateway Example

This sample demonstrates how to chat with a third-party model (OpenAI, Anthropic, Mistral, and others) through the IBM watsonx.ai **Model Gateway** using the `ModelGatewayChatService`. A single IBM Cloud API key is used for every provider, and the gateway routes each request to the underlying model.

## Overview

With this sample, you can:

- Configure default `ModelGatewayChatParameters` (temperature, service tier, router cache) once on the service
- Override those parameters on a single request
- Send a user message to a gateway model and print the answer
- Inspect the gateway metadata returned with the response (resolved model, service tier, system fingerprint, cache hit, token usage)

## Prerequisites

The Model Gateway component must already be installed and configured in your watsonx.ai instance, with at least one model provider and one model added. This is a one-time setup performed by an administrator, see [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui).

Before running the application, set the following environment variables or create a `.env` file in the project root:

| Variable            | Required | Description |
|---------------------|----------|-------------|
| `WATSONX_API_KEY`   | Yes      | Your watsonx.ai API key |
| `WATSONX_URL`       | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_MODEL_ID`  | Yes      | The gateway model to use, in `provider/model` form (for example `openai/gpt-4o-mini`) or the alias configured by your administrator |

Example (Linux/macOS):

```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_MODEL_ID=openai/gpt-4o-mini
```

Example (Windows CMD):

```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_MODEL_ID=openai/gpt-4o-mini
```

## How to Run

Use Maven to build and run the sample:

```bash
mvn package exec:java
```

## Notes

- The model ID must be one of the models exposed by your gateway configuration. Use the `ModelGatewayCatalogService` to list what is available.
- `cached()` is `true` only when the gateway serves the response from its cache. This sample disables caching in the default parameters, so the first run reports `false`.

## References

- [Model Gateway Documentation](https://ibm.github.io/watsonx-ai-java-sdk/services/model-gateway/) - the SDK guide for all gateway services.
- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway) - product documentation.
