---
layout: default
title: Service Provider Interface
parent: Advanced
nav_order: 2
permalink: /advanced/spi/
---

# Service Provider Interface

The IBM watsonx.ai Java SDK exposes several Service Provider Interfaces that allow framework integrators to replace or customize its core infrastructure — HTTP transport, thread management, and JSON serialization — without changing any application code. All SPIs are resolved at runtime via the Java `ServiceLoader` mechanism.

---

## REST Client SPI

Every service delegates HTTP communication to an abstract `RestClient`. The concrete implementation is discovered at startup via `ServiceLoader`; if none is registered, the SDK falls back to its built-in `DefaultRestClient` (based on the Java `HttpClient`).

### Service to REST client mapping

| Service | REST client |
|---------|-------------|
| `ChatService` | `ChatRestClient` |
| `EmbeddingService` | `EmbeddingRestClient` |
| `RerankService` | `RerankRestClient` |
| `TokenizationService` | `TokenizationRestClient` |
| `DetectionService` | `DetectionRestClient` |
| `TextClassificationService` | `TextClassificationRestClient` |
| `TextExtractionService` | `TextExtractionRestClient` |
| `TimeSeriesService` | `TimeSeriesRestClient` |
| `FoundationModelService` | `FoundationModelRestClient` |
| `ToolService` | `ToolRestClient` |
| `DeploymentService` | `DeploymentRestClient` |

Each `RestClient` performs the lookup this way:

```java
static ChatRestClient.Builder builder() {
    return ServiceLoader.load(ChatRestClientBuilderFactory.class).findFirst()
        .map(Supplier::get)
        .orElse(DefaultRestClient.builder());
}
```

### Providing a custom REST client

The recommended pattern is to keep the factory and builder as **static nested classes** inside the custom `RestClient`. The example below shows a full implementation for `ChatRestClient` using Quarkus and the RESTEasy Reactive client.

#### Step 1 — Define a JAX-RS interface for the API

Declare the API endpoints using JAX-RS annotations. The SDK's request/response types can be used directly:

```java
@Path("/ml/v1")
public interface ChatRestApi {

    @POST
    @Path("text/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ChatResponse chat(
        @HeaderParam("X-Request-Id") String requestId,
        @HeaderParam("X-Global-Transaction-Id") String transactionId,
        @QueryParam("version") String version,
        TextChatRequest request);

    @POST
    @Path("text/chat_stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    Multi<String> chatStreaming(
        @HeaderParam("X-Request-Id") String requestId,
        @HeaderParam("X-Global-Transaction-Id") String transactionId,
        @QueryParam("version") String version,
        TextChatRequest request);
}
```

#### Step 2 — Implement the `RestClient`

Extend `ChatRestClient`, build the framework-native client in the constructor using the inherited fields, and implement the abstract methods:

```java
public final class QuarkusChatRestClient extends ChatRestClient {

    private final ChatRestApi client;

    QuarkusChatRestClient(Builder builder) {
        super(builder);
        try {
            client = QuarkusRestClientBuilder.newBuilder()
                .baseUrl(URI.create(baseUrl).toURL())
                .clientHeadersFactory(new BearerTokenHeaderFactory(authenticator))
                .connectTimeout(timeout.toSeconds(), TimeUnit.SECONDS)
                .readTimeout(timeout.toSeconds(), TimeUnit.SECONDS)
                .build(ChatRestApi.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ChatResponse chat(String transactionId, TextChatRequest textChatRequest) {
        // Quarkus implementation
        // ...
    }

    @Override
    public CompletableFuture<ChatResponse> chatStreaming(
            String transactionId,
            TextChatRequest textChatRequest,
            ChatClientContext context,
            ChatHandler handler) {
        // Quarkus implementation
        // ...
    }

    // Factory and Builder as static nested classes
    public static final class QuarkusChatRestClientBuilderFactory
            implements ChatRestClientBuilderFactory {
        @Override
        public Builder get() {
            return new QuarkusChatRestClient.Builder();
        }
    }

    static final class Builder extends ChatRestClient.Builder {
        @Override
        public ChatRestClient build() {
            return new QuarkusChatRestClient(this);
        }
    }
}
```

#### Step 3 — Register via `ServiceLoader`

Create the file:

```
META-INF/services/com.ibm.watsonx.ai.chat.ChatRestClient$ChatRestClientBuilderFactory
```

Note the `$` separator — this is the JVM convention for nested class names in `ServiceLoader` registration files. The file content is the fully qualified name of the factory:

```
io.quarkiverse.langchain4j.watsonx.runtime.client.impl.QuarkusChatRestClient$QuarkusChatRestClientBuilderFactory
```

Once registered, any `ChatService` built in that runtime will automatically use your implementation.

### Real-world example: Quarkus integration

The `quarkus-langChain4j-watsonx` integration uses this SPI to replace the default Java `HttpClient` with Quarkus's reactive RESTEasy client. This allows the SDK to participate in Quarkus's managed thread model, reactive pipelines (Mutiny), and GraalVM native compilation. A `*BuilderFactory` is registered for each service via CDI — the consuming application uses the same `ChatService`, `EmbeddingService`, etc. API without any modification.

> See [quarkus-langchain4j watsonx](https://github.com/quarkiverse/quarkus-langchain4j/tree/main/model-providers/watsonx/runtime/src/main/java/io/quarkiverse/langchain4j/watsonx/runtime/client) for the complete reference implementation.

---

## Executor SPI

The SDK uses three distinct executors internally, each replaceable independently via `ServiceLoader`.

| SPI interface | Default behavior | Used for |
|---------------|-----------------|---------|
| `CpuExecutorProvider` | `ForkJoinPool.commonPool()` | CPU-bound tasks: JSON parsing, data transformation |
| `IOExecutorProvider` | Single-threaded (configurable via `WATSONX_IO_EXECUTOR_THREADS`) | HTTP response processing, SSE stream parsing |
| `CallbackExecutorProvider` | Virtual threads (Java 21+), cached thread pool (Java 17–20) | User callbacks in `ChatHandler` and `TextGenerationHandler` |

The three executors are intentionally separate to prevent user callback code from blocking the SSE parsing thread, and to keep CPU-bound work off the I/O thread.

The same pattern applies to `CpuExecutorProvider` and `IOExecutorProvider`. The executor is loaded once at startup and cached for the lifetime of the JVM.

---

## JSON SPI

By default, the SDK uses **Jackson** for JSON serialization and deserialization, configured with `snake_case` property naming and `NON_NULL` inclusion. The `JsonProvider` SPI allows replacing this with any other JSON library.

```java
public interface JsonProvider {
    <T> T fromJson(String json, Class<T> clazz);
    <T> T fromJson(String json, TypeToken<T> typeToken);
    String toJson(Object object);
    String prettyPrint(Object object);
    boolean isValidObject(String json);
}
```

`TypeToken<T>` is a utility class provided by the SDK to capture generic type information at runtime, used for deserializing parameterized types like `List<ChatMessage>` or `DetectionResponse<DetectionTextResponse>`.