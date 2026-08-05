---
id: setup
title: Setup & Prerequisites
---

# Setup & Prerequisites

The code samples throughout this documentation reference a handful of values - an **API key**, a **Project** (or **Space**) **ID**, **Cloud Object Storage** details, and so on. These identify **resources you create in watsonx.ai**. The SDK does not create them for you.

This page maps every value used in the samples to the resource behind it and links to the official IBM documentation that walks you through creating it.

> **Two deployment options.** watsonx.ai runs both as a managed service on **IBM Cloud** and as **on-premises software** on IBM Cloud Pak for Data (CP4D). The steps below describe the **IBM Cloud** setup. On CP4D the core concepts (projects, spaces, deployments, connections) are the same, but you authenticate differently and use your own instance URL as the base URL. See [On-premises (CP4D)](#on-premises-cp4d) for what changes.

---

## What you need

| What it is | How to obtain it |
|------------|------------------|
| A provisioned watsonx.ai instance on IBM Cloud | [Sign up for watsonx.ai](#1-sign-up-for-watsonxai) |
| IBM Cloud API key (`WATSONX_API_KEY`), exchanged for an IAM token | [Create an API key](#2-create-an-ibm-cloud-api-key) |
| The project where inference runs (`WATSONX_PROJECT_ID`) | [Find your Project ID](#3-create-a-project--find-the-project-id) |
| A deployment space - alternative to a project (`SPACE_ID`) | [Create a deployment space](#4-create-a-deployment-space-optional) |
| An asset deployed to a space (`DEPLOYMENT_ID`) | [Deploy an asset](#5-deploy-an-asset-optional) |
| A Cloud Object Storage connection and bucket (`CONNECTION_ID` + `BUCKET_NAME`) | [Set up Cloud Object Storage](#6-set-up-cloud-object-storage-cos) |
| Gateway component + provider secrets configured by an admin | [Set up Model Gateway](#7-set-up-model-gateway-optional) |
| The regional watsonx.ai endpoint (`baseUrl` / `CloudRegion`) | [Choose your region](#8-choose-your-region-base-url) |
| The foundation model to call (`modelId`) | [Browse foundation models](#9-browse-foundation-models) |

Not every service needs every value:

- **`WATSONX_API_KEY` + `WATSONX_PROJECT_ID` (or `SPACE_ID`) + `baseUrl` + `modelId`** - required by most services: [Chat](services/chat-service/), [Embedding](services/embedding-service/), [Rerank](services/rerank-service/), [Tokenization](services/tokenization-service/), [Detection](services/detection-service/).
- **`CONNECTION_ID` + `BUCKET_NAME`** - required only by [Text Extraction](services/document-processing/text-extraction-service/), [Text Classification](services/document-processing/text-classification-service/), and [Batch](services/batch-service/).
- **`DEPLOYMENT_ID`** - required only by the [Deployment Service](services/deployment-service/).
- **Model Gateway** - requires a one-time admin setup. No extra values are needed in your code beyond the standard `WATSONX_API_KEY`.

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

## 3. Create a project & find the Project ID

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

The [Text Extraction](services/document-processing/text-extraction-service/), [Text Classification](services/document-processing/text-classification-service/), and [Batch](services/batch-service/) services read and write documents in IBM Cloud Object Storage. Two steps are involved:

1. **Provision COS and create a bucket** - this gives you the `BUCKET_NAME` and the `cosUrl` (the regional S3 endpoint, e.g. `https://s3.us-south.cloud-object-storage.appdomain.cloud`).
2. **Create a connection asset** in your project or space - this yields the `CONNECTION_ID` the SDK uses to reference the bucket.

- 📖 [Provisioning Cloud Object Storage and creating buckets](https://cloud.ibm.com/docs/cloud-object-storage?topic=cloud-object-storage-provision)
- 📖 [Creating a Cloud Object Storage connection](https://dataplatform.cloud.ibm.com/docs/content/wsj/manage-data/conn-cos.html?context=wx&audience=wdp)
- 📖 [Adding connections to a project](https://dataplatform.cloud.ibm.com/docs/content/wsj/manage-data/create-conn.html?context=wx&audience=wdp)

---

## 7. Set up Model Gateway (optional)

The [Model Gateway Service](services/model-gateway/) routes requests to third-party models (OpenAI, Anthropic, Azure, Mistral, and others) through a proxy component hosted inside your IBM watsonx.ai instance. Before you can call it, an **instance administrator** must install the component and configure at least one model provider.

### Before you begin

- The Model Gateway component must be installed in your cluster. See [Installing watsonx.ai](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x) in the IBM Software Hub documentation.
- A **secrets manager** must be configured (IBM Software Hub vault or an external HashiCorp-compatible vault).

### Admin setup steps

1. Open the navigation menu, click **Administration**, and select **Model Gateway**.
2. On the **Model provider** tab, click **Add model provider** and follow the wizard to connect a provider (OpenAI, Anthropic, Azure, etc.) and store its API key in the secrets manager.
3. Select one or more models to expose and optionally assign aliases.
4. Click **Submit**. The gateway is now ready to accept requests.

> Once the admin setup is complete, end users need no additional credentials or identifiers - the standard `WATSONX_API_KEY` and `WATSONX_PROJECT_ID` are sufficient.

- 📖 [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui)
- 📖 [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway)

---

## 8. Choose your region (base URL)

Every service builder needs a `baseUrl`. On **IBM Cloud**, the SDK provides the `CloudRegion` enum (e.g. `CloudRegion.DALLAS`, which maps to `https://us-south.ml.cloud.ibm.com`) as a convenience, or you can pass the URL string directly. Use the region where your watsonx.ai instance was provisioned.

> On **CP4D**, pass your instance URL as the `baseUrl` instead. The `CloudRegion` enum does not apply - see [On-premises (CP4D)](#on-premises-cp4d).

- 📖 [Endpoint URLs by region (apidocs)](https://cloud.ibm.com/apidocs/watsonx-ai#endpoint-url)

---

## 9. Browse foundation models

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
- [REST API reference](https://cloud.ibm.com/apidocs/watsonx-ai) - the underlying API this SDK wraps.
