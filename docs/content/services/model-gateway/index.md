---
id: index
title: Model Gateway
---

# Model Gateway

The **Model Gateway** is a proxy layer hosted inside IBM watsonx.ai that provides a single, OpenAI-compatible endpoint to access third-party foundation models (OpenAI, Anthropic, Azure, Mistral, and others) through your IBM Cloud credentials. Instead of managing separate API keys per provider, you authenticate once with your IBM Cloud API key and the gateway routes every request to the underlying model on your behalf.

> **Official documentation:** [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway)

---

## Prerequisites

Before you can use `ModelGatewayService`, the component must be installed and configured in your IBM watsonx.ai instance. This is a one-time setup performed by an **administrator**.

> **Official documentation:** [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui)

### Before you begin

- The Model Gateway component must be installed in your cluster by an instance administrator. See [Installing watsonx.ai](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x) in the IBM Software Hub documentation.
- A **secrets manager** must be configured. You can use either IBM Software Hub vault or an external HashiCorp-compatible vault. If you use an external vault, you must create a connection to it from IBM Software Hub first.

### Step 1 - Open the Model Gateway UI

Open the navigation menu, click **Administration**, and then select **Model Gateway**.

### Step 2 - Add a model provider

A model provider is a connection to an external inference service (OpenAI, Anthropic, Azure, etc.) secured by a secret stored in your vault.

1. Select the **Model provider** tab and click **Add model provider**.
2. Click **Add provider** for one of the available providers. A configuration window opens.
3. Give the connection a name (and an optional description).
4. Select a **secrets manager instance**.
5. Specify the secret to use for the connection. Three options are available:
   - **Select an existing secret** - if you already have a compatible key-value secret.
   - **Create new secret** (IBM Software Hub vault only) - fill in the fields and click **Create**.
   - **Copy a key to create in your external vault** - fill in the details, click **Show JSON**, and use the generated JSON to create the secret in your external vault. Click the **Refresh** icon when done.

   > **Note:** Secrets must be of **Key-value** type and follow the format required by the Model Gateway. Secrets not created through the gateway UI may not work. All secret names must be unique across all providers.

6. Click **Add** to save the connection. Repeat for any additional providers.
7. Click **Next** to proceed to model selection.

### Step 3 - Select models

A list of all models offered by the connected providers is displayed.

1. Select one or more models and click **Next**.
2. Optionally, type **aliases** for the selected models (multiple models can share the same alias, but models from the same provider cannot).
3. Review the list and click **Submit**.

> **Importing a model not listed:** Click **Import model**, select a provider, type the model ID (e.g. `gpt-3.5-turbo-xxxx`), and click **Add**. The model is added to the selectable list.

Once added, the connections and models appear in the **Model provider** tab. Use the search field or sort menu to navigate them.

---

## Available implementations

The table below lists the Model Gateway operations available in this SDK.

| Operation | Class | Status | Page |
|-----------|-------|--------|------|
| Chat completions | `ModelGatewayService` | Available | [Chat](./chat/) |
| Embeddings | - | Not yet implemented | - |
| Audio transcription | - | Not yet implemented | - |

---

## Related Resources

- [IBM watsonx.ai Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=models-model-gateway)
- [Setting up the Model Gateway in the UI](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-setting-up-model-in-ui)
- [Managing the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-managing-model)
- [Inferencing models through the Model Gateway](https://www.ibm.com/docs/en/watsonx/w-and-w/2.4.x?topic=gateway-inferencing-models-through-model)
