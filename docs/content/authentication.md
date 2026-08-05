---
id: authentication
title: Authentication
---

# Authentication

The SDK uses the `Authenticator` interface as the single abstraction for token-based auth. Two built-in implementations are provided:

| Authenticator | Use when |
|---|---|
| `IBMCloudAuthenticator` | Working against **IBM watsonx.ai on IBM Cloud** - exchanges an API key for an IAM bearer token |
| `CP4DAuthenticator` | Working against **IBM watsonx.ai software** on-premises (CP4D) - three authentication modes available |

Both implementations handle **token caching and automatic renewal** transparently. The SDK fetches a token on the first request, caches it, checks expiry before each subsequent request, and refreshes silently when needed. You never manage token lifecycle manually.

---

## IBM Cloud Authentication

Use `IBMCloudAuthenticator` when working against **IBM watsonx.ai on IBM Cloud**.

### Using `apiKey()` (recommended)

Every service builder accepts `apiKey(String)` as a shorthand. It internally creates an `IBMCloudAuthenticator` with default settings:

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .build();
```

### Using `IBMCloudAuthenticator` explicitly

Use the explicit form when you need to share a single authenticator across multiple services, customize the IAM endpoint, or configure a custom HTTP client for token requests:

```java
IBMCloudAuthenticator authenticator = IBMCloudAuthenticator.builder()
    .apiKey(WATSONX_API_KEY)
    .build();

ChatService chatService = ChatService.builder()
    .authenticator(authenticator)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .build();

EmbeddingService embeddingService = EmbeddingService.builder()
    .authenticator(authenticator)   // reuse the same instance
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-embedding-278m-multilingual")
    .build();
```

The `IBMCloudAuthenticator` also provides a convenience factory for the common case:

```java
IBMCloudAuthenticator authenticator = IBMCloudAuthenticator.withKey(WATSONX_API_KEY);
```

### Builder parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Yes | IBM Cloud API key |
| `baseUrl` | URI | No | IAM token endpoint (default: `https://iam.cloud.ibm.com`) |
| `grantType` | String | No | OAuth grant type (default: `urn:ibm:params:oauth:grant-type:apikey`) |
| `timeout` | Duration | No | Timeout for token requests (default: 60 seconds) |
| `httpClient` | HttpClient | No | Custom HTTP client for token requests |

---

## CP4D Authentication

Use `CP4DAuthenticator` when working against **IBM watsonx.ai software** (on-premises, IBM Cloud Pak for Data). The authenticator supports three authentication modes, selectable via the `authMode` builder parameter.

| `AuthMode` | Credential | Use when |
|------------|------------|----------|
| `LEGACY` *(default)* | `username` + `apiKey` or `password` | Standard CP4D user authentication |
| `IAM` | `username` + `password` | CP4D configured with an external IAM provider |
| `ZEN_API_KEY` | `username` + `apiKey` or `password` | CP4D Zen API key |

### LEGACY mode (default)

The default mode. Authenticates directly against the CP4D instance using a username and either an API key or a password.

```java
// With API key
CP4DAuthenticator authenticator = CP4DAuthenticator.builder()
    .baseUrl("https://your-cp4d-instance.example.com")
    .username("your-username")
    .apiKey(CP4D_API_KEY)
    .build();

// With password
CP4DAuthenticator authenticator = CP4DAuthenticator.builder()
    .baseUrl("https://your-cp4d-instance.example.com")
    .username("your-username")
    .password("your-password")
    .build();
```

### IAM mode

Use this mode when your CP4D instance is federated with an external IAM provider. The authenticator performs a two-step flow: it first obtains an IAM identity token from `/idprovider/v1/auth/identitytoken`, then exchanges it for a CP4D access token via `/v1/preauth/validateAuth`. Only `password` is supported in this mode.

```java
CP4DAuthenticator authenticator = CP4DAuthenticator.builder()
    .baseUrl("https://your-cp4d-instance.example.com")
    .username("your-username")
    .password("your-password")
    .authMode(AuthMode.IAM)
    .build();
```

### ZEN_API_KEY mode

Use this mode for Zen API key authentication.

```java
CP4DAuthenticator authenticator = CP4DAuthenticator.builder()
    .baseUrl("https://your-cp4d-instance.example.com")
    .username("your-username")
    .apiKey(CP4D_API_KEY)
    .authMode(AuthMode.ZEN_API_KEY)
    .build();
```

### Using the authenticator with a service

```java
ChatService chatService = ChatService.builder()
    .authenticator(authenticator)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://your-cp4d-instance.example.com")
    .modelId("ibm/granite-4-h-small")
    .build();
```

### Builder parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `baseUrl` | String / URI | Yes | CP4D instance base URL |
| `username` | String | Yes | CP4D username |
| `apiKey` | String | Conditional | CP4D API key (`LEGACY` and `ZEN_API_KEY` modes - either `apiKey` or `password` required) |
| `password` | String | Conditional | CP4D password (all modes - either `password` or `apiKey` required, mandatory for `IAM` mode) |
| `authMode` | AuthMode | No | Authentication mode: `LEGACY` (default), `IAM`, or `ZEN_API_KEY` |
| `timeout` | Duration | No | Timeout for token requests (default: 60 seconds) |
| `httpClient` | HttpClient | No | Custom HTTP client (useful for SSL configuration) |

---

## Token lifecycle

```
First request
  → Authenticator fetches a token from IAM / CP4D
  → Token cached in memory

Subsequent requests
  → Expiry checked before each call
  → If valid: token attached as Bearer header
  → If expired: new token fetched, cached, request retried automatically

Token expiry during a request
  → SDK catches 401 authentication_token_expired
  → Refreshes token and retries (once by default)
  → Configurable via WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES
```

Both authenticators are **thread-safe** - the same instance can be shared across multiple service clients and called from multiple threads concurrently.

> To tune how many times the SDK retries on token expiry, set the `WATSONX_RETRY_TOKEN_EXPIRED_MAX_RETRIES` environment variable. See [Environment Variables](advanced/environment-variables) for details.