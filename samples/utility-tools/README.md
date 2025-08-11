# Watsonx Utility Tools Sample

This sample demonstrates how to use IBM watsonx.ai Utility Agent Tools through the Java SDK. It shows how to invoke built-in tools like `GoogleSearchTool`, `WebCrawlerTool`, `WikipediaTool` and `WeatherTool`.

## Prerequisites

Before running the application, set the following environment variables or create a `.env` file in the project root:

- `WATSONX_API_KEY` – Your watsonx.ai API key
- `TAVILY_SEARCH_API_KEY` - Tavily API Key (If not set, the TavilySearchTool will not be used)
- `WATSONX_URL` – The base URL for the watsonx.ai service

> NOTE: This is a partial list of URLs:
> - **Dallas**: `https://api.dataplatform.cloud.ibm.com/wx`
> - **Frankfurt**: `https://api.eu-de.dataplatform.cloud.ibm.com/wx`
> - **London**: `https://api.eu-gb.dataplatform.cloud.ibm.com/wx`
> - **Tokyo**: `https://api.jp-tok.dataplatform.cloud.ibm.com/wx`
> - **Sydney**: `https://api.au-syd.dai.cloud.ibm.com/wx`
> - **Toronto**: `https://api.ca-tor.dai.cloud.ibm.com/wx`
> - **Mumbai**: `https://api.ap-south-1.aws.data.ibm.com/wx`
>
> See the [documentation](https://cloud.ibm.com/apidocs/watsonx-ai#endpoint-url) for the full list. 


Example (Linux/macOS):

```bash
export WATSONX_API_KEY=your-api-key
export WATSONX_URL=https://your-watsonx-url
```

Example (Windows CMD):

```cmd
set WATSONX_API_KEY=your-api-key
set WATSONX_URL=https://your-watsonx-url
```

## How to Run

Use Maven to run the sample:

```bash
mvn package exec:java
```