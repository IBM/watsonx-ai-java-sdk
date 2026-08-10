---
id: time-series-service
title: Time Series Service
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

var request = TimeSeriesRequest.builder()
    .inputSchema(schema)
    .data(data)
    .build();

ForecastResponse response = service.forecast(request);
System.out.println("Forecasted points: " + response.outputDataPoints());
```

> **Note:** To see the list of available models, refer to [Supported Foundation Models](https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-models.html?context=wx#ibm-provided). To list only time series models programmatically, use the [Foundation Model Service](/services/foundation-model-service) and filter by `function("function_time_series_forecast")`.

---

## Overview

The `TimeSeriesService` enables you to:

- Forecast future values from historical time series data using **IBM Granite TTM models**.
- Handle single-target and multi-variate time series with multiple ID columns.
- Control prediction horizon via `predictionLength`.
- Override the model ID per-request via `TimeSeriesParameters`.

### Supported Models

The Granite time series models (also known as **Tiny Time Mixers, TTM**) are compact pretrained models from IBM Research for multivariate time series forecasting. Your administrator must install at least one of the following models before use:

| Model ID | Minimum data points per channel | Notes |
|----------|---------------------------------|-------|
| `ibm/granite-ttm-512-96-r2` | 512 | Smallest model, suitable when less historical data is available |
| `ibm/granite-ttm-1024-96-r2` | 1,024 | Balanced model for medium history lengths |
| `ibm/granite-ttm-1536-96-r2` | 1,536 | Best results when the most historical data is available |

All three models output **96 data points per channel** by default and work best with data sampled at **minute or hour intervals**. If you provide more data points than the model requires, the model uses the most recent points up to its limit and ignores the rest. For best results, use the model that accepts the most data points based on the history available to you.

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

The simplest approach - provide schema, data, and get predictions back synchronously:

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

### Multi-variate Forecast with ID Columns

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

var request = TimeSeriesRequest.builder()
    .inputSchema(schema)
    .data(data)
    .parameters(parameters)
    .build();

ForecastResponse response = service.forecast(request);
```

---

## Data Requirements

Before submitting a forecast request, ensure your data meets the following requirements:

- **Numerical values only** - recorded observations must be numerical (for example temperatures, stock prices, or sensor readings). Non-numerical values are not supported.
- **Minimum data points** - each time series must include at least as many data points as the chosen model requires per channel (512, 1,024, or 1,536 rows depending on the model). If fewer points are provided, the request will fail.
- **No missing values** - all arrays for the timestamp column, ID columns, and target columns must have the same length. You cannot skip a data point or use `null` as a placeholder.
- **Uniform sampling frequency** - data must be collected at a consistent interval (for example every 1 minute, 1 hour, or 1 day). Non-uniform timestamps do not cause an error, but may degrade forecast quality.
- **ISO 8601 timestamps recommended** - use `2024-11-12T15:06:35` or `2024-11-12T15:06:35+0000` format to avoid date-convention ambiguity and apparent duplicates due to time-zone differences.

### Frequency String Reference

The `freq` field on `InputSchema` accepts [pandas Period aliases](https://pandas.pydata.org/docs/user_guide/timeseries.html#period-aliases). Common values:

| Value | Meaning |
|-------|---------|
| `min` | Minute |
| `h` | Hour |
| `D` | Calendar day |
| `W` | Week |
| `M` | Month end |
| `Q` | Quarter end |
| `Y` | Year end |

If `freq` is not specified, the service attempts to infer it from the timestamp data. The generated forecast data is formatted using the same frequency you specify.

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
| `futureData` | ForecastData | Exogenous features known in advance for the forecast horizon (e.g., holidays, scheduled events). **Only supported when using [`DeploymentService`](/services/deployment-service)** |
| `modelId` | String | Override the service-level model ID for this request |
| `projectId` | String | Override the default Project ID |
| `spaceId` | String | Override the default Space ID |
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

Each map in `results()` contains one entry per column (the timestamp column, ID columns, and forecasted target columns), all as lists of values aligned by index:

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
