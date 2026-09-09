---
id: http-client
title: HTTP Client
---

# HTTP Client

By default, the **IBM watsonx.ai Java SDK** uses the Java built-in `HttpClient`. No additional dependencies are required. Every service builder exposes a `httpClient(HttpClient)` method that lets you replace or configure the underlying client when the defaults are not sufficient.

---

## Default Behavior

If no custom `HttpClient` is provided, the SDK creates one automatically. All services share this default configuration unless overridden.

---

## Customizing the HTTP Client

Pass a pre-configured `HttpClient` instance to the service builder. The SDK will use it for all requests made by that service instance.

```java
HttpClient httpClient = HttpClient.newBuilder()
    ...
    .build();

ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .httpClient(httpClient)
    .build();
```

---

## Common Customizations

### SSL / TLS configuration

On-premises deployments (CP4D) often use self-signed or private CA certificates. Configure a custom `SSLContext` to trust them:

```java
KeyStore trustStore = KeyStore.getInstance("JKS");
try (InputStream is = new FileInputStream("truststore.jks")) {
    trustStore.load(is, "changeit".toCharArray());
}

TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
tmf.init(trustStore);

SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(null, tmf.getTrustManagers(), null);

HttpClient httpClient = HttpClient.newBuilder()
    .sslContext(sslContext)
    .build();
```

> For CP4D deployments, pass the same `HttpClient` to both the `CP4DAuthenticator` and the service builder so that token requests and inference requests use the same TLS configuration. See [Authentication](../authentication) for details.

### Disabling SSL verification

For development or testing against self-signed certificates without a truststore, you can disable SSL verification via the service builder directly:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://my-internal-instance.example.com")
    .modelId("ibm/granite-4-h-small")
    .verifySsl(false)
    .build();
```

### Proxy configuration

Route requests through an HTTP proxy:

```java
HttpClient httpClient = HttpClient.newBuilder()
    .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 8080)))
    .build();
```

### HTTP/2 configuration

The Java `HttpClient` defaults to HTTP/2 with HTTP/1.1 fallback. To force HTTP/1.1:

```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .build();
```

---

## Request Timeout

In addition to the `HttpClient`-level `connectTimeout`, the SDK exposes a per-service `timeout` that controls the maximum duration to wait for a complete response:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .timeout(Duration.ofMinutes(5))   // default: 60 seconds
    .build();
```

The `timeout` applies to each individual request. For streaming responses, it governs the total time allowed for the stream to complete.

---

## Request logging

Request and response payloads can be logged for debugging:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .logRequests(true)
    .logResponses(true)
    .build();
```

With `logRequests(true)` / `logResponses(true)`, the SDK writes a human-readable log line through SLF4J at `INFO`. This default behavior already covers most debugging needs:

| What | Behavior |
|------|----------|
| Log level | `INFO`, through the `LoggerInterceptor` logger |
| Format | A multi-line block with method, URL, headers, status code, and a pretty-printed JSON body |
| Secret masking | Fields such as `api_key`, `password`, `secret`, `access_token`, and `refresh_token` are replaced with `***` |
| Base64 payloads | Image and audio data URIs are truncated instead of being printed in full |
| Correlation | Every request carries a `Watsonx-AI-SDK-Request-Id` header, echoed in the corresponding response log line |

Enable the `INFO` level for that logger name in your SLF4J backend (Logback, Log4j2, and so on) to see these log lines.

### Custom loggers

For structured logging, forwarding to a telemetry pipeline, or any format other than the default text block, pass an `HttpRequestLogger` or `HttpResponseLogger` instead of a boolean. Both are functional interfaces, so a lambda is enough:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .logRequests(entry -> telemetry.record("request", entry.request().uri(), entry.body()))
    .logResponses(entry -> telemetry.record("response", entry.statusCode(), entry.body()))
    .build();
```

`entry.body()` arrives already masked and truncated the same way as the default log line, so secrets are never passed to your logger in clear text. `HttpResponseLog` also exposes `requestId()`, `statusCode()`, `headers()`, `uri()`, and `error()`.

### Custom level

`logRequests` / `logResponses` accept an optional `Level` alongside the custom logger. It gates only that logger, independently of the fixed `INFO` level used for the SDK's own default log lines:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .logRequests(entry -> auditLog.write(entry), Level.DEBUG)
    .build();
```

The custom logger above only fires once the `LoggerInterceptor` logger is enabled for `DEBUG` in your SLF4J backend, whether or not you also want the SDK's own `INFO` log lines enabled.

> See [Interceptors](./interceptors) for how request/response logging fits into the SDK's internal HTTP interceptor chain.