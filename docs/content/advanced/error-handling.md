---
id: error-handling
title: Error Handling
---

# Error Handling

The SDK provides a typed exception hierarchy so you can catch exactly the errors you care about and let the rest propagate.

---

## Exception hierarchy

All SDK exceptions extend `WatsonxException`, which in turn extends `RuntimeException`. You never need to declare them in `throws` clauses.

```
RuntimeException
└── WatsonxException              (base - always has statusCode, errorCode, message, traceId)
    ├── AuthenticationTokenExpiredException   ← handled automatically by the SDK
    ├── AuthorizationRejectedException
    ├── InvalidInputArgumentException
    ├── InvalidRequestEntityException
    ├── JsonTypeErrorException
    ├── JsonValidationErrorException
    ├── ModelNotSupportedException
    ├── ModelNoSupportForFunctionException
    ├── TokenQuotaReachedException
    └── UserAuthorizationFailedException
```

`WatsonxException` exposes:

| Method | Type | Description |
|--------|------|-------------|
| `statusCode()` | int | HTTP status code (e.g. `400`, `429`, `500`) |
| `errorCode()` | String | Machine-readable error code from the API (e.g. `"token_quota_reached"`) |
| `getMessage()` | String | Human-readable error description |
| `traceId()` | String | IBM trace ID - include this when reporting issues |

If the API returns an error code that does not map to a specific subclass, the base `WatsonxException` is thrown.

---

## Specific exception types

| Exception | HTTP | Error code | When it occurs |
|-----------|------|------------|---------------|
| `AuthenticationTokenExpiredException` | 401 | `authentication_token_expired` | Token expired mid-request (SDK retries automatically) |
| `AuthorizationRejectedException` | 403 | `authorization_rejected` | API key lacks permission for this operation |
| `InvalidInputArgumentException` | 400 | `invalid_input_argument` | A request parameter has an invalid value |
| `InvalidRequestEntityException` | 400 | `invalid_request_entity` | The request body is malformed or violates constraints |
| `JsonTypeErrorException` | 400 | `json_type_error` | A JSON field has the wrong type |
| `JsonValidationErrorException` | 400 | `json_validation_error` | JSON schema validation failed |
| `ModelNotSupportedException` | 400 | `model_not_supported` | The requested model ID is not available in this region or plan |
| `ModelNoSupportForFunctionException` | 400 | `model_no_support_for_function` | The model does not support the requested capability (e.g. tool calling) |
| `TokenQuotaReachedException` | 429 | `token_quota_reached` | Token quota for the account or project has been reached |
| `UserAuthorizationFailedException` | 403 | `user_authorization_failed` | User-level authorization check failed |

> **Automatic token refresh:** `AuthenticationTokenExpiredException` is caught internally by the SDK, which refreshes the token and retries the request. You will only see this exception if the retry also fails. The retry limit is configurable via `WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES` - see [Environment Variables](./environment-variables).

---

## Usage examples

### Basic error handling

Catch specific subclasses first, then fall back to `WatsonxException` for anything else:

```java
try {
    ChatResponse response = chatService.chat("Hello!");
} catch (TokenQuotaReachedException e) {
    // Quota exceeded - back off and retry later, or switch to a different project
    handleQuotaExceeded();
} catch (ModelNotSupportedException e) {
    // The configured modelId is not available - switch to an alternative
    useAlternativeModel();
} catch (InvalidInputArgumentException e) {
    // A parameter value is wrong - log for debugging
    logger.error("Bad request: {} (traceId={})", e.getMessage(), e.traceId());
} catch (WatsonxException e) {
    // Any other API error
    logger.error("Watsonx error [{}] {}: {} (traceId={})",
        e.statusCode(), e.errorCode(), e.getMessage(), e.traceId());
}
```

### Handling transient errors

The SDK automatically retries `429`, `502`, `503`, `504`, and `520` responses with exponential backoff (see [Environment Variables](./environment-variables)). If retries are exhausted, `WatsonxException` is thrown with the final status code. To implement your own retry on top:

```java
int attempts = 0;
while (attempts < 3) {
    try {
        return chatService.chat("Hello!");
    } catch (WatsonxException e) {
        if (e.statusCode() == 503 && attempts < 2) {
            attempts++;
            Thread.sleep(1000L * attempts);
        } else {
            throw e;
        }
    }
}
```