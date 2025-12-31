# Watsonx Chatbot with Tool Registry Example

This example demonstrates how to use the `ToolRegistry` to manage and execute tools in a chatbot application. The registry provides a clean, type-safe way to register multiple tools and handle their execution with optional lifecycle callbacks.

## Features

- **Tool Registry Pattern**: Centralized tool management using `ToolRegistry`
- **Built-in Tools**: Uses Google Search and Web Crawler tools
- **Lifecycle Callbacks**: Demonstrates the use of `beforeExecution` callback for logging

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

| Variable              | Required | Description |
|-----------------------|----------|-------------|
| `WATSONX_API_KEY`     | Yes      | watsonx.ai API key |
| `WATSONX_URL`         | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_WX_URL`      | Yes      | The base URL for the watsonx.ai wx service (for utility tools) |
| `WATSONX_PROJECT_ID`  | Yes      | watsonx.ai project id |

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://us-south.ml.cloud.ibm.com
export WATSONX_WX_URL=https://api.dataplatform.cloud.ibm.com/wx
export WATSONX_PROJECT_ID=project-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://us-south.ml.cloud.ibm.com
set WATSONX_WX_URL=https://api.dataplatform.cloud.ibm.com/wx
set WATSONX_PROJECT_ID=project-id
```

## How to Run

Use Maven to run the application:

```bash
mvn package exec:java
```