# Watsonx Embedding Example

This is a simple example that demonstrates how to use IBM watsonx.ai to execute text extraction.

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

- `WATSONX_API_KEY` – Your watsonx.ai API key
- `WATSONX_URL` – The base URL for the watsonx.ai service
- `WATSONX_PROJECT_ID` – Your watsonx.ai project id
- `WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID` - The connection id for the Cloud Object Storage (COS) location containing the source documents
- `WATSONX_DOCUMENT_REFERENCE_BUCKET`- The COS bucket where the source documents are stored
- `WATSONX_RESULTS_REFERENCE_CONNECTION_ID` - The connection id for the COS location where results should be written
- `WATSONX_RESULTS_REFERENCE_BUCKET` -  The COS bucket where the extracted results will be saved
- `CLOUD_OBJECT_STORAGE_URL` - The base URL of the Cloud Object Storage service

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=your-api-key
export WATSONX_URL=https://your-watsonx-url
export WATSONX_PROJECT_ID=your-project-id
export WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID=your-doc-conn-id
export WATSONX_DOCUMENT_REFERENCE_BUCKET=your-source-bucket
export WATSONX_RESULTS_REFERENCE_CONNECTION_ID=your-result-conn-id
export WATSONX_RESULTS_REFERENCE_BUCKET=your-result-bucket
export CLOUD_OBJECT_STORAGE_URL=https://s3.<region>.cloud-object-storage.appdomain.cloud
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=your-api-key
set WATSONX_URL=https://your-watsonx-url
set WATSONX_PROJECT_ID=your-project-id
set WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID=your-doc-conn-id
set WATSONX_DOCUMENT_REFERENCE_BUCKET=your-source-bucket
set WATSONX_RESULTS_REFERENCE_CONNECTION_ID=your-result-conn-id
set WATSONX_RESULTS_REFERENCE_BUCKET=your-result-bucket
set CLOUD_OBJECT_STORAGE_URL=https://s3.<region>.cloud-object-storage.appdomain.cloud
```
## How to Run
Use Maven to run the application. 
```bash
mvn package exec:java 
```