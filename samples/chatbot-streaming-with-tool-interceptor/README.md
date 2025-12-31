# Watsonx Chatbot with Tool Interceptor

This sample demonstrates how to use the **Tool Interceptor** feature to handle malformed tool call arguments during streaming chat sessions.

## Overview

Some models, like `ibm/granite-4-h-small`, may occasionally generate invalid JSON when making tool calls during streaming. This sample shows how to use a `toolInterceptor` to automatically detect and fix malformed JSON arguments by leveraging another LLM call.

### Key Features

- **Streaming chat** with tool calling support
- **Tool Interceptor** that validates and sanitizes malformed JSON
- **Automatic recovery** from invalid tool call arguments using an LLM
- **Conversation context** preservation for accurate JSON fixing

## How It Works

1. The chatbot uses `granite-4-h-small` for the main conversation
2. When a tool call is made, the `toolInterceptor` validates the JSON arguments
3. If the JSON is invalid:
   - The interceptor extracts the conversation context
   - Makes a separate LLM call to fix the malformed JSON
   - Returns the sanitized tool call arguments
4. The fixed tool call is then executed normally

### Tool Interceptor Implementation

```java
.toolInterceptor((ctx, fc) -> 
    Json.isValidObject(fc.arguments()) 
        ? fc 
        : sanitize(ctx, fc.name(), fc.arguments())
)
```

The `sanitize` method:
- Analyzes the conversation history to understand user intent
- Uses the tool's schema as a reference
- Calls an LLM to generate valid JSON matching the schema
- Returns a corrected `FunctionCall` object

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