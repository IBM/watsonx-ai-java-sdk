# Watsonx Chatbot with reasoning Example

This is a simple chatbot project that uses streaming and reasoning.

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

| Variable                            | Required | Description |
|-------------------------------------|----------|-------------|
| `WATSONX_API_KEY`                   | Yes      | watsonx.ai API key |
| `WATSONX_URL`                       | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_GRANITE_3_3_DEPLOYMENT_ID` | Yes      | The deployment id of `ibm/granite-3-3-8b-instruct`. This model is no longer available in the standard models list, so it must be hosted through a deployment and referenced by its deployment id. |

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_GRANITE_3_3_DEPLOYMENT_ID=deployment-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_GRANITE_3_3_DEPLOYMENT_ID=deployment-id
```

## How to Run
Use Maven to run the application:

```bash
mvn package exec:java
```
