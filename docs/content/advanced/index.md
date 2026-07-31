---
id: index
title: Advanced
---

# Advanced

This section covers lower-level aspects of the SDK for developers who need to customize its behavior beyond the standard configuration options.

## What's in this section

| Topic | What it covers |
|-------|---------------|
| **[HTTP Client](http-client)** | Replace or configure the underlying `java.net.http.HttpClient` - TLS, proxies, timeouts, HTTP/2, and request logging |
| **[Error Handling](error-handling)** | Exception hierarchy, specific error codes, and retry patterns |
| **[Environment Variables](environment-variables)** | Runtime tuning of retry behavior and I/O thread pool without code changes |
| **[SPI](spi)** | Replace the HTTP transport, thread executor, or JSON provider via `ServiceLoader` - the extension points used by Quarkus and other frameworks |

## Common customization patterns

**Custom TLS for CP4D** - pass a configured `HttpClient` to both the authenticator and the service builder so all requests use the same SSL context. See [HTTP Client - SSL / TLS configuration](http-client#ssl--tls-configuration).

**Tuning retries in production** - the SDK retries on token expiry and transient errors (`429`, `503`, `504`, `520`) by default. Adjust the limits and backoff via environment variables. See [Environment Variables - Retry configuration](environment-variables#retry-configuration).

**Quarkus / framework integration** - replace the default `java.net.http.HttpClient` transport with a framework-native REST client via the REST Client SPI. See [SPI - REST Client SPI](spi#rest-client-spi).
