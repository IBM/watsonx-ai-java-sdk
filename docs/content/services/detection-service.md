---
id: detection-service
title: Detection Service
---


# Detection Service

The `DetectionService` provides functionality to analyze text content using **IBM watsonx.ai detection APIs**. It supports identification of harmful content, personally identifiable information (PII), and other types of sensitive or unsafe text through configurable detectors.

## Quick Start

```java
DetectionService detectionService = DetectionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .governanceInstanceId(WATSONX_GOVERNANCE_INSTANCE_ID)
    .baseUrl(CloudRegion.DALLAS)
    .build();

var request = DetectionTextRequest.builder()
    .input("I kill you")
    .detectors(Hap.ofThreshold(0.3))
    .build();

DetectionResponse<DetectionTextResponse> detections = detectionService.detect(request);
detections.detections().forEach(d -> System.out.println(d.detection() + " [score=" + d.score() + "]: " + d.text()));
// → has_HAP [score=0.97]: I kill you
```

---

## Overview

The `DetectionService` enables you to:

- Detect hate speech, abuse, and profanity (**HAP**) in text.
- Identify personally identifiable information (**PII**) such as phone numbers, emails, and names.
- Apply content safety checks using **IBM Granite Guardian**.
- Combine multiple detectors in a single request.
- Configure detection thresholds for fine-grained control.

> **Two modes are available for content screening in the SDK:**
> - **Standalone detection** (this service) - analyze arbitrary text with `Hap`, `Pii`, and `GraniteGuardian` outside a chat flow (e.g. moderating user-generated content, log analysis, offline scans).
> - **Inline chat moderation** via [`ChatService`](/services/chat-service#content-moderation) - attach a `ChatModeration` to a `ChatRequest` and let detectors run *during* the chat call. Results come back on the `TextChatResponse`.
>
> The two APIs are independent: the same detector categories but different request types (`DetectionTextRequest` vs `ChatRequest`), different response shapes, and different classes (`com.ibm.watsonx.ai.detection.detector.*` vs `com.ibm.watsonx.ai.chat.ChatModeration.*`).

---

## Service Configuration

### Basic Setup

To start using the Detection Service, create a `DetectionService` instance with the minimum required configuration.

```java
DetectionService detectionService = DetectionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .governanceInstanceId(WATSONX_GOVERNANCE_INSTANCE_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com")
    .build();
```

### Using CloudRegion

Instead of manually specifying the `baseUrl`, you can use the `CloudRegion` to automatically configure the correct endpoint.

```java
DetectionService detectionService = DetectionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .governanceInstanceId(WATSONX_GOVERNANCE_INSTANCE_ID)
    .baseUrl(CloudRegion.DALLAS)
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `projectId` | String | Conditional | Project ID where the service is deployed |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |
| `governanceInstanceId` | String | No | The GUID of your watsonx.governance service instance |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

---

## Detectors

Detectors are the building blocks of a detection request. Each detector targets a specific category of content and can be configured independently. Multiple detectors can be combined in a single request.

### Hap (Hate, Abuse, and Profanity)

The `Hap` detector identifies harmful language, hate speech, and profanity in text. You can control its sensitivity using the `threshold` parameter.

```java
// Default configuration
Hap hap = Hap.ofDefaults();

// With custom threshold
Hap hap = Hap.ofThreshold(0.3)
```

### Pii (PII Detection)

The `Pii` detector scans text for sensitive personal information such as phone numbers, email addresses, names, and other identifiers.

```java
// Default configuration
Pii pii = Pii.ofDefaults();
```

### GraniteGuardian

The `GraniteGuardian` detector uses IBM Granite Guardian for content moderation and broader safety analysis.

> **Deprecated**: `GraniteGuardian` is accepted by the context and generated detection endpoints but will be removed in a future release. Prefer `ContextRelevance`, `Faithfulness`, or `AnswerRelevance` for those endpoints.

```java
// Default configuration
GraniteGuardian guardian = GraniteGuardian.ofDefaults();

// With custom threshold
GraniteGuardian guardian = GraniteGuardian.ofThreshold(0.5);
```

### ContextRelevance

The `ContextRelevance` detector evaluates whether the retrieved context documents are relevant to the input query. Used with `detectContext`.

```java
// Default configuration
ContextRelevance cr = ContextRelevance.ofDefaults();

// With threshold and method
ContextRelevance cr = ContextRelevance.builder()
    .threshold(0.5)
    .build();
```

### Faithfulness

The `Faithfulness` detector (also known as groundedness) evaluates whether the generated answer is supported by the context. Used with `detectContext`.

```java
// Default configuration
Faithfulness f = Faithfulness.ofDefaults();

// With threshold and method
Faithfulness f = Faithfulness.builder()
    .threshold(0.5)
    .build();
```

### AnswerRelevance

The `AnswerRelevance` detector evaluates whether the generated text answers the original prompt. Used with `detectGenerated`.

```java
// Default configuration
AnswerRelevance ar = AnswerRelevance.ofDefaults();

// With threshold and method
AnswerRelevance ar = AnswerRelevance.builder()
    .threshold(0.5)
    .build();
```

---

## Examples

### Detecting HAP Content

```java
 var request = DetectionTextRequest.builder()
    .input("I kill you")
    .detectors(Hap.ofThreshold(0.3))
    .build();

DetectionResponse<DetectionTextResponse> response = detectionService.detect(request);
DetectionTextResponse detection = response.detections().get(0);
System.out.println(detection.text());          // → I kill you
System.out.println(detection.detection());     // → has_HAP
System.out.println(detection.score());         // → 0.97
System.out.println(detection.detectionType()); // → hap
```

### Detecting PII

```java
var request = DetectionTextRequest.builder()
    .input("My name is George and my phone number is 1234567890")
    .detectors(Pii.ofDefaults())
    .build();

DetectionResponse<DetectionTextResponse> response = detectionService.detect(request);
DetectionTextResponse detection = response.detections().get(0);
System.out.println(detection.text());          // → 1234567890
System.out.println(detection.detection());     // → PhoneNumber
System.out.println(detection.detectionType()); // → pii
System.out.println(detection.score());         // → ~0.8
```

### Combining Multiple Detectors

You can run multiple detectors in a single request. The response will include all detections from all active detectors.

```java
var request = DetectionTextRequest.builder()
    .input("I kill you with my phone number 1234567890")
    .detectors(Pii.ofDefaults(), Hap.ofThreshold(0.3))
    .build();

DetectionResponse<DetectionTextResponse> response = detectionService.detect(request);

for (DetectionTextResponse detection : response.detections()) {
    System.out.printf("[%s] %s (score=%.2f): \"%s\"%n",
        detection.detectionType(),
        detection.detection(),
        detection.score(),
        detection.text()
    );
}
// → [hap] has_HAP (score=0.94): "I kill you with my phone number 1234567890"
// → [pii] PhoneNumber (score=0.80): "1234567890"
```

### Context Detection

Analyzes input text against one or more context documents. Supports `ContextRelevance` and `Faithfulness` detectors.

```java
var request = DetectionContextRequest.builder()
    .input("What is IBM?")
    .context(List.of("https://en.wikipedia.org/wiki/IBM"))
    .detectors(ContextRelevance.ofDefaults(), Faithfulness.ofThreshold(0.5))
    .build();

DetectionResponse<DetectionContextResponse> response = detectionService.detectContext(request);

for (DetectionContextResponse detection : response.detections()) {
    System.out.printf("[%s] %s (score=%.2f)%n",
        detection.detectionType(),
        detection.detection(),
        detection.score()
    );
    for (DetectionContextResponse.Evidence evidence : detection.evidence()) {
        System.out.printf("  - %s: %s (score=%.2f)%n", evidence.name(), evidence.value(), evidence.score());
    }
}
```

### Generated Text Detection

Analyzes a prompt together with its generated response. Supports the `AnswerRelevance` detector.

```java
var request = DetectionGeneratedRequest.builder()
    .prompt("What is IBM?")
    .generatedText("IBM is a technology and consulting company.")
    .detectors(AnswerRelevance.ofDefaults())
    .build();

DetectionResponse<DetectionGeneratedResponse> response = detectionService.detectGenerated(request);

for (DetectionGeneratedResponse detection : response.detections()) {
    System.out.printf("[%s] %s (score=%.2f)%n",
        detection.detectionType(),
        detection.detection(),
        detection.score()
    );
}
```

---

## DetectionTextRequest

The `DetectionTextRequest` encapsulates the input text and the list of detectors to apply.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `input` | String | Yes | The text to analyze |
| `detectors` | List\<BaseDetector\> | Yes | One or more detectors to run |
| `projectId` | String | No | Override the default Project ID |
| `spaceId` | String | No | Override the default Space ID |
| `transactionId` | String | No | Request tracking ID |
| `serviceInstanceId` | String | No | Overrides the service-level default. |

---

## DetectionTextResponse

Each item in the `detections()` list represents a single detection match and exposes the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `text()` | String | The portion of input text that was flagged |
| `start()` | int | Start character offset in the input string |
| `end()` | int | End character offset in the input string |
| `detection()` | String | The specific detection label (e.g. `has_HAP`, `PhoneNumber`) |
| `detectionType()` | String | The detector category (e.g. `hap`, `pii`) |
| `score()` | double | Confidence score for the detection |

---

## DetectionContextRequest

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `input` | String | Yes | The text to analyze |
| `context` | List\<String\> | Yes | Context document URLs (1-100 items) |
| `detectors` | List\<BaseDetector\> | Yes | One or more detectors to run (`ContextRelevance`, `Faithfulness`) |
| `projectId` | String | No | Override the default Project ID |
| `spaceId` | String | No | Override the default Space ID |
| `transactionId` | String | No | Request tracking ID |
| `serviceInstanceId` | String | No | Overrides the service-level default. |

---

## DetectionContextResponse

Each item in the `detections()` list exposes:

| Field | Type | Description |
|-------|------|-------------|
| `detectorId()` | String | The id of the underlying model that produced the result (e.g. `granite_guardian_3_2_5b`, `context_relevance_125m_v1_en`) |
| `detectionType()` | String | The detector category (e.g. `context_relevance`, `faithfulness`) |
| `detection()` | String | The detection label |
| `score()` | double | Confidence score (0-1) |
| `evidence()` | List\<Evidence\> | Supporting evidence items |

Each `Evidence` item exposes:

| Field | Type | Description |
|-------|------|-------------|
| `name()` | String | The name of the evidence entry |
| `value()` | String | The value of the evidence entry |
| `score()` | double | Confidence score for this evidence |
| `evidence()` | List\<Evidence\> | Nested child evidence items (null for leaf nodes) |

---

## DetectionGeneratedRequest

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `prompt` | String | Yes | The original prompt text |
| `generatedText` | String | Yes | The generated text to evaluate |
| `detectors` | List\<BaseDetector\> | Yes | One or more detectors to run (`AnswerRelevance`) |
| `projectId` | String | No | Override the default Project ID |
| `spaceId` | String | No | Override the default Space ID |
| `transactionId` | String | No | Request tracking ID |
| `serviceInstanceId` | String | No | Overrides the service-level default. |

---

## DetectionGeneratedResponse

Each item in the `detections()` list exposes:

| Field | Type | Description |
|-------|------|-------------|
| `detectorId()` | String | The id of the underlying model that produced the result (e.g. `granite_guardian_3_2_5b`, `answer_relevance_125m_v1_en`) |
| `detectionType()` | String | The detector category (e.g. `answer_relevance`) |
| `detection()` | String | The detection label |
| `score()` | double | Confidence score (0-1) |

---

## Finding the governance instance ID

The `governanceInstanceId` is the GUID of your [watsonx.governance](https://cloud.ibm.com/catalog/services/watsonxgovernance) service instance on IBM Cloud. The GUID is required to use the `ContextRelevance`, `Faithfulness`, and `AnswerRelevance` detectors.

**From the IBM Cloud console:**

1. Open the [IBM Cloud resource list](https://cloud.ibm.com/resources).
2. Under **AI / Machine Learning**, click on your watsonx.governance instance.
3. In the details panel, copy the **GUID** value

**From the IBM Cloud CLI:**

```bash
ibmcloud resource service-instance <your-instance-name>
```

> For full details on provisioning watsonx.governance, see the [IBM documentation](https://www.ibm.com/docs/en/watsonx/saas?topic=cloud-setting-up-watsonxgovernance).

---

## Related Resources

- [Detection API Reference - Content](https://cloud.ibm.com/apidocs/watsonx-ai#text-detection-content)
- [Detection API Reference - Context](https://cloud.ibm.com/apidocs/watsonx-ai#text-detection-context)
- [Detection API Reference - Generated](https://cloud.ibm.com/apidocs/watsonx-ai#text-detection-generated)
- [Setting up watsonx.governance on IBM Cloud](https://www.ibm.com/docs/en/watsonx/saas?topic=cloud-setting-up-watsonxgovernance)
- [Sample](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/detection)
