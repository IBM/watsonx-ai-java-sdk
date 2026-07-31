---
id: document-processing
title: Document Processing
---

# Document Processing

The document processing services work together to extract and classify structured information from documents stored in **IBM Cloud Object Storage (COS)**. A `CONNECTION_ID` and `BUCKET_NAME` are required in addition to the standard credentials - see [Setup & Prerequisites](../../setup#6-set-up-cloud-object-storage-cos).

## Services

| Service | What it does |
|---------|-------------|
| **[Create Schema](schema/create-schema-service)** | Generate a key-value extraction schema from sample documents |
| **[Improve Schema](schema/improve-schema-service)** | Refine an existing schema using additional examples |
| **[Merge Schema](schema/merge-schema-service)** | Consolidate multiple schemas into one |
| **[Text Extraction](text-extraction-service)** | Extract structured key-value pairs from documents in COS |
| **[Text Classification](text-classification-service)** | Classify documents stored in COS |

## Typical workflow

1. **Create a schema** from sample documents using `CreateSchemaService`. Optionally refine it with `ImproveSchemaService` or combine multiple schemas with `MergeSchemaService`.
2. **Extract** structured key-value pairs from documents using `TextExtractionService` with the generated schema.
3. **Classify** documents by type using `TextClassificationService` with the same schema.
