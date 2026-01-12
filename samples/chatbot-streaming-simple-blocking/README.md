# Watsonx Streaming Chatbot - Simple Blocking Approach

This sample demonstrates how to build a streaming chatbot using a **simplified blocking approach** with the `Consumer<String>` handler and default tools configuration.

## Overview

This example showcases a pragmatic approach to handling streaming responses in Java applications:

1. **Simplified Streaming Handler**: Uses `Consumer<String>` for cleaner code when you only need the text content
2. **Blocking Thread Pattern**: Uses `.join()` to block the thread while waiting for streaming completion

## How It Works

### 1. Configure the Chat Service

The chatbot is configured with a default tool (`get_current_time`) in the builder:

```java
ChatService chatService = ChatService.builder()
    .modelId("ibm/granite-4-h-small")
    .baseUrl(config.getValue("WATSONX_URL", String.class))
    .apiKey(config.getValue("WATSONX_API_KEY", String.class))
    .projectId(config.getValue("WATSONX_PROJECT_ID", String.class))
    .tools(Tool.of("get_current_time", "Get the current time"))
    ...
    .build();
```

### 2. Stream Responses with Blocking

When streaming responses, the simplified handler receives only the text, and we block until completion:

```java
var assistantMessage = chatService.chatStreaming(messages, System.out::print)
    .join()  // Block until streaming completes
    .toAssistantMessage();
```

### 3. Handle Tool Calls

If the model decides to call the `get_current_time` tool, it's automatically executed and the conversation continues:

```java
if (assistantMessage.hasToolCalls()) {
    var toolMessage = assistantMessage.processTools((toolName, toolArgs) -> LocalTime.now());
    messages.addAll(toolMessage);
    assistantMessage = chatService.chatStreaming(messages, System.out::print)
        .join() // Block until streaming completes
        .toAssistantMessage();
    messages.add(assistantMessage);
}
```

## When to Use This Pattern

This blocking streaming approach is ideal for:

- **CLI applications**: Where sequential interaction is natural
- **Simple chatbots**: Where code simplicity is more important than maximum concurrency
- **Java 21+ applications**: Where virtual threads make blocking efficient

For high-concurrency web applications or when running on Java versions before 21, consider using fully asynchronous patterns with `CompletableFuture` composition instead.

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