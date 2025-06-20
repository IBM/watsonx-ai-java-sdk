# Watsonx Embedding Example

This is a simple example that demonstrates how to use IBM watsonx.ai to execute rerank.

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

- `WATSONX_API_KEY` – Your watsonx.ai API key
- `WATSONX_URL` – The base URL for the watsonx.ai service
- `WATSONX_PROJECT_ID` – Your watsonx.ai project id

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=your-api-key
export WATSONX_URL=https://your-watsonx-url
export WATSONX_PROJECT_ID=your-project-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=your-api-key
set WATSONX_URL=https://your-watsonx-url
set WATSONX_PROJECT_ID=your-project-id
```
## How to Run
Use Maven to run the application. 
```bash
mvn clean package exec:java 
```