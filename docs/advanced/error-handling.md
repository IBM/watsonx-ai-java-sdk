---
layout: default
title: Error Handling
parent: Advanced
nav_order: 5
---

# Error Handling

The SDK provides specific exception types for common watsonx.ai API errors, enabling precise error handling and recovery strategies.

---

## Exception Hierarchy

All watsonx.ai exceptions extend `WatsonxException`, which provides:
- HTTP status code
- Detailed error information from the API response
- Error trace ID for debugging

---

## Specific Exception Types

The SDK automatically maps API error codes to specific exception types:

| Exception | Error Code | Description |
|-----------|------------|-------------|
| `AuthenticationTokenExpiredException` | `authentication_token_expired` | Authentication token has expired (automatically handled by SDK) |
| `AuthorizationRejectedException` | `authorization_rejected` | Authorization was rejected |
| `InvalidInputArgumentException` | `invalid_input_argument` | Invalid input argument provided |
| `InvalidRequestEntityException` | `invalid_request_entity` | Request entity is invalid |
| `JsonTypeErrorException` | `json_type_error` | JSON type error occurred |
| `JsonValidationErrorException` | `json_validation_error` | JSON validation failed |
| `ModelNotSupportedException` | `model_not_supported` | Requested model is not supported |
| `ModelNoSupportForFunctionException` | `model_no_support_for_function` | Model doesn't support the function |
| `TokenQuotaReachedException` | `token_quota_reached` | Token quota has been reached |
| `UserAuthorizationFailedException` | `user_authorization_failed` | User authorization failed |

If an error doesn't match any of these codes, the generic `WatsonxException` is thrown.

{: .note }
> **Automatic Token Refresh**: The SDK automatically handles `AuthenticationTokenExpiredException` by refreshing the token and retrying the request. By default, it retries once when a token expires. This behavior can be configured via the `WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES` environment variable. See [Environment Variables](../environment-variables) for details.

---

## Usage Examples

### Basic Error Handling

Catch specific exceptions to implement targeted recovery strategies:

```java
try {
    ChatResponse response = chatService.chat("Hello!");
} catch (TokenQuotaReachedException e) {
    handleQuotaExceeded();
} catch (ModelNotSupportedException e) {
    useAlternativeModel();
} catch (WatsonxException e) {
    // Handle other errors
    logger.error("Watsonx error: {}", e.getMessage());
}
```