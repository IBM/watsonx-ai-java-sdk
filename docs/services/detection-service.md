---
layout: default
title: Detection Service
parent: Services
nav_order: 4
permalink: /services/detection-service/
---


# Detection Service

The `DetectionService` provides functionality to analyze text content using **IBM watsonx.ai detection APIs**. It supports identification of harmful content, personally identifiable information (PII), and other types of sensitive or unsafe text through configurable detectors.

## Quick Start

```java
DetectionService detectionService = DetectionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
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

---

## Service Configuration

### Basic Setup

To start using the Detection Service, create a `DetectionService` instance with the minimum required configuration.

```java
DetectionService detectionService = DetectionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com")
    .build();
```

### Using CloudRegion

Instead of manually specifying the `baseUrl`, you can use the `CloudRegion` to automatically configure the correct endpoint.

```java
DetectionService detectionService = DetectionService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
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

### Pii (Personally Identifiable Information)

The `Pii` detector scans text for sensitive personal information such as phone numbers, email addresses, names, and other identifiers.

```java
// Default configuration
Pii pii = Pii.ofDefaults();
```

### GraniteGuardian

The `GraniteGuardian` detector uses IBM Granite Guardian for content moderation and broader safety analysis.

```java
// Default configuration
GraniteGuardian guardian = GraniteGuardian.ofDefaults();

// With custom threshold
GraniteGuardian guardian = GraniteGuardian.ofThreshold(0.5);
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

---

## DetectionTextRequest

The `DetectionTextRequest` encapsulates the input text and the list of detectors to apply.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `input` | String | Yes | The text to analyze |
| `detectors` | List\<BaseDetector\> | Yes | One or more detectors to run |
| `projectId` | String | No | Override the default project ID |
| `spaceId` | String | No | Override the default space ID |
| `transactionId` | String | No | Request tracking ID |

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

## Related Resources

- [Detection API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#text-detection-content)
- [Sample](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/detection)
