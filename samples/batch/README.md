# Watsonx Batch Example

This sample demonstrates how to use IBM watsonx.ai to submit and execute batch inference jobs using the `BatchService`. It sends multiple chat completion requests in a single batch and waits for the results.

## Overview

With this sample, you can:

- Build a JSONL payload with multiple inference requests
- Upload the payload and submit a batch job via `BatchService`
- Wait for the batch job to complete and retrieve the results
- Print each result mapped to its `custom_id`

## Prerequisites

Before running the application, set the following environment variables or create a `.env` file in the project root:

| Variable              | Required | Description |
|-----------------------|----------|-------------|
| `WATSONX_API_KEY`     | Yes      | Your watsonx.ai API key |
| `WATSONX_URL`         | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_PROJECT_ID`  | Yes      | Your watsonx.ai project ID |

### Example (Linux/macOS):

```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_PROJECT_ID=project-id
```

### Example (Windows CMD):

```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_PROJECT_ID=project-id
```

## How to Run

Use Maven to build and run the sample:

```bash
mvn package exec:java
```