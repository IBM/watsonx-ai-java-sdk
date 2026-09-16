---
id: interceptors
title: Interceptors
---

# Interceptors

Every request the SDK sends passes through an internal chain of HTTP interceptors before it reaches watsonx.ai, and every response passes back through the same chain on its way to the caller. This page describes that chain for troubleshooting purposes.

This chain is not a public extension point. No service builder lets you register your own interceptor, and its composition and order are fixed by the SDK.

> Looking for `MessageInterceptor`, `PartialResponseInterceptor`, or `ToolInterceptor`? Those are a separate, application-level mechanism for post-processing chat responses, unrelated to the HTTP-level chain described here. See [Chat - Interceptors](../services/chat-service#interceptors).

---

## The chain

Each service sends its requests through the following interceptors, in this fixed order:

1. **Retry on expired token** - retries the request once, with a freshly obtained token, if the first attempt failed because the authentication token expired.
2. **Authentication** - attaches the `Authorization` header using the configured `Authenticator` (an IBM Cloud API key, an IAM token, or CP4D credentials). Present only when an authenticator is configured. See [Authentication](../authentication).
3. **Retry on transient errors** - retries the request on retryable HTTP status codes (`429`, `502`, `503`, `504`, `520`), with configurable backoff. See [Environment Variables](./environment-variables#retry-configuration).
4. **Logging** - logs the request and response through SLF4J, or delegates to a custom `HttpRequestLogger` / `HttpResponseLogger`. Present only when request or response logging is enabled. See [Request logging](./http-client#request-logging).

Logging sits last in the chain, closest to the actual network call. A retried request passes through it once per attempt, so a request retried twice produces three request/response log entries, not one.

---

## Request correlation

Before the chain runs, every request is assigned a `Watsonx-AI-SDK-Request-Id` header (a random UUID) if it does not already carry one. This id is stable across retries of the same logical request, and it is the value returned by `HttpResponseLog.requestId()` when using a custom response logger, letting you correlate a request with its response (or with a failure) in your own logs.
