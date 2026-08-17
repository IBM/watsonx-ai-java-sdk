# Chat Inline Moderation Example

This is a simple example that demonstrates how to enable **inline content moderation** on an IBM watsonx.ai chat request and how to mask the detected values on the assistant response using the `Masker` utility.

For standalone text detection outside a chat flow, see the [detection](../detection) sample instead.

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

- `WATSONX_API_KEY` - Your watsonx.ai API key
- `WATSONX_URL` - The base URL for the watsonx.ai service
- `WATSONX_PROJECT_ID` - Your watsonx.ai project id

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_PROJECT_ID=project-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_PROJECT_ID=project-id
```
## How to Run
Use Maven to run the application. 
```bash
mvn package exec:java 
```
