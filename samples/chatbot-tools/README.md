# Watsonx Chatbot Example

This is a simple chatbot project that utilizes tools.
Once the application is running, you can interact with the assistant in the terminal. If you ask the assistant to send an email, it will prompt you for the necessary information (email address, subject, and body) and simulate sending the email using the tool defined in the class `Tools.java`.

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

| Variable              | Required | Description |
|-----------------------|----------|-------------|
| `WATSONX_API_KEY`     | Yes      | watsonx.ai API key |
| `WATSONX_URL`         | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_PROJECT_ID`  | Yes      | watsonx.ai project id |
| `WATSONX_MODEL_ID`    | No       | watsonx.ai model id. If not set, a default LLM will be used. When setting this variable, use a value from the `API model ID` column in the [IBM provided models list](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx#ibm-provided). |

> **Important:** the chosen model must support the use of Tools.

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_PROJECT_ID=project-id
# Optional: export WATSONX_MODEL_ID=model-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_PROJECT_ID=project-id
:: Optional: set WATSONX_MODEL_ID=model-id
```

## How to Run
Use Maven to run the application:

```bash
mvn package exec:java
```
