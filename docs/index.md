---
layout: default
title: Getting Started
nav_order: 1
permalink: /
---

# Getting Started

Welcome to the **IBM watsonx.ai Java SDK** documentation.

## Overview

The IBM watsonx.ai Java SDK is an open-source client library for [IBM watsonx.ai](https://www.ibm.com/watsonx), an enterprise-grade AI platform for building, training, and deploying AI models at scale. It provides a unified Java interface to the watsonx.ai ecosystem and works with both **IBM watsonx.ai for IBM Cloud** and **IBM watsonx.ai software** (on-premises deployments).

| Service | Description |
|---------|-------------|
| **[Chat](chat-service)** | Conversational AI with multi-turn dialogue, streaming, tool calling, vision, and reasoning |
| **[Embedding](embedding-service)** | Dense vector representations for semantic search, similarity, and RAG |
| **[Rerank](rerank-service)** | Relevance scoring and reordering of candidate documents |
| **[Detection](detection-service)** | Identification of harmful content (HAP), PII, and safety violations |
| **[Tokenization](tokenization-service)** | Token counting and analysis for any model |
| **[Foundation Model](foundation-model-service)** | Browse and query the watsonx.ai model catalog |
| **[Text Classification](text-classification-service)** | Document classification from IBM Cloud Object Storage |
| **[Text Extraction](text-extraction-service)** | Structured data extraction from documents in IBM Cloud Object Storage |
| **[Time Series](time-series-service)** | Time series forecasting using IBM Granite TTM models |
| **[Tool](tool-service)** | Server-side utility tools for agentic workflows |
| **[Deployment](deployment-service)** | Chat, text generation, and forecasting via deployed model endpoints |

---

## Prerequisites

Before getting started, ensure you have:

- A [watsonx.ai](https://www.ibm.com/watsonx/get-started) service instance (IBM Cloud or on-premises)
- Java 17 or higher
- Maven or Gradle

---

## Installation

Add the SDK to your Maven project:

```xml
<dependency>
    <groupId>com.ibm.watsonx</groupId>
    <artifactId>watsonx-ai</artifactId>
    <version>{{ site.watsonx_version }}</version>
</dependency>
```

Or for Gradle:

```gradle
implementation 'com.ibm.watsonx:watsonx-ai:{{ site.watsonx_version }}'
```

---

## Quick Start

```java
ChatService chatService = ChatService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .build();

AssistantMessage response = chatService.chat("Tell me a joke").toAssistantMessage();
System.out.println(response.content());
```

---

## Framework Integrations

The IBM watsonx.ai Java SDK is also integrated with popular LLM application frameworks, enabling seamless usage within agentic and AI-powered applications.

Developers can leverage `watsonx.ai` models through:

- [LangChain4j](https://docs.langchain4j.dev/)  
- [Quarkus LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [Apache Camel](https://camel.apache.org/components/ibm-watsonx-ai-component.html)

## Contributing

Contributions are welcome. See the [Contributing Guide](https://github.com/IBM/watsonx-ai-java-sdk#contributing) for details.

## License

This project is licensed under the **Apache License 2.0**. See the [LICENSE](https://github.com/IBM/watsonx-ai-java-sdk/blob/main/LICENSE) file for details.

---

## Resources

- [GitHub Repository](https://github.com/IBM/watsonx-ai-java-sdk)
- [API Documentation](https://cloud.ibm.com/apidocs/watsonx-ai)
- [IBM watsonx.ai Documentation](https://dataplatform.cloud.ibm.com/docs/content/wsj/getting-started/welcome-main.html?context=wx&audience=wdp&locale=en)
- [IBM watsonx.ai Platform](https://www.ibm.com/watsonx)