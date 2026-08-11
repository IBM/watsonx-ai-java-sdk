---
id: chat-service
title: Chat Service
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

ChatResponse response = chatService.chat("Hello!");
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
| `messageInterceptor` | MessageInterceptor\<ChatRequest\> | No | Modify assistant messages before returning |
| `toolInterceptor` | ToolInterceptor\<ChatRequest\> | No | Normalize/modify tool call arguments |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

### Advanced Configuration

You can configure **default parameters** and **tools** that will automatically apply to every chat request created by a `ChatService` instance. These defaults simplify reuse and ensure consistent behavior across multiple calls.

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

The `ChatService` uses structured **message objects** to represent all interactions in a conversation. Each message type serves a specific role, ensuring that conversation flows are consistent and easy to manage.

- **SystemMessage** - defines the assistant's behavior and personality before the conversation begins. Use this to prime the model with instructions or context.
- **UserMessage** - represents input from a user, which can include text, images, video, or audio. A single `UserMessage` can contain multiple content elements.
- **AssistantMessage** - represents a response from the assistant, which can include text, reasoning information, and any tool calls executed during the conversation.
- **ToolMessage** - represents a response from a tool invoked by the assistant.

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

// With image from file - ImageContent.from(...) throws IOException
try {
    UserMessage.of(
        TextContent.of("Describe this image"),
        ImageContent.from(new File("image.jpg"))
    );
} catch (IOException e) {
    // handle the I/O error
}

// Shorthand image with Path (reads the file internally)
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

Send a single message and get a response.

```java
ChatResponse response = chatService.chat("What is the capital of France?");
System.out.println(response.toAssistantMessage().content());
// → Paris is the capital of France.
```

### Multi-Turn Conversation

Maintain a conversation history so the model can remember previous messages and provide context-aware responses.

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

Fine-tune the generation behavior: shorter answers, more creative output, or deterministic results.

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

---

## Streaming

Display text as it is generated instead of waiting for the complete response.

### Simple Streaming

Pass a `Consumer<String>` to receive each text chunk as it arrives:

```java
CompletableFuture<ChatResponse> future = chatService.chatStreaming(
    List.of(UserMessage.text("Tell me a story about a robot")),
    System.out::print
);

ChatResponse finalResponse = future.get();
```

### Streaming with ChatHandler

Implement `ChatHandler` for full control over the streaming process (metadata, finish reasons, tool call fragments, error handling):

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

> **Threading note:** Callbacks run on the callback executor (virtual threads on Java 21+, configurable via the `CallbackExecutorProvider` SPI). Within a single request every callback is delivered sequentially, in the order the events were emitted, and never concurrently with another one: all the `onPartialToolCall` fragments of a tool call reach the handler before its `onCompleteToolCall`, tool calls arrive in index order, and `onCompleteResponse` comes last. Consecutive callbacks may run on different threads, but each one returns before the next one starts and what it wrote is visible to the next one, so your handler can accumulate into unsynchronized fields. The flip side is that a callback that blocks delays the ones after it in the same request.

---

## Tool Calling

Tool calling enables the model to invoke external functions instead of generating text alone. The model decides when an action is needed and returns a structured tool call that your code executes.

### Basic Tool Calling

Define a tool, pass it to the request, and handle the tool call in a loop:

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

### Guided Choice (Constrained Output)

Constrain the model's output to a fixed set of options. Useful for classification tasks and yes/no questions.

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

## Interceptors

Interceptors run automatically after every non-streaming response, before the result is returned to your application. They are configured once on the service builder and apply transparently to all subsequent calls. Both are `@FunctionalInterface`, so you can pass a lambda directly.

### Message Interceptor

`MessageInterceptor` lets you modify or sanitize the assistant's text content. Common uses: stripping whitespace, filtering unwanted patterns, normalizing formatting.

> **Note:** `MessageInterceptor` applies to **non-streaming** requests only. For streaming, process the content directly inside `ChatHandler` callbacks.

```java
ChatService chatService = ChatService.builder()
    // ...
    .messageInterceptor((ctx, message) -> message == null ? "" : message.strip())
    .build();
```

### Tool Interceptor

`ToolInterceptor` intercepts each completed tool call before it reaches your handler, letting you validate, normalize, or unwrap the arguments (for example, unwrapping double-encoded JSON strings that some models produce).

```java
ChatService chatService = ChatService.builder()
    // ...
    .toolInterceptor((ctx, functionCall) -> {
        var args = functionCall.arguments();
        // Unwrap double-encoded JSON strings if present
        return args != null && args.startsWith("\"")
            ? functionCall.withArguments(Json.fromJson(args, String.class))
            : functionCall;
    })
    .build();
```

### InterceptorContext

Both interceptors receive an `InterceptorContext` as their first argument, which provides access to the current request, the current response, and a way to invoke the model again.

| Method | Description |
|--------|-------------|
| `ctx.request()` | The original `ChatRequest` that triggered this response |
| `ctx.response()` | An `Optional<ChatResponse>` with the current response |
| `ctx.invoke(ChatRequest)` | Sends a new request to the model and returns its response |

`MessageInterceptor`, `ToolInterceptor`, and `InterceptorContext` are parameterized by the request type of the service they are registered on, so `ctx.request()` returns that concrete type with no cast: `ChatRequest` here, `DeploymentChatRequest` on [`DeploymentService`](/services/deployment-service), and `ModelGatewayChatRequest` on [`ModelGatewayChatService`](/services/model-gateway). The type argument is inferred when you pass a lambda, and only needs to be written out if you declare the interceptor separately:

```java
MessageInterceptor<ChatRequest> interceptor = (ctx, message) -> message == null ? "" : message.strip();

ChatService chatService = ChatService.builder()
    // ...
    .messageInterceptor(interceptor)
    .build();
```

`ctx.invoke()` reuses the same `ChatService` instance (same model, project, base URL, and default parameters), so you can add a second reasoning step without instantiating anything new. Per-request overrides are still possible via `ChatParameters`:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .messageInterceptor((ctx, message) -> {

        // Override the model just for this verification call
        var verificationParams = ChatParameters.builder()
            .modelId("mistralai/mistral-small-3-1-24b-instruct-2503")
            .guidedChoice("PASS", "FAIL")
            .build();

        var verificationRequest = ChatRequest.builder()
            .parameters(verificationParams)
            .messages(
                SystemMessage.of("You are a fact-checker. Reply with PASS or FAIL."),
                UserMessage.text("Is this response factually correct?\n\n" + message))
            .build();

        var verdict = ctx.invoke(verificationRequest).toAssistantMessage().content();
        return verdict.equals("FAIL")
            ? "I'm not confident in my answer. Please consult an expert."
            : message;
    })
    .build();

chatService.chat("Does water boil on the Moon?");
```

> `ctx.invoke()` counts as a separate API call and consumes additional tokens. Use it when the benefit (validation, rewriting, classification) justifies the cost.

---

## Structured Output

Constrain the model to produce valid JSON, making it straightforward to deserialize the response directly into your domain objects.

### JSON Mode

Instructs the model to always produce a valid JSON object. Define the expected structure in your system prompt:

```java
record Response(String name, List<String> useCases) {}

ChatParameters parameters = ChatParameters.builder()
    .responseAsJson()
    .build();

List<ChatMessage> messages = List.of(
    SystemMessage.of("You are a helpful assistant that outputs JSON"),
    UserMessage.text("""
        Give me a programming language with their use cases.
        Use the following JSON format:
        {
            "name": ...
            "use_cases": [...]
        }""")
);

ChatResponse response = chatService.chat(messages, parameters);
System.out.println(response.toAssistantMessage().toObject(Response.class));
// → Response[name=Python, useCases=[Web development, Data analysis, ...]]
```

### JSON Schema Mode

Provide a schema that defines exactly what structure you expect. The model will generate output that conforms to it:

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

> **Note:** By default, Jackson uses `snake_case` for JSON property names. Make sure the field names in your prompt and schema follow the same convention (e.g., `use_cases` instead of `useCases`) to ensure correct deserialization.

---

## Image

Models with vision capabilities can analyze images alongside text (image description, visual question answering, OCR, and more). Include an image directly in the `UserMessage`:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("mistralai/mistral-small-3-1-24b-instruct-2503")
    .build();

var message = UserMessage.image(
    "Give a short description of the image",
    Paths.get("/path/to/image.jpg")
);

var response = chatService.chat(message);
System.out.println(response.toAssistantMessage().content());
```

> **Model compatibility:** Not all models support image input. Check the [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx) page before using this feature.

## Video

Models with video capabilities can analyze video content alongside text (video description, action recognition, scene understanding, and more). Include a video file directly in the `UserMessage`:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("nvidia-nemotron-nano-12b-v2-vl-fp8")
    .build();

ChatResponse response = chatService.chat(
    UserMessage.video("Tell me more about this video", Paths.get("/path/to/video.mp4"))
);
System.out.println(response.toAssistantMessage().content());
```

> **Model availability:** `nvidia-nemotron-nano-12b-v2-vl-fp8` must be available in your region and plan and deployed to a deployment space. Check the [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx) page before using this feature.

## Audio

Audio-enabled models can process spoken audio alongside text (transcription, question answering on audio, and more). Include an audio file directly in the `UserMessage`:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4.0-1b-speech")
    .build();

ChatResponse response = chatService.chat(
    UserMessage.audio("Transcribe the audio into text.", Paths.get("/path/to/audio.wav"))
);
System.out.println(response.toAssistantMessage().content());
```

You can also pass an `InputStream` when the audio comes from a web upload or streaming source:

```java
UserMessage message = UserMessage.audio("Transcribe the audio into text.", inputStream, "audio/wav");
```

Or build the content manually from base64-encoded data:

```java
UserMessage message = UserMessage.of(
    TextContent.of("Transcribe the audio into text."),
    AudioContent.of("audio/wav", base64EncodedData)
);
```

> **Model availability:** `ibm/granite-4.0-1b-speech` must be available in your region and plan. Check the [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx) page before using this feature.

---

## Reasoning / Thinking Mode

Some foundation models can include internal reasoning steps as part of their response. Depending on the model, this reasoning may be embedded in the same text as the final response, or returned separately in a dedicated field.

There are two configuration modes:

- **ExtractionTags** - for models that return reasoning and response in the same text block.
- **ThinkingEffort** - for models that already separate reasoning and response automatically.

### Mixed reasoning-response models

Use `ExtractionTags` when the model outputs reasoning and response together as a single string. Use `Think` and `Response` to specify the exact opening and closing delimiters used by the model:

```java
// Granite
ChatRequest request = ChatRequest.builder()
    .messages(UserMessage.text("Why is the sky blue?"))
    .thinking(ExtractionTags.of(
        new Think("<think>", "</think>"),
        new Response("<response>", "</response>")
    ))
    .build();

// Gemma-4
ChatRequest request = ChatRequest.builder()
    .messages(UserMessage.text("Why is the sky blue?"))
    .thinking(ExtractionTags.of(new Think("<|channel>thought\n", "<channel|>")))
    .build();

ChatResponse response = chatService.chat(request);
AssistantMessage message = response.toAssistantMessage();

System.out.println("Reasoning: " + message.thinking());
System.out.println("Answer:    " + message.content());
```

**Tag behavior:**
- Both `Think` and `Response` specified: extract reasoning from the first delimiter pair, response from the second.
- Only `Think` specified: everything outside that delimiter pair is treated as the response.

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

### Separate reasoning and response fields

For models that already separate reasoning from response, use `ThinkingEffort` to control how much reasoning the model applies, or enable it with a boolean flag:

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

`ToolRegistry` centralizes tool definitions and execution logic, keeping the agentic loop clean and easy to maintain.

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
    messages.addAll(assistant.processTools(toolRegistry));
    assistant = chatService.chat(messages).toAssistantMessage();
    messages.add(assistant);
}

System.out.println(assistant.content());
// → Yes – IBM publishes a **Java SDK for watsonx.ai** ...
```

### Creating Custom Tools

Implement `ExecutableTool` to define your own tools:

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

Three callbacks are available for monitoring and controlling tool execution:

```java
ToolRegistry registry = ToolRegistry.builder()
    .register(new WeatherTool())
    .beforeExecution((toolName, toolArgs) -> System.out.println("Calling: " + toolName))
    .afterExecution((toolName, toolArgs, result) -> System.out.println("Result: " + result))
    .onError((toolName, toolArgs, error) -> System.err.println(toolName + " failed: " + error.getMessage()))
    .build();
```

### Selective Tool Registration

Register all tools once and expose only a subset per conversation:

```java
ToolRegistry registry = ToolRegistry.builder()
    .register(new WeatherTool(), new SearchTool(), new CalculatorTool())
    .build();

// Use all tools
ChatService chatService = ChatService.builder()
    .tools(registry.tools())
    .build();

// Use only specific tools
ChatService limitedService = ChatService.builder()
    .tools(registry.tools(WeatherTool.class, SearchTool.class))
    .build();
```

---

## Chat Parameters

`ChatParameters` controls response length, creativity, repetition handling, output format, and more.

### Builder Reference

| Parameter | Type | Range | Description |
|-----------|------|-------|-------------|
| `maxCompletionTokens` | Integer | ≥ 0 | Maximum tokens in the response. Set to `0` to use the model's full context window. |
| `temperature` | Double | 0.0 – 2.0 | Randomness (0.0 = deterministic) |
| `topP` | Double | 0.0 – 1.0 | Nucleus sampling threshold |
| `frequencyPenalty` | Double | -2.0 – 2.0 | Discourage frequent tokens |
| `presencePenalty` | Double | -2.0 – 2.0 | Encourage new topics |
| `repetitionPenalty` | Double | > 1.0 | Discourage repeated words/phrases |
| `lengthPenalty` | Double | Any | > 1.0 shorter, < 1.0 longer, 1.0 neutral |
| `stop` | List\<String\> | Max 4 | Stop sequences to end generation |
| `seed` | Integer | Any | Random seed for reproducibility |
| `n` | Integer | ≥ 1 | Number of completions to generate |
| `logprobs` | Boolean | - | Return log probabilities |
| `topLogprobs` | Integer | ≥ 1 | Top token log probs (requires `logprobs=true`) |
| `logitBias` | Map\<String, Integer\> | - | Adjust token probabilities |
| `timeLimit` | Duration | Any | Maximum generation time |
| `toolChoiceOption` | ToolChoiceOption | AUTO, REQUIRED, NONE | Tool selection strategy |
| `toolChoice` | String | Tool name | Force a specific tool call |
| `guidedChoice` | Set\<String\> | Any | Constrain output to one of the given options |
| `guidedRegex` | String | Valid regex | Constrain output to a regex pattern |
| `guidedGrammar` | String | CFG grammar | Constrain output to a context-free grammar |
| `responseFormat` | - | - | Use `responseAsText()`, `responseAsJson()`, `responseAsJsonSchema()` |
| `modelId` | String | - | Override default model for this request |
| `projectId` | String | - | Override default project for this request |
| `spaceId` | String | - | Override default space for this request |
| `transactionId` | String | - | Request tracking ID |
| `crypto` | String | - | Key reference for encrypting the inference request (e.g., IBM Key Protect CRN) |

---

## Content Moderation

Content moderation lets watsonx.ai screen chat input and output for **Personally Identifiable Information (PII)**, **Hate and Profanity (HAP)**, and **Granite Guardian** categories. When one or more detectors match, the results are returned alongside the assistant's response so your application can decide how to react (redact, block, log, or warn the user) based on your own policy.

> **Two modes are available for content screening in the SDK:**
> - **Inline chat moderation** (this section) - attach a `ChatModeration` to a `ChatRequest`. Detectors run as part of the chat call and results come back on the `TextChatResponse`. Use it when you want screening to happen *during* the generation.
> - **Standalone detection** via [`DetectionService`](/services/detection-service) - analyze arbitrary text with `Hap`, `Pii`, and `GraniteGuardian` outside a chat flow (e.g. moderating user-generated content, log analysis, offline scans).
>
> The two APIs are independent: the same detector categories but different request types (`ChatRequest` vs `DetectionTextRequest`), different response shapes, and different classes (`com.ibm.watsonx.ai.chat.ChatModeration.*` vs `com.ibm.watsonx.ai.detection.detector.*`).

### Detector Types

| Detector | Purpose | Enable via |
|---|---|---|
| `pii` | Detects personal data such as phone numbers, emails, credit cards, addresses | `.pii(p -> p.input(true).output(true))` |
| `hap` | Detects hate and profanity above a confidence threshold | `.hap(h -> h.input(0.8f).output(0.9f))` |
| `graniteGuardian` | General-purpose harm/safety classifier | `.graniteGuardian(g -> g.input(0.85f))` |

Each detector accepts an `input` toggle/threshold, an `output` toggle/threshold, and an optional `.mask(true)` flag.

### Enabling Moderation on a Request

Attach a `ChatModeration` configuration to a `ChatRequest` via `.moderations(...)`:

```java
var moderation = ChatModeration.builder()
    .pii(p -> p.output(true))
    .hap(h -> h.output(0.8f))
    .build();

var request = ChatRequest.builder()
    .messages(
        SystemMessage.of("You are a helpful assistant"),
        UserMessage.text("Contact me at john@example.com, phone 555-1234"))
    .moderations(moderation)
    .build();

TextChatResponse response = chatService.chat(request);
```

### Reading the Results

When one or more detectors match, `TextChatResponse` exposes two independent structures:

- `response.moderations()` - a map keyed by detector name (`"pii"`, `"hap"`, `"granite_guardian"`) with per-match details (`score`, `input`, `position`, `entity`, `word`).
- `response.detections()` - a map keyed by target position (`"input"`, `"output"`) with per-choice detection entries containing the underlying detector ID and matches.

Both are `null` when no detector matched.

```java
if (response.moderations() != null) {
    var piiMatches = response.moderations().get("pii");
    if (piiMatches != null) {
        for (var match : piiMatches) {
            System.out.printf("%s: %s (score=%.2f, at [%d, %d))%n",
                match.entity(), match.word(), match.score(),
                match.position().start(), match.position().end());
        }
    }
}
```

Fields on `ModerationResult`:

| Field | Type | Description |
|---|---|---|
| `score` | `float` | Confidence of the match (0.0 – 1.0) |
| `input` | `boolean` | `true` if the match was found in the input, `false` for output |
| `position` | `Position` | Start (inclusive) / end (exclusive) offsets in the text |
| `entity` | `String` | Detected entity type (e.g. `"PhoneNumber"`) |
| `word` | `String` | The matched text |

### Restricting Moderation to Input Ranges

You can limit input moderation to specific text ranges via `InputRanges`:

```java
var moderation = ChatModeration.builder()
    .pii(p -> p.input(true))
    .inputRanges(List.of(
        InputRanges.of(0, 50),
        InputRanges.of(100, 150)))
    .build();
```

Only text within the given ranges is evaluated on input.

### Redacting Content

The SDK returns detection results untouched without modifying the assistant message content. To redact matched values, use the `Masker` utility class, which walks through the output moderation matches of a `TextChatResponse` and rewrites the corresponding ranges:

```java
var content = response.toAssistantMessage().content();

// Default: replace each matched range with '*' of the same length.
String masked = Masker.mask(content, response);

// Custom: replace each match with a labelled placeholder.
String labelled = Masker.mask(content, response, m -> "[" + m.entity() + "]");
```

Pass a custom replacer when you need a redaction policy that depends on more than the single match (e.g. hashing, tokenization, external lookup).

When you need the redacted result as an `AssistantMessage` ready to be added back to a conversation, use `maskToMessage`:

```java
// Default: replaces each match with '*' repeated for the length of the match.
AssistantMessage masked = Masker.maskToMessage(response);

// Custom: replaces each match with a labelled placeholder.
AssistantMessage labelled = Masker.maskToMessage(response, m -> "[" + m.entity() + "]");
```

### Streaming Chat Moderation

Moderation is fully supported in streaming mode. Results are available in two places:

- **Per-chunk** on `PartialChatResponse.moderations()` and `.detections()` inside `onPartialResponse`. Same shape as the final response, scoped to what was flagged in that single chunk. Empty when the chunk carries no match. Useful to react in real time (stop the stream, tag the current token, alert an auditor).
- **Aggregated** on the final response passed to `onCompleteResponse`. All per-chunk matches are merged and detection entries with the same `choice_index` are collapsed into a single entry. The callback signature declares `ChatResponse`, so cast it to `TextChatResponse` to reach the moderation fields.

```java
chatService.chatStreaming(request, new ChatHandler() {
    @Override
    public void onPartialResponse(String chunk, PartialChatResponse partial) {
        System.out.print(chunk);

        // Per-chunk reaction: this chunk triggered one or more detectors.
        if (partial.moderations() != null && !partial.moderations().isEmpty()) {
            partial.moderations().forEach((detector, matches) ->
                System.out.printf("%n[%s] flagged in this chunk: %d match(es)%n", detector, matches.size()));
        }
    }

    @Override
    public void onCompleteResponse(ChatResponse response) {
        // Aggregated view across the whole stream.
        var textResponse = (TextChatResponse) response;
        if (textResponse.moderations() != null) {
            System.out.println("\nModeration results (aggregated): " + textResponse.moderations().keySet());
        }
    }
});
```

---

## Related Resources

- [Chat API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#chat-completions)
- [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx)
- [Basic Chatbot (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot)
- [Streaming Chatbot (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot-streaming)
- [Chatbot with Tools (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot-tools)
- [Chatbot with ToolRegistry (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot-with-tool-registry)
- [Chatbot with Reasoning (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chatbot-reasoning)
- [Chat Inline Moderation (Sample)](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/chat-inline-moderation)