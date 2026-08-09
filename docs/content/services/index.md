---
id: index
title: Services
---

# Services

Every service in the SDK follows the same **builder pattern**: configure it once with credentials, endpoint, and (where applicable) a model ID, then call its methods to interact with the API. Service instances are **thread-safe** and intended to be shared - create one per service per application, not one per request.

## Inference

| Service | What it does |
|---------|-------------|
| **[Chat](chat-service)** | Conversational AI - synchronous and streaming, tool calling, vision, reasoning, structured output |
| **[Embedding](embedding-service)** | Convert text to dense vectors for semantic search, similarity, and RAG |
| **[Rerank](rerank-service)** | Score and sort a list of candidate passages against a query |
| **[Time Series](time-series-service)** | Forecast time series data using IBM Granite TTM models |
| **[Deployment](deployment-service)** | Target a deployed model by `deploymentId` for chat and forecasting |
| **[Model Gateway](model-gateway)** | Chat with third-party models (OpenAI, Anthropic, etc.) via a unified IBM-managed gateway |
| **[Foundation Model](foundation-model-service)** | Browse the model catalog - filter by provider, task, function, lifecycle |

## Document processing

These services read from and write to **IBM Cloud Object Storage (COS)**. A `CONNECTION_ID` and `BUCKET_NAME` are required in addition to the standard credentials - see [Setup & Prerequisites](../setup#6-set-up-cloud-object-storage-cos).

| Service | What it does |
|---------|-------------|
| **[Create Schema](document-processing/schema/create-schema-service)** | Generate a key-value extraction schema from sample documents |
| **[Improve Schema](document-processing/schema/improve-schema-service)** | Refine an existing schema using additional examples |
| **[Merge Schema](document-processing/schema/merge-schema-service)** | Consolidate multiple schemas into one |
| **[Text Extraction](document-processing/text-extraction-service)** | Extract structured key-value pairs from documents in COS |
| **[Text Classification](document-processing/text-classification-service)** | Classify documents stored in COS |

## Utilities

| Service | What it does |
|---------|-------------|
| **[Tool](tool-service)** | Invoke IBM-hosted utility tools (search, weather, Python interpreter, RAG) |
| **[Tokenization](tokenization-service)** | Count tokens and retrieve the individual token strings for a given model |
| **[Detection](detection-service)** | Detect harmful content (HAP), PII, and safety violations |
| **[File](file-service)** | Upload, list, retrieve, and delete files used as batch job inputs |
| **[Batch](batch-service)** | Submit high-volume asynchronous inference jobs from JSONL files |
