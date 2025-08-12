# Watsonx Embedding Example

This example demonstrates how to use IBM watsonx.ai to perform text extraction from documents stored in IBM Cloud Object Storage (COS).

## Prerequisites

Before running the application, set the following environment variables or create a `.env` file in the project root:

| Variable                                      | Required | Description |
|-----------------------------------------------|----------|-------------|
| `WATSONX_API_KEY`                             | Yes      | watsonx.ai API key |
| `WATSONX_URL`                                 | Yes      | The base URL for the watsonx.ai service |
| `WATSONX_PROJECT_ID`                          | Yes      | watsonx.ai project id |
| `WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID`    | Yes      | The connection id for the COS location containing the **source documents** |
| `WATSONX_DOCUMENT_REFERENCE_BUCKET`           | Yes      | The COS bucket where the source documents are stored |
| `WATSONX_RESULTS_REFERENCE_CONNECTION_ID`     | Yes      | The connection id for the COS location where **results** should be written |
| `WATSONX_RESULTS_REFERENCE_BUCKET`            | Yes      | The COS bucket where the extracted results will be saved |
| `CLOUD_OBJECT_STORAGE_URL`                    | Yes      | The base URL of the IBM Cloud Object Storage service (e.g. `https://s3.<region>.cloud-object-storage.appdomain.cloud`) |

> **Note:**
> - For more information about how to create the *IBM Cloud Object Storage connection*, see the [documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/manage-data/conn-cos.html?context=wx&locale=en).
> - To find the *bucket name* and COS endpoint for your region, see the [IBM Cloud Object Storage buckets documentation](https://cloud.ibm.com/docs/cloud-object-storage?topic=cloud-object-storage-endpoints).
> - For more information, see [IBM Text Extraction documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html?context=wx&audience=wdp). 

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_PROJECT_ID=project-id
export WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID=doc-conn-id
export WATSONX_DOCUMENT_REFERENCE_BUCKET=source-bucket
export WATSONX_RESULTS_REFERENCE_CONNECTION_ID=result-conn-id
export WATSONX_RESULTS_REFERENCE_BUCKET=result-bucket
export CLOUD_OBJECT_STORAGE_URL=https://s3.<region>.cloud-object-storage.appdomain.cloud
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_PROJECT_ID=project-id
set WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID=doc-conn-id
set WATSONX_DOCUMENT_REFERENCE_BUCKET=source-bucket
set WATSONX_RESULTS_REFERENCE_CONNECTION_ID=result-conn-id
set WATSONX_RESULTS_REFERENCE_BUCKET=result-bucket
set CLOUD_OBJECT_STORAGE_URL=https://s3.<region>.cloud-object-storage.appdomain.cloud
```

## How to Run
Use Maven to run the application:

```bash
mvn package exec:java
```