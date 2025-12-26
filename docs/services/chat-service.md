---
layout: default
title: Chat Service
parent: Services
nav_order: 1
permalink: /services/chat-service/
---

# Chat Service

The `ChatService` provides functionality to interact with **IBM watsonx.ai foundation models** for conversational AI applications. It supports synchronous and streaming chat completions, tool calling, reasoning, and structured outputs.

## Quick Start

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .build();

ChatResponse response = chatService.chat("Hello! How are you?");
System.out.println(response.toAssistantMessage().content());
// → Hello! How can I help you today?
```
> **Note:** To see the list of available models, refer to [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx).

---

## Overview

The `ChatService` enables you to:

- Build conversational AI applications with multi-turn dialogue.
- Stream responses in real-time for interactive experiences.
- Enable models to call external functions and tools.
- Maintain conversation history and context.
- Configure generation parameters for customized outputs.
- Handle structured JSON responses.
- Support reasoning capabilities for complex problem-solving.

---

## Service Configuration

### Basic Setup

To start using the Chat Service, create a `ChatService` instance with the minimum required configuration. The following example shows the essential parameters needed to authenticate and select a model. Additional configuration options are described below.

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com")
    .modelId("ibm/granite-4-h-small")
    .build();
```

### Using CloudRegion

Instead of manually specifying the `baseUrl`, you can use the `CloudRegion` to automatically configure the correct endpoint for your IBM Cloud region. This is more convenient and less error-prone.

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `projectId` | String | Conditional | Project ID where the model is deployed |
| `spaceId` | String | Conditional | Space ID where the model is deployed (alternative to `projectId`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `modelId` | String | Yes | Foundation model ID |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `parameters` | ChatParameters | No | Default parameters applied to all requests |
| `tools` | List\<Tool\> | No | Default tools available to the model |
| `messageInterceptor` | MessageInterceptor | No | Modify assistant messages before returning |
| `toolInterceptor` | ToolInterceptor | No | Normalize/modify tool call arguments |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

### Advanced Configuration

You can configure **default parameters** and **tools** that will automatically apply to every chat request created by this `ChatService` instance. These defaults simplify reuse and ensure consistent behavior across multiple calls.

```java
ChatParameters defaultParameters = ChatParameters.builder()
    .maxCompletionTokens(1000)
    .temperature(0.7)
    .build();

Tool emailTool = Tool.of(
    "send_email",
    "Send an email",
    JsonSchema.object()
        .property("to", JsonSchema.string())
        .property("subject", JsonSchema.string())
        .property("body", JsonSchema.string())
        .required("to", "subject", "body")
);

var chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .parameters(defaultParameters)
    .tools(emailTool)
    .build();
```

---

## Message Types

The `ChatService` uses structured **message objects** to represent all interactions in a conversation. Each message type serves a specific role, ensuring that the conversation flows are consistent and easy to manage.

The message types are:

- **SystemMessage** – defines the assistant's behavior and personality before the conversation begins. Use this to prime the model with instructions or context.  
- **UserMessage** – represents input from a user, which can include text, images, video, or audio. A single `UserMessage` can contain multiple content elements.  
- **AssistantMessage** – represents a response from the assistant, which can include text, reasoning information, and any tool calls executed during the conversation.  
- **ToolMessage** – represents a response from a tool invoked by the assistant.

These message types help manage **multi-turn conversations**, tool invocations, and structured outputs in a clear and consistent way.

> **Tip:** Always start your conversation with a `SystemMessage` to set clear instructions for the assistant. Default behavior, content, and context can then be extended with `UserMessage` inputs, and responses are represented by `AssistantMessage` and `ToolMessage`.

### SystemMessage

Sets the assistant's behavior and personality.

```java
SystemMessage.of("You are a helpful assistant specialized in programming.");
```

### UserMessage

Sends text or multimodal content.

```java
// Plain text
UserMessage.text("Hello!");

// With image from file
UserMessage.of(
    TextContent.of("Describe this image"),
    ImageContent.from(new File("image.jpg"))
);

// Shorthand image with Path
UserMessage.image("Analyze this image", Paths.get("image.png"));
```

### AssistantMessage

Represents the model's response.

```java
AssistantMessage assistantMessage = response.toAssistantMessage();

String content = assistantMessage.content();
String thinking = assistantMessage.thinking(); // Available when reasoning is enabled
boolean hasTools = assistantMessage.hasToolCalls();
List<ToolCall> tools = assistantMessage.toolCalls();
```

---

## Examples

### Simple Chat

Let's start with the simplest possible interaction - sending a single message and getting a response. This is perfect for one-off questions or when you don't need to maintain conversation history.

```java
ChatResponse response = chatService.chat("What is the capital of France?");
System.out.println(response.toAssistantMessage().content());
// → Paris is the capital of France.
```

### Multi-Turn Conversation

For more natural interactions, you'll want to maintain conversation history. This allows the model to remember previous messages and provide context-aware responses. Here's how to build a conversation where each message builds on the previous ones.

```java
var conversation = new ArrayList<ChatMessage>();
conversation.add(SystemMessage.of("You are a helpful assistant"));
conversation.add(UserMessage.text("What is the capital of France?"));

var response = chatService.chat(conversation);
conversation.add(response.toAssistantMessage());

System.out.println(response.toAssistantMessage().content());
// → The capital of France is Paris.

conversation.add(UserMessage.text("What is its population?"));
response = chatService.chat(conversation);

System.out.println(response.toAssistantMessage().content());
// → Paris has a population of approximately 2.2 million people...
```

### Customizing Generation Parameters

Sometimes you need more control over how the model generates responses. Maybe you want shorter answers, more creative output, or deterministic results. Parameters let you fine-tune the generation behavior to match your needs.

```java
var parameters = ChatParameters.builder()
    .maxCompletionTokens(100)
    .temperature(0.3)
    .topP(0.9)
    .build();

List<ChatMessage> messages = List.of(
    SystemMessage.of("You are a concise assistant"),
    UserMessage.text("Explain quantum computing")
);

var response = chatService.chat(messages, parameters);
```

### Streaming Responses

When building interactive applications, waiting for the complete response can feel slow. Streaming lets you display text as it's generated, creating a more responsive user experience - just like typing appears chunk by chunk.

```java
CompletableFuture<ChatResponse> future = chatService.chatStreaming(
    List.of(UserMessage.text("Tell me a story about a robot")),
    System.out::print
);

ChatResponse finalResponse = future.get();
```

### Streaming with ChatHandler

For more sophisticated applications, you might need access to additional information during streaming - like response metadata, finish reasons, or error handling. The `ChatHandler` interface gives you complete control over the streaming process.

```java
chatService.chatStreaming(
    messages,
    new ChatHandler() {
        @Override
        public void onPartialResponse(String text, PartialChatResponse partial) {
            System.out.print(text);
        }

        @Override
        public void onCompleteResponse(ChatResponse response) {
            System.out.println("Total tokens: " + response.usage().totalTokens());
        }

        @Override
        public void onError(Throwable error) {
            System.err.println("Error: " + error.getMessage());
        }
    }
);
```

| Callback | Required | Description |
|----------|----------|-------------|
| `onPartialResponse` | Yes | Called for each text chunk as it arrives |
| `onCompleteResponse` | No | Called once when streaming completes successfully |
| `onError` | No | Called when an error occurs |
| `onPartialToolCall` | No | Called for each fragment of a streaming tool call |
| `onCompleteToolCall` | No | Called once per tool when arguments are fully assembled |
| `onPartialThinking` | No | Called for each chunk of reasoning content |
| `failOnFirstError` | No | Return `true` to stop streaming on first error (default: `false`) |

> **Threading note:** All callbacks execute sequentially. On `Java 21+`, virtual threads are used by default. Custom executors can be configured via `CallbackExecutorProvider` SPI.

### Tool Calling

One of the most powerful features is enabling the model to call external functions. Instead of just generating text, the model can decide when it needs to perform actions - like sending emails, querying databases, or calling APIs. Here's how to define a tool and let the model use it.

```java
Tool emailTool = Tool.of(
    "send_email",
    "Send an email to a recipient",
    JsonSchema.object()
        .property("to", JsonSchema.string("Email address"))
        .property("subject", JsonSchema.string("Email subject"))
        .property("body", JsonSchema.string("Email body"))
        .required("to", "subject", "body")
);

List<ChatMessage> messages = new ArrayList<>(List.of(
    SystemMessage.of("You are a helpful assistant"),
    UserMessage.text("Send an email to john@example.com with body \"Hello from watsonx.ai\"")
));

ChatResponse response = chatService.chat(messages, List.of(emailTool));
AssistantMessage assistantMsg = response.toAssistantMessage();

if (assistantMsg.hasToolCalls()) {

    List<ToolMessage> toolMessages = assistantMsg.processTools((toolName, args) -> {
        sendEmail(args.get("to"), args.get("subject"), args.get("body"));
        return "Email sent successfully to " + args.get("to");
    });

    messages.add(assistantMsg);
    messages.addAll(toolMessages);
    response = chatService.chat(messages, List.of(emailTool));
}

System.out.println(response.toAssistantMessage().content());
// → The email has been sent successfully to john@example.com.
```

### Message Interceptor

The `MessageInterceptor` allows you to modify or sanitize the assistant's text content before it's returned to your application. This is useful for removing unwanted formatting, filtering content, or applying transformations.

> **Important:** `MessageInterceptor` only works with **non-streaming** chat requests. For streaming requests, you need to process the content directly in the `ChatHandler` callbacks.

```java
ChatService chatService = ChatService.builder()
    // ...
    .messageInterceptor((ctx, message) -> message.strip().replaceAll("\\s+", " ")).build();
```

### Tool Interceptor

The `ToolInterceptor` allows you to modify tool call arguments before they're executed. This is useful for validation, normalization, adding default values, or logging tool calls.

```java
ChatService chatService = ChatService.builder()
    // ...
    .toolInterceptor((ctx, functionCall) -> {
        // Modify arguments before execution
        return functionCall.withArguments(...);
    })
    .build();
```

### Structured JSON Output

When you need the model to return data in a specific format (like for APIs or data processing), you can request **JSON** output. This is perfect for extracting structured information from text or generating data that other systems can consume.

```java
record Response(String name, List<String> useCases) {};

ChatParameters parameters = ChatParameters.builder()
    .responseAsJson()
    .build();

List<ChatMessage> messages = List.of(
    SystemMessage.of("You are a helpful assistant that outputs JSON"),
    UserMessage.text("""
        Give me a programming language with their use cases
        Use the following json format as result:
        {
            "name": ...
            "use_cases": [...]
        }""")
    );

ChatResponse response = chatService.chat(messages, parameters);
System.out.println(response.toAssistantMessage().toObject(Response.class));
// → Response[name=Python, useCases=[Web development, Data analysis, Machine learning, Artificial intelligence, Scientific computing, Automation and scripting, Rapid application development]]
```

For even more control over **JSON output**, you can provide a schema that defines exactly what structure you expect. The model will generate **JSON** that conforms to your **schema**, ensuring consistency and making it easier to process the results.

```java
JsonSchema schema = JsonSchema.array().items(JsonSchema.string()).build();

ChatParameters parameters = ChatParameters.builder()
    .responseAsJsonSchema(schema)
    .build();

List<ChatMessage> messages = List.of(
    SystemMessage.of("You are a helpful assistant"),
    UserMessage.text("Give me three programming languages")
);

ChatResponse response = chatService.chat(messages, parameters);
var languages = response.toAssistantMessage().toObject(TypeToken.listOf(String.class));
System.out.println(languages);
// → ["Python", "JavaScript", "Java"]
```
> **Important:** By default, `Jackson` uses `snake_case` for JSON property names. Make sure that the field names in your prompt and schema follow the `snake_case` convention (e.g., **use_cases** instead of **useCases**) to ensure proper deserialization.

### Vision Capabilities

Vision-enabled models can analyze images alongside text. This is useful for image description, visual question answering, OCR, and more. Simply include an image in your user message.

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("mistralai/mistral-medium-2505")  // Vision-capable model
    .build();

var message = UserMessage.image(
    "Give a short description of the image",
    Paths.get("/path/to/image.jpg")
);
    
var response = chatService.chat(message);
System.out.println(response.toAssistantMessage().content());
```

> **Model compatibility:** Not all models support vision. Check the [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx) page before using this feature.

### Guided Choice (Constrained Output)

When you need the model to choose from a specific set of options, use guided choice. This is perfect for classification tasks, **yes/no** questions, or any scenario where you want to limit the possible outputs.

```java
ChatParameters parameters = ChatParameters.builder()
    .guidedChoice("Yes", "No")
    .build();

ChatRequest request = ChatRequest.builder()
    .messages(UserMessage.text("Is 2 + 2 equal to 5?"))
    .parameters(parameters)
    .build();

String answer = chatService.chat(request).toAssistantMessage().content();
System.out.println(answer); 
// → "No"
```

---

## Reasoning / Thinking Mode

Some foundation models can include internal reasoning (also called "thinking") steps as part of their responses. Depending on the model, this reasoning may be embedded in the same text as the final response, or returned separately in a dedicated field.

There are **two configuration modes** to enable reasoning:

- **ExtractionTags** - For models that return reasoning and response in the same text block
- **ThinkingEffort/Boolean** - For models that already separate reasoning and response automatically

### Models that mix reasoning and response in the same text

Use `ExtractionTags` when the model outputs reasoning and response in the same text string. The tags define XML-like markers used to separate the reasoning from the final response.

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-3-3-8b-instruct")
    .build();

ChatRequest request = ChatRequest.builder()
    .messages(UserMessage.text("Why is the sky blue?"))
    .thinking(ExtractionTags.of("think", "response"))
    .build();

ChatResponse response = chatService.chat(request);
AssistantMessage message = response.toAssistantMessage();

System.out.println("Reasoning: " + message.thinking());
System.out.println("Answer:    " + message.content());
```

**Tag behavior:**
- Both tags specified: extract reasoning from the first tag, response from the second
- Only the reasoning tag specified: everything outside that tag is treated as the response

**Streaming with ExtractionTags:**

```java
chatService.chatStreaming(request, new ChatHandler() {
    @Override
    public void onPartialThinking(String chunk, PartialChatResponse partial) {
        System.out.print(chunk);  // Streams the reasoning in real-time
    }

    @Override
    public void onPartialResponse(String chunk, PartialChatResponse partial) {
        System.out.print(chunk);  // Streams the answer in real-time
    }
});
```

### Models that return reasoning and response as separate fields

For models that already return reasoning and response as separate fields, use `ThinkingEffort` to control how much reasoning the model applies during generation, or simply enable it with a boolean flag.

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("openai/gpt-oss-120b")
    .build();

ChatRequest request = ChatRequest.builder()
    .messages(UserMessage.text("Why is the sky blue?"))
    .thinking(ThinkingEffort.HIGH)
    .build();

AssistantMessage message = chatService.chat(request).toAssistantMessage();
System.out.println("Reasoning: " + message.thinking());
System.out.println("Answer:    " + message.content());
```

---

## ToolRegistry

When working with multiple tools, the `ToolRegistry` provides a convenient way to register, manage, and execute tools automatically. This approach simplifies tool handling by centralizing tool definitions and execution logic, making your code cleaner and more maintainable.

### Basic Usage

```java
ToolService toolService = ToolService.builder()
    .apiKey(WATSONX_API_KEY)
    .baseUrl(CloudRegion.DALLAS)
    .build();

ToolRegistry toolRegistry = ToolRegistry.builder()
    .register(new GoogleSearchTool(toolService), new WebCrawlerTool(toolService))
    .build();

ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .tools(toolRegistry.tools())
    .build();

List<ChatMessage> messages = new ArrayList<>();
messages.add(SystemMessage.of("You are a helpful assistant"));
messages.add(UserMessage.text("Is there a watsonx.ai Java SDK?"));

AssistantMessage assistant = chatService.chat(messages).toAssistantMessage();
messages.add(assistant);

while (assistant.hasToolCalls()) {
    messages.addAll(assistant.processTools(toolRegistry::execute));
    assistant = chatService.chat(messages).toAssistantMessage();
    messages.add(assistant);
}

System.out.println(assistant.content());
// → Yes – IBM publishes a **Java SDK for watsonx.ai** ...
```

### Creating Custom Tools

In order to create your own tools in combination with the `ToolRegistry`, you need to implement the `ExecutableTool` interface:

```java
public class WeatherTool implements ExecutableTool {

    @Override
    public String name() { 
        return "get_weather"; 
    }

    @Override
    public Tool schema() {
        return Tool.of(
            "get_weather",
            "Get current weather for a location",
            JsonSchema.object()
                .property("location", JsonSchema.string("City name"))
                .property("unit", JsonSchema.string("celsius or fahrenheit"))
                .required("location")
                .build()
        );
    }

    @Override
    public String execute(ToolArguments args) {
        String location = args.get("location");
        // ... call weather API
        return "The weather in " + location + " is ...";
    }
}
```

### Lifecycle Callbacks

`ToolRegistry` supports three lifecycle callbacks for monitoring and controlling tool execution:

```java
ToolRegistry registry = ToolRegistry.builder()
    .register(new WeatherTool())
    .beforeExecution((toolName, toolArgs) -> System.out.println("Calling: " + toolName))
    .afterExecution((toolName, toolArgs, result) -> System.out.println("Result: " + result))
    .onError((toolName, toolArgs, error) -> System.err.println(toolName + " failed: " + error.getMessage()))
    .build();
```

#### Selective Tool Registration

You can register specific tools for different conversations:

```java
ToolRegistry registry = ToolRegistry.builder()
    .register(new WeatherTool(), new SearchTool(), new CalculatorTool())
    .build();

// Use all tools
ChatService chatService = ChatService.builder()
    .tools(registry.tools())  // All 3 tools
    .build();

// Use only specific tools
ChatService limitedService = ChatService.builder()
    .tools(registry.tools("get_weather", "search"))  // Only 2 tools
    .build();
```

---

## Chat Parameters

The `ChatParameters` class allows you to customize and fine-tune the behavior of chat requests sent to the foundation model. By configuring these parameters, you can control aspects such as response length, creativity, repetition handling, output format, and more. This flexibility enables you to tailor the model's generation to match your specific use case requirements.

### Builder Reference

| Parameter | Type | Range | Description |
|-----------|------|-------|-------------|
| `maxCompletionTokens` | Integer | ≥ 0 | Maximum tokens in the response (0 = model max) |
| `temperature` | Double | 0.0 – 2.0 | Randomness (0.0 = deterministic) |
| `topP` | Double | 0.0 – 1.0 | Nucleus sampling threshold |
| `frequencyPenalty` | Double | -2.0 – 2.0 | Discourage frequent tokens |
| `presencePenalty` | Double | -2.0 – 2.0 | Encourage new topics |
| `repetitionPenalty` | Double | > 1.0 | Discourage repeated words/phrases |
| `lengthPenalty` | Double | Any | > 1.0 shorter, < 1.0 longer, 1.0 neutral |
| `stop` | List\<String\> | Max 4 | Stop sequences to end generation |
| `seed` | Integer | Any | Random seed for reproducibility |
| `n` | Integer | ≥ 1 | Number of completions to generate |
| `logprobs` | Boolean | — | Return log probabilities |
| `topLogprobs` | Integer | ≥ 1 | Top token log probs (requires `logprobs=true`) |
| `logitBias` | Map\<String, Integer\> | — | Adjust token probabilities |
| `timeLimit` | Duration | Any | Maximum generation time |
| `toolChoiceOption` | ToolChoiceOption | AUTO, REQUIRED, NONE | Tool selection strategy |
| `toolChoice` | String | Tool name | Force a specific tool call |
| `guidedChoice` | Set\<String\> | Any | Constrain output to one of the given options |
| `guidedRegex` | String | Valid regex | Constrain output to a regex pattern |
| `guidedGrammar` | String | CFG grammar | Constrain output to a context-free grammar |
| `responseFormat` | — | — | Use `responseAsText()`, `responseAsJson()`, `responseAsJsonSchema()` |
| `modelId` | String | — | Override default model for this request |
| `projectId` | String | — | Override default project for this request |
| `spaceId` | String | — | Override default space for this request |
| `transactionId` | String | — | Request tracking ID |
| `crypto` | Crypto | — | Encryption configuration |

---

## Related Resources

- [Chat API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#chat-completions)
- [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx)
- [Basic Chatbot (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot)
- [Streaming Chatbot (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot-streaming)
- [Chatbot with Tools (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot-tools)
- [Chatbot with ToolRegistry (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot-with-tool-registry)
- [Chatbot with Reasoning (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot-reasoning)