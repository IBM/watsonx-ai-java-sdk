# CP4D Authenticator Sample

This sample demonstrates how to authenticate and interact with the watsonx.ai service using the `CP4DAuthenticator` class in the SDK. The sample uses the legacy authentication mode (`AuthMode.LEGACY`), which is the default when no specific `AuthMode` is set. It shows how to configure authentication using a **base URL**, **username**, and **API key**, and then sends a chat message to the watsonx.ai service.

## Overview

With this sample, you will:

- Use the `CP4DAuthenticator` class with the watsonx.ai service.
- Send a user message to a chat model and receive a response.

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