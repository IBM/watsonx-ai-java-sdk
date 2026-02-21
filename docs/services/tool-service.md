---
layout: default
title: Tool Service
parent: Services
nav_order: 10
permalink: /services/tool-service/
---

# Tool Service

The `ToolService` provides access to **IBM watsonx.ai Utility Agent Tools** — a set of server-side tools that can be invoked directly or integrated with `ChatService` for agentic workflows. Each built-in tool also implements the `ExecutableTool` interface, enabling automatic dispatch when a foundation model requests a tool call.

## Quick Start

```java
ToolService toolService = ToolService.builder()
    .apiKey(WATSONX_API_KEY)
    .baseUrl(CloudRegion.DALLAS)
    .build();

// Use a built-in tool directly
GoogleSearchTool googleSearch = new GoogleSearchTool(toolService);
List<GoogleSearchResult> results = googleSearch.search("watsonx.ai java sdk");

// Or run any tool by name using the generic API
String output = toolService.run(
    ToolRequest.structuredInput("Weather", Map.of("location", "Rome"))
);
```

---

## Overview

The `ToolService` enables you to:

- List all available utility tools and inspect their metadata, input schema, and config schema.
- Retrieve metadata for a specific tool by name.
- Execute any tool generically via `run()` using structured or unstructured input.
- Use built-in tool wrappers (`GoogleSearchTool`, `WeatherTool`, etc.) for type-safe invocations.
- Integrate built-in tools with `ChatService` via the `ExecutableTool` interface for agentic tool-call dispatch.

---

## Service Configuration

### Basic Setup

```java
ToolService toolService = ToolService.builder()
    .apiKey(WATSONX_API_KEY)
    .baseUrl("https://us-south.wx.ml.cloud.ibm.com") // or use CloudRegion
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service URL |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |

> Either `apiKey` or `authenticator` must be provided. No `projectId`, `spaceId`, or `modelId` is required.

---

## Service Methods

### List All Tools

```java
ToolService.Resources resources = toolService.getAll();

for (UtilityTool tool : resources.resources()) {
    System.out.println(tool.name() + " — " + tool.description());
}
```

### Get Tool by Name

```java
UtilityTool tool = toolService.getByName("GoogleSearch");

System.out.println(tool.name());
System.out.println(tool.description());
System.out.println(tool.agentDescription());  // system prompt guidance for LLM agents
System.out.println(tool.inputSchema());        // JSON schema for the input
System.out.println(tool.configSchema());       // JSON schema for config options
```

### Run a Tool Generically

```java
// Structured input — for tools with a defined input schema
String output = toolService.run(
    ToolRequest.structuredInput(
        "GoogleSearch",
        Map.of("q", "watsonx.ai"),          // input map
        Map.of("maxResults", 5)             // config map
    )
);

// Unstructured input — for tools that accept a plain string
String output = toolService.run(
    ToolRequest.unstructuredInput("RAGQuery", "What is this project about?")
);
```

The `run()` method always returns the `output` field from the tool's JSON response as a `String`.

---

## UtilityTool

The `UtilityTool` record represents the metadata returned by `getAll()` and `getByName()`:

| Field | Type | Description |
|-------|------|-------------|
| `name()` | String | Unique tool identifier used in requests |
| `description()` | String | Human-readable description of the tool |
| `agentDescription()` | String | System prompt guidance for LLM agents |
| `inputSchema()` | Map\<String, Object\> | JSON schema for the tool's input payload |
| `configSchema()` | Map\<String, Object\> | JSON schema for the tool's configuration options |

---

## Built-in Tools

All built-in tools implement the `ExecutableTool` interface, which exposes:

- `name()` — the tool schema name used by the LLM
- `schema()` — the `Tool` definition to pass to `ChatService`
- `execute(ToolArguments args)` — called automatically during tool-call dispatch

There are several built-in tools in watsonx.ai that can be used to add functionality to an LLM.

---

### GoogleSearchTool

Search for online trends, news, current events, real-time information, or research topics.

```java
GoogleSearchTool googleSearch = new GoogleSearchTool(toolService);

// Default: up to 10 results
List<GoogleSearchResult> results = googleSearch.search("watsonx.ai java sdk");

// Custom max results
List<GoogleSearchResult> results = googleSearch.search("watsonx.ai java sdk", 3);

for (GoogleSearchResult r : results) {
    System.out.println(r.title());
    System.out.println(r.description());
    System.out.println(r.url());
}
```

---

### TavilySearchTool

Search for online trends, news, current events, and research topics using the Tavily API. Requires a Tavily API key.

```java
TavilySearchTool tavilySearch = new TavilySearchTool(toolService, TAVILY_API_KEY);

List<TavilySearchResult> results = tavilySearch.search("watsonx.ai java sdk");
List<TavilySearchResult> results = tavilySearch.search("watsonx.ai java sdk", 5);

for (TavilySearchResult r : results) {
    System.out.println(r.title());
    System.out.println(r.url());
    System.out.println(r.content());
    System.out.println(r.score());   // relevance score (Double)
}
```

---

### WeatherTool

Fetch current weather information for a location.

```java
WeatherTool weather = new WeatherTool(toolService);

// Location only
String result = weather.find("Rome");

// Location and country
String result = weather.find("Naples", "Italy");

```

---

### WebCrawlerTool

Fetch and extract the text content of a specific web page. Use when you know the URL and need to read its content — not for discovering new URLs.

```java
WebCrawlerTool crawler = new WebCrawlerTool(toolService);
String content = crawler.process("https://github.com/IBM/watsonx-ai-java-sdk");
```

---

### WikipediaTool

Search Wikipedia and retrieve article summaries.

```java
WikipediaTool wikipedia = new WikipediaTool(toolService);
String result = wikipedia.search("watsonx.ai");
```

---

### PythonInterpreterTool

Execute Python code in a remote IBM watsonx.ai deployment runtime and return the console output. Requires a deployment ID for the Python interpreter.

```java
PythonInterpreterTool python = new PythonInterpreterTool(toolService, PYTHON_DEPLOYMENT_ID);
String result = python.run("print('Hello World!')");
// → Hello World!
```

---

### RAGQueryTool

Query one or more vector indexes using semantic similarity to retrieve relevant document passages. Requires a project ID or space ID and at least one vector index ID.

```java
RAGQueryTool ragQuery = RAGQueryTool.builder()
    .toolService(toolService)
    .projectId("my-project-id")
    .vectorIndexIds("index-1")
    .description("The document contains information about the watsonx.ai Java SDK.")
    .build();

String result = ragQuery.query("What is the Maven groupId?");
```

---

## Integration with ChatService

All built-in tools implement `ExecutableTool` and can be passed directly to `ChatService` to enable agentic tool-call dispatch. When the model requests a tool call, your code dispatches it and feeds the result back.

### Manual Dispatch

```java
GoogleSearchTool googleSearch = new GoogleSearchTool(toolService);
WebCrawlerTool webCrawler = new WebCrawlerTool(toolService);

var chatService = ChatService.builder()
    .baseUrl(CloudRegion.DALLAS)
    .apiKey(WATSONX_API_KEY)
    .projectId(PROJECT_ID)
    .modelId("ibm/granite-4-h-small")
    .tools(googleSearch.schema(), webCrawler.schema())
    .build();

List<ChatMessage> messages = new ArrayList<>();
messages.add(SystemMessage.of("You are a helpful assistant."));
messages.add(UserMessage.text("Is there a watsonx.ai Java SDK? If yes, give me the Maven groupId."));

var assistantMessage = chatService.chat(messages).toAssistantMessage();
messages.add(assistantMessage);

if (assistantMessage.hasToolCalls()) {
    var toolMessages = assistantMessage.processTools((toolName, toolArgs) ->
        switch (toolName) {
            case "google_search" -> googleSearch.search(toolArgs.get("query")).toString();
            case "webcrawler"    -> webCrawler.process(toolArgs.get("url"));
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
    );
    messages.addAll(toolMessages);
    assistantMessage = chatService.chat(messages).toAssistantMessage();
}

System.out.println(assistantMessage.content());
```

### Using ToolRegistry

`ToolRegistry` simplifies multi-tool dispatch by registering tools by name and optionally adding lifecycle hooks:

```java
RAGQueryTool ragQuery = RAGQueryTool.builder()
    .toolService(toolService)
    .projectId(PROJECT_ID)
    .vectorIndexIds(VECTOR_INDEX_ID)
    .description("Documents contain information about our internal project.")
    .build();

ToolRegistry registry = ToolRegistry.builder()
    .register(ragQuery)
    .build();

var chatService = ChatService.builder()
    ...
    .tools(registry.tools())
    .build();

// During tool-call dispatch:
var toolMessages = assistantMessage.processTools(registry::execute);
```

---

## Built-in Tools Summary

| Tool class | Schema name | Requires | Returns |
|------------|-------------|----------|---------|
| `GoogleSearchTool` | `google_search` | — | `List<GoogleSearchResult>` |
| `TavilySearchTool` | `tavily_search` | Tavily API key | `List<TavilySearchResult>` |
| `WeatherTool` | `weather` | — | `String` (plain text) |
| `WebCrawlerTool` | `webcrawler` | — | `String` (page content) |
| `WikipediaTool` | `wikipedia` | — | `String` (article summary) |
| `PythonInterpreterTool` | `python_interpreter` | Deployment ID | `String` (console output) |
| `RAGQueryTool` | `rag_query` | Project/space ID + vector index IDs | `String` (retrieved passages) |

---

## Related Resources

- [Utility Agent Tools API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#get-utility-agent-tools)
- [ChatService Documentation](./chat-service.md)
- [Sample code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/utility-tools)