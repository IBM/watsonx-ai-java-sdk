---
layout: default
title: Environment Variables
parent: Advanced
nav_order: 3
permalink: /advanced/environment-variables/
---

# Environment Variables

The SDK can be tuned at runtime via environment variables, without requiring code changes or recompilation. Variables are read once at startup and cached for the lifetime of the JVM.

---

## Retry

The SDK automatically retries failed requests in two distinct scenarios: **expired authentication tokens** and **transient HTTP errors**. Both behaviors are configurable independently.

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES` | `Integer` | `1` | Maximum retry attempts when a request fails due to an expired authentication token. On each retry, a fresh token is fetched before the request is resent. |
| `WATSONX_RETRY_STATUS_CODES_MAX_RETRIES` | `Integer` | `10` | Maximum retry attempts for transient HTTP errors. Applies to status codes `429`, `503`, `504`, and `520`. |
| `WATSONX_RETRY_STATUS_CODES_BACKOFF_ENABLED` | `Boolean` | `true` | When `true`, retries use exponential backoff — the interval doubles after each failed attempt. Set to `false` for fixed-interval retries. |
| `WATSONX_RETRY_STATUS_CODES_INITIAL_INTERVAL_MS` | `Long` | `20` | Initial retry interval in milliseconds. When exponential backoff is enabled, this is the base interval that doubles with each retry. |

---

## I/O Executor

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `WATSONX_IO_EXECUTOR_THREADS` | Integer | _unset_ | Caps the I/O executor to a fixed-size pool of this many threads. When unset, the default executor is used: virtual threads on Java 21+, a cached thread pool on Java 17–20. Has no effect if a custom [`IOExecutorProvider`](spi.md#executor-spi) is registered. |

> User callbacks (`ChatHandler`, `TextGenerationHandler`) run on a separate executor and are not affected by this variable. See [SPI - Executor](../spi#executor-spi) for details on the executor model.