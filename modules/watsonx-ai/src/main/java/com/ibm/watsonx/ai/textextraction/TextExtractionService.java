/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import static com.ibm.watsonx.ai.core.Json.fromJson;
import static com.ibm.watsonx.ai.core.Json.toJson;
import static com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type.PAGE_IMAGES;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.Parameters;

/**
 * Service client for extracting text from high-value business documents. *
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextExtractionService textExtractionService = TextExtractionService.builder()
 *   .url("https://...") // or use CloudRegion
 *   .authenticationProvider(authProvider)
 *   .projectId("my-project-id")
 *   .documentReference("3b33d2da-fb14-4776-ac57-294b1b11d7aa", "my-bucket")
 *   .resultReference("3b33d2da-fb14-4776-ac57-294b1b11d7aa", "my-bucket")
 *   .build();
 *
 * TextExtractionResponse response = textExtractionService.startExtraction("myfile.pdf")
 * }</pre>
 */
public class TextExtractionService extends WatsonxService {

  private static final Logger logger = LoggerFactory.getLogger(TextExtractionService.class);
  private final CosReference documentReference;
  private final CosReference resultReference;

  public TextExtractionService(Builder builder) {
    super(builder);
    this.documentReference = requireNonNull(builder.documentReference, "documentReference value cannot be null");
    this.resultReference = requireNonNull(builder.resultReference, "resultReference value cannot be null");
  }

  /**
   * Starts a new text extraction request for the specified document.
   * <p>
   * This operation initiates the extraction of text and metadata for a document located at the specified path.
   * <p>
   * Refer to the
   * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html?context=wx&audience=wdp">official
   * documentation</a> for more details on the supported formats, features, and limitations of the text extraction service.
   *
   * @param path The location of the document to be processed.
   * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
   *
   */
  public TextExtractionResponse startExtraction(String path) {
    return startExtraction(path, null);
  }

  /**
   * Starts a new text extraction request for the specified document.
   * <p>
   * This operation initiates the extraction of text and metadata for a document located at the specified path. The behavior and output of the
   * extraction can be customized using the provided {@link TextExtractionParameters}.
   * <p>
   * Refer to the
   * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html?context=wx&audience=wdp">official
   * documentation</a> for more details on the supported formats, features, and limitations of the text extraction service.
   *
   * @param path The location of the document to be processed.
   * @param parameters Configuration parameters for the text extraction request.
   * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
   *
   */
  public TextExtractionResponse startExtraction(String path, TextExtractionParameters parameters) {
    requireNonNull(path);

    String outputFileName = null;
    String projectId = this.projectId;
    String spaceId = this.spaceId;
    List<String> requestedOutputs = List.of("plain_text");
    CosReference documentReference = this.documentReference;
    CosReference resultReference = this.resultReference;
    Parameters params = null;
    Map<String, Object> custom = null;

    if (nonNull(parameters)) {
      outputFileName = parameters.getOutputFileName();
      projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
      spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
      requestedOutputs = requireNonNullElse(parameters.getRequestedOutputs(), requestedOutputs);
      documentReference = requireNonNullElse(parameters.getDocumentReference(), this.documentReference);
      resultReference = requireNonNullElse(parameters.getResultReference(), this.resultReference);
      params = parameters.toParameters();
      custom = parameters.getCustom();
    }

    if (isNull(outputFileName)) {

      var isMultiOutput =
        requestedOutputs.size() > 1 || requestedOutputs.get(0).equals(PAGE_IMAGES.value()) ? true : false;

      if (isMultiOutput) {

        outputFileName = "/";

      } else {

        var type = Type.fromValue(requestedOutputs.get(0));
        outputFileName = FileUtils.addExtension(path, type);
      }
    }

    var request = new TextExtractionRequest(
      projectId,
      spaceId,
      documentReference.toDataReference(path),
      resultReference.toDataReference(outputFileName),
      params,
      custom
    );

    var httpRequest =
      HttpRequest.newBuilder(URI.create(url.toString() + "%s/extractions?version=%s".formatted(ML_API_PATH, version)))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(BodyPublishers.ofString(toJson(request)))
        .build();

    try {

      var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
      return fromJson(httpReponse.body(), TextExtractionResponse.class);

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Retrieves the results of a text extraction request by its unique identifier.
   * <p>
   * This operation fetches the details and results of a previously submitted text extraction request. Note that the retention period for extraction
   * results is 2 days. If the request is older than 2 days, the results will no longer be available and this method will return {@code false}.
   *
   * @param id The unique identifier of the text extraction request.
   * @return A {@link TextExtractionResponse} containing the results of the request.
   */
  public TextExtractionResponse fetchExtractionRequest(String id) {
    return fetchExtractionRequest(id, TextExtractionFetchParameters.builder().build());
  }

  /**
   * Retrieves the results of a text extraction request by its unique identifier.
   * <p>
   * This operation fetches the details and results of a previously submitted text extraction request. Note that the retention period for extraction
   * results is 2 days. If the request is older than 2 days, the results will no longer be available and this method will return {@code false}.
   *
   * @param id The unique identifier of the text extraction request.
   * @param parameters Parameters to specify the project or space context in which the request was made.
   * @return A {@link TextExtractionResponse} containing the results of the request.
   */
  public TextExtractionResponse fetchExtractionRequest(String id, TextExtractionFetchParameters parameters) {
    requireNonNull(id, "The id can not be null");

    var projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
    var spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
    var queryParameters = getQueryParameters(projectId, spaceId);
    var uri = URI.create(url.toString() + "%s/extractions/%s?%s".formatted(ML_API_PATH, id, queryParameters));

    var httpRequest = HttpRequest.newBuilder(uri)
      .header("Accept", "application/json")
      .GET()
      .build();

    try {

      var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
      return fromJson(httpReponse.body(), TextExtractionResponse.class);

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Deletes a text extraction request.
   *
   * @param id The unique identifier of the text extraction request to delete.
   * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
   */
  public boolean delete(String id) {
    return delete(id, TextExtractionDeleteParameters.builder().build());
  }

  /**
   * Deletes a text extraction request.
   * <p>
   * This operation cancels the specified text extraction request. If the {@code hardDelete} parameter is set to {@code true}, it will also delete the
   * associated job metadata.
   *
   * @param id The unique identifier of the text extraction request to delete.
   * @param parameters Parameters specifying the space or project context, and whether to perform a hard delete.
   * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
   */
  public boolean delete(String id, TextExtractionDeleteParameters parameters) {
    requireNonNull(id, "The id can not be null");

    var projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
    var spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;

    var queryParameters = parameters.getHardDelete()
      .map(nullable -> getQueryParameters(projectId, spaceId).concat("&hard_delete=true"))
      .orElse(getQueryParameters(projectId, spaceId));

    var uri = URI.create(url.toString() + "%s/extractions/%s?%s".formatted(ML_API_PATH, id, queryParameters));
    var httpRequest = HttpRequest.newBuilder(uri).DELETE().build();

    try {

      syncHttpClient.send(httpRequest, BodyHandlers.ofString());
      return true;

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    } catch (WatsonxException e) {
      if (e.statusCode() == 404)
        return false;
      throw e;
    }
  }

  /**
   * Generates query parameters for API requests based on provided project_id or space_id.
   */
  private String getQueryParameters(String projectId, String spaceId) {
    if (nonNull(projectId))
      return "version=%s&project_id=%s".formatted(version, URLEncoder.encode(projectId, Charset.defaultCharset()));
    else
      return "version=%s&space_id=%s".formatted(version, URLEncoder.encode(spaceId, Charset.defaultCharset()));
  }

  /*
  private TextExtractionResponse startExtraction(String absolutePath, TextExtractionParameters parameters, boolean waitUntilJobIsDone)
      throws TextExtractionException {
      requireNonNull(absolutePath);
      requireNonNull(parameters);

      var removeOutputFile = parameters.isRemoveOutputFile();
      var removeUploadedFile = parameters.isRemoveUploadedFile();
      var outputFileName = parameters.getOutputFileName();

      if (!waitUntilJobIsDone && (removeOutputFile || removeUploadedFile))
          throw new IllegalArgumentException(
              "The asynchronous version of startExtraction doesn't allow the use of the \"removeOutputFile\" and \"removeUploadedFile\" parameters");

      if (isNull(outputFileName) || outputFileName.isBlank()) {

          var isMultiOutput =
              requestedOutputs.size() > 1
                  || (requestedOutputs.size() == 1 && requestedOutputs.get(0).equals(PAGE_IMAGES))
                      ? true
                      : false;

          var isSingleOutput = requestedOutputs.size() == 1 ? true : false;

          if (isMultiOutput) {

              outputFileName = "/";

          } else if (isSingleOutput) {

              var type = Type.fromValue(requestedOutputs.get(0));
              outputFileName = FileUtils.addExtension(outputFileName, type);
          }
      }

      var request = new TextExtractionRequest(
          projectId,
          spaceId,
          requireNonNullElse(parameters.getDocumentReference(), documentReference).toDataReference(outputFileName),
          requireNonNullElse(parameters.getResultReference(), resultReference).toDataReference(outputFileName),
          parameters.toParameters(),
          null // TODO: PASS THE CUSTOM PARAMETERS.
      );


      TextExtractionResponse response = retryOn(new Callable<TextExtractionResponse>() {
          @Override
          public TextExtractionResponse call() throws Exception {
              return watsonxClient.startTextExtractionJob(request, version);
          }
      });

      if (!waitUntilJobIsDone)
          return response;

      Status status;
      long sleepTime = 100;
      LocalTime endTime = LocalTime.now().plus(parameters.timeout);

      do {

          if (LocalTime.now().isAfter(endTime))
              throw new TextExtractionException("timeout",
                  "Execution to extract %s file took longer than the timeout set by %s milliseconds"
                      .formatted(absolutePath, parameters.timeout.toMillis()));

          try {

              Thread.sleep(sleepTime);
              sleepTime *= 2;
              sleepTime = Math.min(sleepTime, 3000);

          } catch (Exception e) {
              throw new TextExtractionException("interrupted", e.getMessage());
          }

          var processId = response.metadata().id();
          response = retryOn(new Callable<TextExtractionResponse>() {
              @Override
              public TextExtractionResponse call() throws Exception {
                  return watsonxClient.getTextExtractionDetails(processId, spaceId, projectId, version);
              }
          });

          status = response.entity().results().status();

      } while (status != Status.FAILED && status != Status.COMPLETED);

      return response;
  }*/

  /**
   * Returns a new {@link Builder} instance.
   * <p>
   * <b>Example usage:</b>
   *
   * <pre>{@code
   * TextExtractionService textExtractionService = TextExtractionService.builder()
   *   .url("https://...") // or use CloudRegion
   *   .authenticationProvider(authProvider)
   *   .projectId("my-project-id")
   *   .documentReference("3b33d2da-fb14-4776-ac57-294b1b11d7aa", "my-bucket")
   *   .resultReference("3b33d2da-fb14-4776-ac57-294b1b11d7aa", "my-bucket")
   *   .build();
   *
   * TextExtractionResponse response = textExtractionService.startExtraction("myfile.pdf")
   * }</pre>
   *
   * @return {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for constructing {@link TextExtractionService} instances with configurable parameters.
   */
  public static class Builder extends WatsonxService.Builder<Builder> {
    private CosReference documentReference;
    private CosReference resultReference;

    /**
     * Specifies the Cloud Object Storage connection and bucket where the input files are stored.
     *
     * @param connectionId The id of the COS connection asset.
     * @param bucket The name of the bucket containing the input documents.
     */
    public Builder documentReference(String connectionId, String bucket) {
      this.documentReference = new CosReference(connectionId, bucket);
      return this;
    }

    /**
     * Specifies the Cloud Object Storage connection and bucket where the extracted results should be stored.
     *
     * @param connectionId The id of the COS connection asset.
     * @param bucket The name of the bucket where results will be written.
     */
    public Builder resultReference(String connectionId, String bucket) {
      this.resultReference = new CosReference(connectionId, bucket);
      return this;
    }

    /**
     * Builds a {@link TextExtractionService} instance using the configured parameters.
     *
     * @return a new instance of {@link TextExtractionService}
     */
    public TextExtractionService build() {
      return new TextExtractionService(this);
    }
  }
}
