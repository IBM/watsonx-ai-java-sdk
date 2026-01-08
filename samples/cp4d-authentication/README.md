# CP4D Authenticator Sample

This sample demonstrates how to authenticate and interact with the watsonx.ai service using the `CP4DAuthenticator` class in the SDK. The sample uses the legacy authentication mode (`AuthMode.LEGACY`), which is the default when no specific `AuthMode` is set. It shows how to configure authentication using a **base URL**, **username**, and **API key**, and then sends a chat message to the watsonx.ai service.

## Overview

With this sample, you will:

- Use the `CP4DAuthenticator` class with the watsonx.ai service.
- Send a user message to a chat model and receive a response.
- Learn how to disable SSL certificate verification for development environments with self-signed certificates.

## Disabling SSL Certificate Verification

When working with CP4D instances that use self-signed certificates, you need to disable SSL certificate verification. The SDK provides two approaches:

### Option 1: Using `verifySsl(false)`

The simplest approach is to use the built-in `verifySsl()` method available on both the authenticator and service builders:

```java
ChatService chatService = ChatService.builder()
    .baseUrl(baseUrl)
    .modelId("ibm/granite-3-2-8b-instruct")
    .projectId(projectId)
    .verifySsl(false)  // Disable SSL verification
    .authenticator(
        CP4DAuthenticator.builder()
            .baseUrl(baseUrl)
            .username(username)
            .apiKey(apiKey)
            .verifySsl(false)  // Disable SSL verification
            .build()
    ).build();
```

### Option 2: Using a Custom HttpClient

Alternatively, you can create a custom `HttpClient` with a trust-all SSL context (as shown in the sample code):

```java
HttpClient httpClient = HttpClient.newBuilder()
    .sslContext(createTrustAllSSLContext())
    .executor(ExecutorProvider.ioExecutor())
    .build();

ChatService chatService = ChatService.builder()
    .baseUrl(baseUrl)
    .modelId("ibm/granite-3-2-8b-instruct")
    .projectId(projectId)
    .httpClient(httpClient)
    .authenticator(
        CP4DAuthenticator.builder()
            .baseUrl(baseUrl)
            .username(username)
            .apiKey(apiKey)
            .httpClient(httpClient)
            .build()
    ).build();
```

## Prerequisites

Before running the application, you need to set the following environment variables or create a .env file in the project root:

- `CP4D_URL` – The base URL for the IBM Cloud Pak for Data instance.
- `CP4D_USERNAME` – Your CP4D username for authentication.
- `CP4D_API_KEY` – Your CP4D API key for authentication.
- `CP4D_PROJECT_ID` – The CP4D project id that contains the model you want to interact with.

### Example (Linux/macOS):

```bash
export CP4D_URL=https://your-cp4d-instance-url
export CP4D_USERNAME=your-username
export CP4D_API_KEY=your-api-key
export CP4D_PROJECT_ID=your-project-id
```

### Example (Windows CMD):

```cmd
set CP4D_URL=https://your-cp4d-instance-url
set CP4D_USERNAME=your-username
set CP4D_API_KEY=your-api-key
set CP4D_PROJECT_ID=your-project-id
```

## How to Run

Use Maven to build and run the sample:

```bash
mvn package exec:java
```