# Watsonx Time Series Forecast Example

This is a simple example that demonstrates how to use IBM watsonx.ai to generate time series forecasts using the `ibm/granite-ttm-512-96-r2` model.

The example focuses on forecasting hourly energy consumption (in kWh) for a single building (e.g., "D1") using historical time series data.

## Use Case

Forecasting short-term energy consumption for a building is useful for optimizing resource usage, anticipating peak demand, and reducing operational costs.This sample simulates a typical industrial or commercial energy monitoring scenario where time-based predictions are needed for planning and automation.

Forecast schema:
```json
{
    "timestamp_column": "date",
    "id_columns": ["ID1"],
    "target_columns": ["TARGET1"]
}
```

Forecast data:

```json
{
    "date": [
        "2023-10-01T00:00:00",
        "2023-10-01T01:00:00",
        "2023-10-01T02:00:00",
        "2023-10-01T03:00:00",
        "2023-10-01T04:00:00",
        "2023-10-01T05:00:00",
        ...
    ],
    "ID1": [
        "D1",
        "D1",
        "D1",
        "D1",
        "D1",
        "D1",
        ...
    ],
    "TARGET1": [
        2.3,
        2.8,
        2.5,
        2.7,
        3.0,
        3.2,
        ...
    ]
}
```

## Prerequisites

Before running the application, set the following environment variables or create a new `.env` file in the project's root folder:

- `WATSONX_API_KEY` – Your watsonx.ai API key
- `WATSONX_URL` – The base URL for the watsonx.ai service
- `WATSONX_PROJECT_ID` – Your watsonx.ai project id

Example (Linux/macOS):
```bash
export WATSONX_API_KEY=api-key
export WATSONX_URL=https://watsonx-url
export WATSONX_PROJECT_ID=project-id
```

Example (Windows CMD):
```cmd
set WATSONX_API_KEY=api-key
set WATSONX_URL=https://watsonx-url
set WATSONX_PROJECT_ID=project-id
```
## How to Run
Use Maven to run the application. 
```bash
mvn package exec:java 
```