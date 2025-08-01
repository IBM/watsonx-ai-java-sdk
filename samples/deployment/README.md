# Watsonx Deployment Sample

This sample demonstrates how interact with a LLM deployment via the `DeploymentService`. It retrieves deployment metadata and performs a chat completion using the deployed model.

## Overview

With this sample, you can:

- Connect to a deployed model on Watsonx.ai
- Retrieve deployment metadata (name, type, status)
- Send a user message to the deployed model and get the response

This is useful when you've already deployed a foundation model or prompt-tuned model via the watsonx.ai catalog and want to use it directly.

## Prerequisites

Before running the application, set the following environment variables or create a `.env` file in the project root:

- `WATSONX_API_KEY` – Your Watsonx.ai API key
- `WATSONX_URL` – The base URL for the Watsonx.ai service
- `WATSONX_DEPLOYMENT` – The ID of the deployment you want to invoke
- `WATSONX_SPACE_ID` – The space ID that contains the deployment

### Example (Linux/macOS):

```bash
export WATSONX_API_KEY=your-api-key
export WATSONX_URL=https://your-watsonx-url
export WATSONX_DEPLOYMENT=your-deployment-id
export WATSONX_SPACE_ID=your-space-id
```

### Example (Windows CMD):

```cmd
set WATSONX_API_KEY=your-api-key
set WATSONX_URL=https://your-watsonx-url
set WATSONX_DEPLOYMENT=your-deployment-id
set WATSONX_SPACE_ID=your-space-id
```

## How to Run

Use Maven to build and run the sample:

```bash
mvn package exec:java
```

## Notes

- This sample assumes that the deployment is already created via the watsonx.ai user interface.
- The deployed model must support chat completions.

## References

- [Deploy on Demand Overview](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/deploy-on-demand-overview.html?context=wx) – Overview of the on-demand deployment flow for foundation models in watsonx.ai.
- [Foundation Models in watsonx.ai](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx#dod) – Information about available foundation models and their usage.