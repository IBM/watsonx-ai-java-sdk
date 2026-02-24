---
layout: default
title: HTTP Client
parent: Advanced
nav_order: 1
permalink: /advanced/http-client/
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

## Logging

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