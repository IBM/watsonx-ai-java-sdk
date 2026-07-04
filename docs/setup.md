---
layout: default
title: Setup & Prerequisites
nav_order: 2
permalink: /setup
---

# Setup & Prerequisites

The code samples throughout this documentation reference a handful of values - an **API key**, a **Project** (or **Space**) **ID**, **Cloud Object Storage** details, and so on. These identify **resources you create in watsonx.ai**. The SDK does not create them for you.

This page maps every value used in the samples to the resource behind it and links to the official IBM documentation that walks you through creating it.

> **Two deployment options.** watsonx.ai runs both as a managed service on **IBM Cloud** and as **on-premises software** on IBM Cloud Pak for Data (CP4D). The steps below describe the **IBM Cloud** setup. On CP4D the core concepts - projects, spaces, deployments, connections - are the same, but you authenticate differently and use your own instance URL as the base URL. See [On-premises (CP4D)](#on-premises-cp4d) for what changes.

---

## What you need at a glance

| Value in the samples | What it is | How to obtain it |
|----------------------|------------|------------------|
| *(service instance)* | A provisioned watsonx.ai instance on IBM Cloud | [Sign up for watsonx.ai](#1-sign-up-for-watsonxai) |
| `WATSONX_API_KEY` | IBM Cloud API key, exchanged for an IAM token | [Create an API key](#2-create-an-ibm-cloud-api-key) |
| `WATSONX_PROJECT_ID` | The project where inference runs | [Find your Project ID](#3-create-a-project-and-find-the-project-id) |
| `SPACE_ID` | A deployment space (alternative to a project) | [Create a deployment space](#4-create-a-deployment-space-optional) |
| `DEPLOYMENT_ID` | An asset deployed to a space | [Deploy an asset](#5-deploy-an-asset-optional) |
| `CONNECTION_ID` + `BUCKET_NAME` | A Cloud Object Storage connection and bucket | [Set up Cloud Object Storage](#6-set-up-cloud-object-storage-cos) |
| `baseUrl` / `CloudRegion` | The regional watsonx.ai endpoint | [Choose your region](#7-choose-your-region-base-url) |
| `modelId` | The foundation model to call | [Browse foundation models](#8-browse-foundation-models) |

Not every service needs every value. [Chat](services/chat-service/), [Embedding](services/embedding-service/), [Rerank](services/rerank-service/), [Tokenization](services/tokenization-service/), and [Detection](services/detection-service/) only need an **API key**, a **Project ID (or Space ID)**, a **region**, and a **model ID**. COS values are required only by the [Text Extraction](services/text-extraction-service/), [Text Classification](services/text-classification-service/), and [Batch](services/batch-service/) services. A `DEPLOYMENT_ID` is required only by the [Deployment Service](services/deployment-service/).

---

## 1. Sign up for watsonx.ai

Create an IBM Cloud account and provision a watsonx.ai service instance. The free plan is enough to get started.

- 📖 [Signing up for watsonx.ai](https://dataplatform.cloud.ibm.com/docs/content/wsj/getting-started/signup-wx.html?context=wx&audience=wdp)

---

## 2. Create an IBM Cloud API key

The SDK authenticates to IBM Cloud by exchanging an API key for an IAM bearer token (handled automatically by `IBMCloudAuthenticator` - see [Authentication](authentication)). Create the key from **Manage → Access (IAM) → API keys** in the IBM Cloud console and store it securely. It is shown only once.

This is the value passed to `apiKey(...)` and referenced as `WATSONX_API_KEY` in the samples.

- 📖 [Managing user API keys](https://cloud.ibm.com/docs/account?topic=account-userapikey)

---

## 3. Create a project and find the Project ID

A **project** is the workspace where inference requests run. After creating one, open its **Manage → General** tab. The **Project ID** is listed there. This is the value for `projectId(...)` / `WATSONX_PROJECT_ID`.

- 📖 [Creating a project](https://dataplatform.cloud.ibm.com/docs/content/wsj/getting-started/projects.html?context=wx&audience=wdp)
- 📖 [Working in projects](https://dataplatform.cloud.ibm.com/docs/content/wsj/manage-data/manage-projects.html?context=wx&audience=wdp)

> Every service accepts either a `projectId` **or** a `spaceId` - you do not need both.

---

## 4. Create a deployment space (optional)

A **deployment space** is a workspace for assets that are ready for testing or production. Most services can use a space instead of a project via `spaceId(...)`. Create one under **Deployments**, then find the **Space ID** in the space's **Manage** tab.

- 📖 [Deployment spaces](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/ml-spaces_local.html?context=wx&audience=wdp)

---

## 5. Deploy an asset (optional)

The [Deployment Service](services/deployment-service/) targets a `DEPLOYMENT_ID` instead of a `modelId`. To obtain one, deploy an asset (a foundation model or a prompt template) into a deployment space. Once deployed, the deployment's unique ID is shown in the space.

- 📖 [Creating online deployments](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/deploy-online.html?context=wx&audience=wdp)

---

## 6. Set up Cloud Object Storage (COS)

The [Text Extraction](services/text-extraction-service/), [Text Classification](services/text-classification-service/), and [Batch](services/batch-service/) services read and write documents in IBM Cloud Object Storage. Two steps are involved:

1. **Provision COS and create a bucket** - this gives you the `BUCKET_NAME` and the `cosUrl` (the regional S3 endpoint, e.g. `https://s3.us-south.cloud-object-storage.appdomain.cloud`).
2. **Create a connection asset** in your project or space - this yields the `CONNECTION_ID` the SDK uses to reference the bucket.

- 📖 [Provisioning Cloud Object Storage and creating buckets](https://cloud.ibm.com/docs/cloud-object-storage?topic=cloud-object-storage-provision)
- 📖 [Creating a Cloud Object Storage connection](https://dataplatform.cloud.ibm.com/docs/content/wsj/manage-data/conn-cos.html?context=wx&audience=wdp)
- 📖 [Adding connections to a project](https://dataplatform.cloud.ibm.com/docs/content/wsj/manage-data/create-conn.html?context=wx&audience=wdp)

---

## 7. Choose your region (base URL)

Every service builder needs a `baseUrl`. On **IBM Cloud**, the SDK provides the `CloudRegion` enum (e.g. `CloudRegion.DALLAS`, which maps to `https://us-south.ml.cloud.ibm.com`) as a convenience, or you can pass the URL string directly. Use the region where your watsonx.ai instance was provisioned.

> On **CP4D**, pass your instance URL as the `baseUrl` instead. The `CloudRegion` enum does not apply - see [On-premises (CP4D)](#on-premises-cp4d).

- 📖 [Endpoint URLs by region (apidocs)](https://cloud.ibm.com/apidocs/watsonx-ai#endpoint-url)

---

## 8. Browse foundation models

The `modelId` passed to a service (e.g. `ibm/granite-4-h-small`) must match a model available in your region and plan. Browse the catalog to see what is supported, or query it programmatically with the [Foundation Model Service](services/foundation-model-service/).

The catalog groups models into two categories, and each is consumed through a different service:

- **Provided with watsonx.ai (pay per token)** - models already hosted in watsonx.ai. Reference them directly by `modelId` through [Chat](services/chat-service/) and the other inference services. No deployment step is required.
- **Deploy on demand (pay by the hour)** - models you first deploy into a deployment space from the **Resource Hub**. Once deployed, they are called by their `DEPLOYMENT_ID` through the [Deployment Service](services/deployment-service/) - see [Deploy an asset](#5-deploy-an-asset-optional).

- 📖 [Supported foundation models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx&audience=wdp)

---

## On-premises (CP4D)

If you run **IBM watsonx.ai software** on Cloud Pak for Data (CP4D) instead of IBM Cloud, the core concepts (projects, spaces, deployments, connections) are the same. What changes is how you connect:

- **Authentication** - use `CP4DAuthenticator` (username plus password, API key, or Zen API key) instead of an IBM Cloud API key with `IBMCloudAuthenticator`. See the [Authentication](authentication#cp4d-authentication) page for the available modes.
- **Base URL** - pass your CP4D instance URL (e.g. `https://cpd.example.com`) as the `baseUrl`. The `CloudRegion` enum and the regional endpoints from step 7 do not apply.
- **Creating resources** - projects, deployment spaces, deployments, and Cloud Object Storage connections are created in your CP4D web console. The resulting identifiers (Project ID, Space ID, Deployment ID, Connection ID) are used exactly the same way as on IBM Cloud.

Provisioning steps are specific to your installation, so refer to your CP4D administrator or the IBM Cloud Pak for Data documentation for details.

---

## Next steps

- [Authentication](authentication) - configure `IBMCloudAuthenticator` / `CP4DAuthenticator`.
- [Services](services/) - call your first service.
- [watsonx.ai REST API reference](https://cloud.ibm.com/apidocs/watsonx-ai) - the underlying API this SDK wraps.
