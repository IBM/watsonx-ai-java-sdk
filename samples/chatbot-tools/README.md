# Watsonx Chatbot Example

This is a simple chatbot project that utilizes tools.
Once the application is running, you can interact with the assistant in the terminal. If you ask the assistant to send an email, it will prompt you for the necessary information (email address, subject, and body) and simulate sending the email using the tool defined in the class `Tools.java`.

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

- `WATSONX_API_KEY` – Your watsonx.ai API key
- `WATSONX_URL` – The base URL for the watsonx.ai service
- `WATSONX_PROJECT_ID` – Your watsonx.ai project id

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=your-api-key
export WATSONX_URL=https://your-watsonx-url
export WATSONX_PROJECT_ID=your-project-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=your-api-key
set WATSONX_URL=https://your-watsonx-url
set WATSONX_PROJECT_ID=your-project-id
```

## How to Run
Use Maven to run the application:

```bash
mvn clean package exec:java
```
