# Watsonx Chatbot with reasoning Example

This is a simple chatbot project.

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

| Variable              | Required | Description |
|-----------------------|----------|-------------|
| `WATSONX_API_KEY`     | Yes      | watsonx.ai API key |
| `WATSONX_URL`         | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_PROJECT_ID`  | Yes      | watsonx.ai project id |

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_PROJECT_ID=project-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_PROJECT_ID=project-id
```

## How to Run
Use Maven to run the application:

```bash
mvn package exec:java
```
