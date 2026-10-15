# Detection Example

This is a simple example that demonstrates how to use IBM watsonx.ai to detect hate and profanity (HAP) and personally identifiable information (PII).

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

- `WATSONX_API_KEY` - Your watsonx.ai API key
- `WATSONX_URL` - The base URL for the watsonx.ai service
- `WATSONX_PROJECT_ID` - Your watsonx.ai project id
- `WATSONX_GOVERNANCE_INSTANCE_ID` _(optional)_ - Your watsonx.governance service instance id; when set, it is used in place of `WATSONX_PROJECT_ID` to scope detection requests

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_PROJECT_ID=project-id
export WATSONX_GOVERNANCE_INSTANCE_ID=governance-instance-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_PROJECT_ID=project-id
set WATSONX_GOVERNANCE_INSTANCE_ID=governance-instance-id
```
## How to Run
Use Maven to run the application. 
```bash
mvn package exec:java 
```