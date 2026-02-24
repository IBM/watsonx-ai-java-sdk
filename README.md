[![Maven Central](https://img.shields.io/maven-central/v/com.ibm.watsonx/watsonx-ai.svg)](https://central.sonatype.com/artifact/com.ibm.watsonx/watsonx-ai)
[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://adoptium.net/temurin/releases/?version=17&os=any&arch=any)
[![Documentation](https://img.shields.io/badge/docs-online-blue)](https://ibm.github.io/watsonx-ai-java-sdk/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# IBM watsonx.ai Java SDK

The **IBM watsonx.ai Java SDK** is an open-source client library that interacts with [IBM watsonx.ai](https://cloud.ibm.com/apidocs/watsonx-ai), an enterprise-grade AI platform for building, training, and deploying AI models at scale.

## Prerequisites

Before getting started, ensure you have:

- A [watsonx.ai](https://www.ibm.com/watsonx/get-started) service instance (IBM Cloud or on-premises)
- Java 17 or higher
- Maven or Gradle

## Installation

Add the SDK to your **Maven** project:

```xml
<dependency>
    <groupId>com.ibm.watsonx</groupId>
    <artifactId>watsonx-ai</artifactId>
    <version>0.18.0</version>
</dependency>
```

Or for **Gradle**:

```gradle
implementation 'com.ibm.watsonx:watsonx-ai:0.18.0'
```

## Quick Start

Build and run your first watsonx.ai-powered chat:

```java
ChatService chatService = ChatService.builder()
    .apiKey(System.getenv("WATSONX_API_KEY"))
    .projectId(System.getenv("WATSONX_PROJECT_ID"))
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-4-h-small")
    .build();

AssistantMessage response = chatService
    .chat("Tell me a joke")
    .toAssistantMessage();

System.out.println(response.content());
```

See the [documentation](https://ibm.github.io/watsonx-ai-java-sdk/) for advanced usage.

## Samples

Examples of usage are available in the [samples/](samples) directory.

## Framework Integrations

The SDK integrates seamlessly with popular Java frameworks:

- **[LangChain4j](https://docs.langchain4j.dev/)** – Native integration for building LLM-powered applications in Java.
- **[Quarkus LangChain4j](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)** – Optimized integration for building LLM-powered applications with Quarkus.
- **[Apache Camel](https://camel.apache.org/components/ibm-watsonx-ai-component.html)** – Integrate AI capabilities into enterprise integration patterns and workflows.

## Contributing

We welcome contributions! Please follow these guidelines when submitting a patch:

**License header**: all source files must include an SPDX header for Apache License 2.0:

```
/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
```

The header and code formatting are applied automatically by running:

```
./mvnw compile
```

**DCO sign-off**: include a `Signed-off-by` line in your commit message:

```
Signed-off-by: Your Name <your.email@example.com>
```

Add it automatically with:

```
git commit -s
```

## License

Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
