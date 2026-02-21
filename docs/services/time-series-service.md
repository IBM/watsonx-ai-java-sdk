---
layout: default
title: Time Series Service
parent: Services
nav_order: 9
permalink: /services/time-series-service/
---

# Time Series Service

The `TimeSeriesService` provides functionality to generate time series forecasts using **IBM watsonx.ai foundation models** (**Granite TTM family**). It accepts historical timestamped data and predicts future values, supporting both single-target and multi-variate scenarios.

## Quick Start

```java
TimeSeriesService service = TimeSeriesService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl(CloudRegion.DALLAS)
    .modelId("ibm/granite-ttm-512-96-r2")
    .build();

InputSchema schema = InputSchema.builder()
    .timestampColumn("date")
    .addIdColumn("ID1")
    .build();

ForecastData data = ForecastData.create()
    .add("date", "2024-01-01T00:00:00")
    .add("date", "2024-01-02T00:00:00")
    .add("date", "2024-01-03T00:00:00")
    ...
    .add("ID1", "series-A", 512)
    .addAll("sales", 120.5, 135.0, 128.3, ...);

var request =  TimeSeriesRequest.builder()
    .inputSchema(schema)
    .data(data)
    .build();

ForecastResponse response = service.forecast(request);
System.out.println("Forecasted points: " + response.outputDataPoints());
```

> **Note:** To see the list of available models, refer to [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx#ibm-provided).

---

## Overview

The `TimeSeriesService` enables you to:

- Forecast future values from historical time series data using **IBM Granite TTM models**.
- Handle single-target and multi-variate time series with multiple ID columns.
- Control prediction horizon via `predictionLength`.
- Override the model ID per-request via `TimeSeriesParameters`.

---

## Service Configuration

### Basic Setup

```java
TimeSeriesService service = TimeSeriesService.builder()
    .apiKey(WATSONX_API_KEY)
    .projectId(WATSONX_PROJECT_ID)
    .baseUrl("https://us-south.ml.cloud.ibm.com") // or use CloudRegion
    .modelId("ibm/granite-ttm-512-96-r2")
    .build();
```

### Builder Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `apiKey` | String | Conditional | API key for IBM Cloud authentication |
| `authenticator` | Authenticator | Conditional | Custom authentication (alternative to `apiKey`) |
| `projectId` | String | Conditional | Project ID where the forecast will run |
| `spaceId` | String | Conditional | Space ID (alternative to `projectId`) |
| `modelId` | String | Yes | Foundation model ID to use for forecasting |
| `baseUrl` | String/CloudRegion | Yes | watsonx.ai service base URL |
| `timeout` | Duration | No | Request timeout (default: 60 seconds) |
| `logRequests` | Boolean | No | Enable request logging (default: false) |
| `logResponses` | Boolean | No | Enable response logging (default: false) |
| `httpClient` | HttpClient | No | Custom HTTP client |
| `verifySsl` | Boolean | No | SSL certificate verification (default: true) |
| `version` | String | No | API version override |

> Either `apiKey` or `authenticator` must be provided. Either `projectId` or `spaceId` must be specified.

---

## Examples

### Basic Forecast

The simplest approach — provide schema, data, and get predictions back synchronously:

```java
InputSchema schema = InputSchema.builder()
    .timestampColumn("date")
    .addTargetColumn("sales")
    .build();

ForecastData data = ForecastData.create()
    .addAll("date", "2024-01-01T00:00:00", "2024-01-02T00:00:00", "2024-01-03T00:00:00",
        "2024-01-04T00:00:00", "2024-01-05T00:00:00", ...)
    .addAll("sales", 120.5, 135.0, 128.3, 142.7, 138.1, ...);

ForecastResponse response = service.forecast(schema, data);
```

### Setting Prediction Length

Control how many future time steps to forecast with `predictionLength`:

```java
var parameters = TimeSeriesParameters.builder()
    .predictionLength(24)   // predict next 24 time steps
    .build();

ForecastResponse response = service.forecast(schema, data, parameters);
```

### Multi-Variate Forecast with Multiple ID Columns

Use `idColumns` to segment multiple concurrent time series in the same dataset, and `targetColumns` to specify which columns to forecast:

```java
InputSchema schema = InputSchema.builder()
    .timestampColumn("date")
    .idColumns("region", "product_id")      // compound key per time series
    .targetColumns("units_sold", "revenue") // both columns will be forecasted
    .freq("D")                              // daily frequency
    .build();

ForecastData data = ForecastData.create()
    .addAll("date", "2024-01-01", "2024-01-02", "2024-01-03", "2024-01-01", "2024-01-02", "2024-01-03", ...)
    .addAll("region",     "north", "north", "north", "south", "south", "south", ...)
    .addAll("product_id", "P001",  "P001",  "P001",  "P001",  "P001",  "P001", ...)
    .addAll("units_sold", 100, 120, 110, 80, 95, 88, ...)
    .addAll("revenue",    500.0, 600.0, 550.0, 400.0, 475.0, 440.0, ...);

var parameters = TimeSeriesParameters.builder()
    .predictionLength(7)
    .build();

var request =  TimeSeriesRequest.builder()
    .inputSchema(schema)
    .data(data)
    .parameters(parameters)
    .build();

ForecastResponse response = service.forecast(request);
```

---

## Building Input Data

### ForecastData

`ForecastData` is a columnar data structure where each key is a column name and maps to a list of values. All columns must have the **same number of rows**.

| Method | Description |
|--------|-------------|
| `ForecastData.create()` | Creates a new empty instance |
| `ForecastData.from(map)` | Wraps an existing `Map<String, List<Object>>` |
| `.add(key, value)` | Appends a single value to a column |
| `.add(key, value, times)` | Appends the same value `n` times (useful for repeated ID values) |
| `.addAll(key, values...)` | Appends multiple values to a column |
| `.addAll(key, collection)` | Appends a `Collection` of values to a column |
| `.get(key)` | Returns the list of values for a column |
| `.containsKey(key)` | Checks if a column exists |
| `.asMap()` | Returns the underlying `Map<String, List<Object>>` |

### InputSchema

`InputSchema` describes the structure of the data columns.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `timestampColumn` | String | Yes | Name of the column containing timestamps. ISO 8601 format recommended (e.g., `2024-10-18T01:09:21.454746+00:00`) |
| `idColumns` | String... | No | Columns that form a compound key identifying each unique time series (max 10, each 0–100 chars) |
| `targetColumns` | String... | No | Columns containing the values to forecast (max 500, each 0–100 chars). If not set, all non-timestamp, non-ID columns are used |
| `freq` | String | No | Frequency string (e.g., `"D"`, `"h"`, `"min"`, `"W"`, `"M"`). Inferred from data if not provided |

## Forecast Parameters

The `TimeSeriesParameters` class controls the forecast behavior per request.

### Builder Reference

| Parameter | Type | Description |
|-----------|------|-------------|
| `predictionLength` | Integer | Number of future time steps to predict (≥1, max determined by model context). If not set, the model default is used and `toParameters()` returns `null` (no `parameters` block is sent) |
| `futureData` | ForecastData | Exogenous features known in advance for the forecast horizon (e.g., holidays, scheduled events). **Only supported when using `DeploymentService`** |
| `modelId` | String | Override the service-level model ID for this request |
| `projectId` | String | Override the default project ID |
| `spaceId` | String | Override the default space ID |
| `transactionId` | String | Request tracking ID for tracing |

---

## ForecastResponse

| Field | Type | Description |
|-------|------|-------------|
| `modelId()` | String | Identifier of the model used for the forecast |
| `modelVersion()` | String | Version of the model |
| `createdAt()` | String | ISO 8601 timestamp when the response was created |
| `results()` | List\<Map\<String, Object\>\> | List of prediction result maps, one per time series segment (see below) |
| `inputDataPoints()` | int | Total number of input data points (rows × input columns) |
| `outputDataPoints()` | int | Total number of forecasted data points |

### Reading Results

Each map in `results()` contains one entry per column — the timestamp column, ID columns, and forecasted target columns — all as lists of values aligned by index:

```java
ForecastResponse response = service.forecast(schema, data, params);

for (Map<String, Object> segment : response.results()) {
    List<String> dates   = (List<String>) segment.get("date");
    List<String> ids     = (List<String>) segment.get("ID1");
    List<Double> targets = (List<Double>) segment.get("TARGET1");

    for (int i = 0; i < dates.size(); i++) {
        System.out.printf("%s  [%s]  %.4f%n", dates.get(i), ids.get(i), targets.get(i));
    }
}
// Example output:
// 2024-01-06T00:00:00  [D1]  1.8600
// 2024-01-07T00:00:00  [D1]  3.2400
// 2024-01-08T00:00:00  [D1]  6.7800
```

---

## Related Resources

- [Time Series Forecast API Reference](https://cloud.ibm.com/apidocs/watsonx-ai#time-series-forecast)
- [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx#ibm-provided)
- [Sample Code](https://github.com/IBM/watsonx-ai-java-sdk/tree/main/samples/time-series)
