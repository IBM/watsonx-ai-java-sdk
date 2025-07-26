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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.exeception.WatsonxException;
import com.ibm.watsonx.ai.foundationmodel.FoundationModelService;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.CosUrl;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type;
import com.ibm.watsonx.ai.textextraction.TextExtractionRequest.Parameters;
import com.ibm.watsonx.ai.textextraction.TextExtractionResponse.Error;
import com.ibm.watsonx.ai.textextraction.TextExtractionResponse.Status;

/**
 * Service class to interact with IBM watsonx.ai Text Extraction APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextExtractionService textExtractionService = TextExtractionService.builder()
 *   .url("https://...") // or use CloudRegion
 *   .cosUrl("https://...") // or use CosUrl
 *   .authenticationProvider(authProvider)
 *   .projectId("my-project-id")
 *   .documentReference("<connection_id>", "<bucket-name")
 *   .resultReference("<connection_id>", "<bucket-name")
 *   .build();
 *
 * TextExtractionResponse response = textExtractionService.startExtraction("myfile.pdf")
 * }</pre>
 *
 * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-extraction" target="_blank"> official documentation</a>.
 *
 * @see AuthenticationProvider
 */
public final class TextExtractionService extends WatsonxService {

    private static final Logger logger = LoggerFactory.getLogger(TextExtractionService.class);
    private final String cosUrl;
    private final CosReference documentReference;
    private final CosReference resultReference;

    protected TextExtractionService(Builder builder) {
        super(builder);
        var tmpUrl = requireNonNull(builder.cosUrl, "cosUrl value cannot be null");
        cosUrl = tmpUrl.endsWith("/") ? tmpUrl.substring(0, tmpUrl.length() - 1) : tmpUrl;
        documentReference = requireNonNull(builder.documentReference, "documentReference value cannot be null");
        resultReference = requireNonNull(builder.resultReference, "resultReference value cannot be null");
    }

    /**
     * Starts the text extraction process for a document. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename
     * but using the {@code .md} extension. To customize the output behavior, use the method with the {@link TextExtractionParameters} class.
     *
     * <pre>
     * {@code
     * String startExtraction(String absolutePath, TextExtractionParameters parameters)
     * }
     * </pre>
     *
     * Refer to the
     * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html?context=wx&audience=wdp">official
     * documentation</a> for more details on the supported formats, features, and limitations of the text extraction service.
     * <p>
     * <b>Note:</b> This method does not return the extracted value, use {@code extractAndFetch} to extract the text immediately.
     *
     * @param absolutePath The location of the document to be processed.
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     */
    public TextExtractionResponse startExtraction(String absolutePath) throws TextExtractionException {
        return startExtraction(absolutePath, null);
    }

    /**
     * Starts the text extraction process for a document. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename
     * but using the {@code .md} extension. Output behavior can be customized using the {@link TextExtractionParameters} parameter. Refer to the
     * <a href="https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction.html?context=wx&audience=wdp">official
     * documentation</a> for more details on the supported formats, features, and limitations of the text extraction service.
     * <p>
     * <b>Note:</b> This method does not return the extracted value, use {@code extractAndFetch} to extract the text immediately.
     *
     * @param absolutePath The location of the document to be processed.
     * @param parameters The configuration parameters for text extraction.
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     */
    public TextExtractionResponse startExtraction(String absolutePath, TextExtractionParameters parameters) throws TextExtractionException {
        return startExtraction(absolutePath, parameters, false);
    }

    /**
     * Uploads a local file and starts the text extraction process. The extracted text is saved as a new <b>Markdown</b> file, preserving the original
     * filename but using the {@code .md} extension by default. To customize the output behavior you can use the {@link TextExtractionParameters}
     * class.
     *
     * <pre>
     * {@code
     * String uploadAndStartExtraction(File file, TextExtractionParameters parameters);
     * }
     * </pre>
     *
     * <b>Note:</b> This method does not return the extracted text. Use {@code uploadExtractAndFetch} to extract the text immediately.
     *
     * @param file The local file to be uploaded and processed.
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     */
    public TextExtractionResponse uploadAndStartExtraction(File file) throws TextExtractionException {
        return uploadAndStartExtraction(file, null);
    }

    /**
     * Uploads a local file and starts the text extraction process. The extracted text is saved as a new <b>Markdown</b> file, preserving the original
     * filename but using the {@code .md} extension by default. Output behavior can be customized using the {@link TextExtractionParameters}
     * parameter.
     * <p>
     * <b>Note:</b> This method does not return the extracted text. Use {@code uploadExtractAndFetch} to extract the text immediately.
     *
     * @param file The local file to be uploaded and processed.
     * @param parameters The configuration parameters for text extraction.
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     */
    public TextExtractionResponse uploadAndStartExtraction(File file, TextExtractionParameters parameters) throws TextExtractionException {
        requireNonNull(file);

        if (file.isDirectory())
            throw new TextExtractionException("directory_not_allowed", "The file can not be a directory");

        try {
            upload(new BufferedInputStream(new FileInputStream(file)), file.getName(), parameters, false);
            return startExtraction(file.getName(), parameters);
        } catch (FileNotFoundException e) {
            throw new TextExtractionException("file_not_found", e.getMessage(), e);
        }
    }

    /**
     * Uploads an InputStream and starts the asynchronous text extraction process. The extracted text is saved as a new <b>Markdown</b> file,
     * preserving the original filename but using the {@code .md} extension by default. To customize the output behavior you can use the
     * {@link TextExtractionParameters} class.
     *
     * <pre>
     * {@code
     * String uploadAndStartExtraction(InputStream is, String fileName, TextExtractionParameters parameters);
     * }
     * </pre>
     *
     * <b>Note:</b> This method does not return the extracted text. Use {@code uploadExtractAndFetch} to extract the text immediately.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @return The unique identifier of the text extraction process.
     */
    public TextExtractionResponse uploadAndStartExtraction(InputStream is, String fileName) throws TextExtractionException {
        return uploadAndStartExtraction(is, fileName, null);
    }

    /**
     * Uploads an InputStream and starts the asynchronous text extraction process. The extracted text is saved as a new <b>Markdown</b> file,
     * preserving the original filename but using the {@code .md} extension by default. Output behavior can be customized using the
     * {@link TextExtractionParameters} class.
     * <p>
     * <b>Note:</b> This method does not return the extracted text. Use {@code uploadExtractAndFetch} to extract the text immediately.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @param parameters The configuration parameters for text extraction.
     * @return The unique identifier of the text extraction process.
     */
    public TextExtractionResponse uploadAndStartExtraction(InputStream is, String fileName, TextExtractionParameters parameters)
        throws TextExtractionException {
        upload(is, fileName, parameters, false);
        return startExtraction(fileName, parameters);
    }

    /**
     * Starts the text extraction process for a file that is already present and returns the extracted text value. The extracted text is saved as a
     * new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by default. To customize the output behavior,
     * use the method with the {@link TextExtractionParameters} parameter.
     *
     * <pre>
     * {@code
     * String extractAndFetch(String absolutePath, TextExtractionParameters parameters);
     * }
     * </pre>
     *
     * <b>Note:</b> The default timeout value is set to 60 seconds.
     *
     * @param absolutePath The absolute path of the file.
     * @return The text extracted.
     */
    public String extractAndFetch(String absolutePath) throws TextExtractionException {
        return extractAndFetch(absolutePath, null);
    }

    /**
     * Starts the text extraction process for a file that is already present and returns the extracted text value. The extracted text is saved as a
     * new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by default. Output behavior can be customized
     * using the {@link TextExtractionParameters} class.
     * <p>
     * <b>Note:</b> This method waits until the extraction process is complete and returns the extracted value. The default timeout value is set to 60
     * seconds.
     *
     * @param absolutePath The path of the document to extract text from.
     * @param parameters The configuration parameters for text extraction.
     * @return The text extracted.
     */
    public String extractAndFetch(String absolutePath, TextExtractionParameters parameters) throws TextExtractionException {

        if (nonNull(parameters)) {
            if (parameters.getRequestedOutputs().size() > 1) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed if more than one file is to be generated");
            }
            if (parameters.getRequestedOutputs().size() == 1 && parameters.getRequestedOutputs().get(0).equals(PAGE_IMAGES.value())) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed for the type \"page_images\"");
            }
        }

        var textExtractionResponse = startExtraction(absolutePath, parameters, true);
        var is = getExtractedText(textExtractionResponse, parameters);

        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TextExtractionException("fetch_operation_failed", "Failed to fetch the extracted text", e);
        }
    }

    /**
     * Uploads a local file, starts text extraction process and returns the extracted text value. The extracted text is saved as a new <b>Markdown</b>
     * file, preserving the original filename but using the {@code .md} extension by default. To customize the output behavior, use the method with
     * the {@link Parameters} class.
     *
     * <pre>
     * {@code
     * String uploadExtractAndFetch(File file, TextExtractionParameters parameters);
     * }
     * </pre>
     *
     * <b>Note:</b> This method waits until the extraction process is complete and returns the extracted value. The default timeout value is set to 60
     * seconds.
     *
     * @param file The local file to be uploaded and processed.
     * @return The text extracted.
     */
    public String uploadExtractAndFetch(File file) throws TextExtractionException {
        return uploadExtractAndFetch(file, null);
    }

    /**
     * Uploads a local file and starts the text extraction process. The extracted text is saved as a new <b>Markdown</b> file, preserving the original
     * filename but using the {@code .md} extension by default. Output behavior can be customized using the {@link TextExtractionParameters} class.
     * <p>
     * <b>Notes:</b> This method waits until the extraction process is complete and returns the extracted value. The default timeout value is set to
     * 60 seconds.
     *
     * @param file The local file to be uploaded and processed.
     * @param parameters The configuration parameters for text extraction.
     * @return The text extracted.
     */
    public String uploadExtractAndFetch(File file, TextExtractionParameters parameters) throws TextExtractionException {

        if (nonNull(parameters)) {
            if (parameters.getRequestedOutputs().size() > 1) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed if more than one file is to be generated");
            }
            if (parameters.getRequestedOutputs().size() == 1 && parameters.getRequestedOutputs().get(0).equals(PAGE_IMAGES.value())) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed for the type \"page_images\"");
            }
        }

        try {
            upload(new BufferedInputStream(new FileInputStream(file)), file.getName(), parameters, true);
        } catch (FileNotFoundException e) {
            throw new TextExtractionException("file_not_found", e.getMessage(), e);
        }
        return extractAndFetch(file.getName(), parameters);
    }

    /**
     * Uploads an InputStream, starts text extraction process and returns the extracted text value. The extracted text is saved as a new
     * <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by default. To customize the output behavior, use
     * the method with the {@link Parameters} class.
     *
     * <pre>
     * {@code
     * String uploadExtractAndFetch(InputStream is, String fileName, TextExtractionParameters parameters);
     * }
     * </pre>
     *
     * <li><b>Note:</b> This method waits until the extraction process is complete and returns the extracted value. The default timeout value is set
     * to 60 seconds.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @return The text extracted.
     */
    public String uploadExtractAndFetch(InputStream is, String fileName) throws TextExtractionException {
        return uploadExtractAndFetch(is, fileName, null);
    }

    /**
     * Uploads an InputStream and starts the text extraction process. The extracted text is saved as a new <b>Markdown</b> file, preserving the
     * original filename but using the {@code .md} extension by default. Output behavior can be customized using the {@link TextExtractionParameters}
     * class.
     * <p>
     * <b>Note:</b> This method waits until the extraction process is complete and returns the extracted value. The default timeout value is set to 60
     * seconds.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @param parameters The configuration parameters for text extraction.
     * @return The text extracted.
     */
    public String uploadExtractAndFetch(InputStream is, String fileName, TextExtractionParameters parameters) throws TextExtractionException {

        if (nonNull(parameters)) {
            if (parameters.getRequestedOutputs().size() > 1) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed if more than one file is to be generated");
            }
            if (parameters.getRequestedOutputs().size() == 1 && parameters.getRequestedOutputs().get(0).equals(PAGE_IMAGES.value())) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed for the type \"page_images\"");
            }
        }

        upload(is, fileName, parameters, true);
        return extractAndFetch(fileName, parameters);
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
        var uri = URI.create(url.toString() + "%s/extractions/%s?%s".formatted(ML_API_TEXT_PATH, id, queryParameters));

        var httpRequest = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .timeout(timeout)
            .GET().build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return fromJson(httpReponse.body(), TextExtractionResponse.class);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a file from the specified bucket.
     *
     * @param bucketName The name of the bucket.
     * @param fileName The name of the file to delete.
     * @return true if the file was successfully deleted, false otherwise.
     */
    public boolean deleteFile(String bucketName, String fileName) {
        try {
            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucketName, encodedFileName));
            var httpRequest = HttpRequest.newBuilder(uri).DELETE().timeout(timeout).build();
            var response = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            return response.statusCode() == 204;
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    * Reads a file from the specified bucket.
    *
    * @param bucketName The name of the bucket.
    * @param fileName The name of the file to read.
    */
    public String readFile(String bucketName, String fileName) {
        try {
            var encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            var uri = URI.create(cosUrl + "/%s/%s".formatted(bucketName, encodedFileName));
            var httpRequest = HttpRequest.newBuilder(uri).GET().timeout(timeout).build();
            return syncHttpClient.send(httpRequest, BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a text extraction request.
     *
     * @param id The unique identifier of the text extraction request to delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id) {
        return deleteRequest(id, TextExtractionDeleteParameters.builder().build());
    }

    /**
     * Deletes a text extraction request.
     * <p>
     * This operation cancels the specified text extraction request. If the {@code hardDelete} parameter is set to {@code true}, it will also delete
     * the associated job metadata.
     *
     * @param id The unique identifier of the text extraction request to delete.
     * @param parameters Parameters specifying the space or project context, and whether to perform a hard delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id, TextExtractionDeleteParameters parameters) {
        requireNonNull(id, "The id can not be null");

        var projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
        var spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;

        var queryParameters = parameters.getHardDelete()
            .map(nullable -> getQueryParameters(projectId, spaceId).concat("&hard_delete=true"))
            .orElse(getQueryParameters(projectId, spaceId));

        var uri = URI.create(url.toString() + "%s/extractions/%s?%s".formatted(ML_API_TEXT_PATH, id, queryParameters));
        var httpRequest = HttpRequest.newBuilder(uri).DELETE().timeout(timeout).build();

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

    //
    // Generates query parameters for API requests based on provided project_id or space_id.
    //
    private String getQueryParameters(String projectId, String spaceId) {
        if (nonNull(projectId))
            return "version=%s&project_id=%s".formatted(version, URLEncoder.encode(projectId, Charset.defaultCharset()));
        else
            return "version=%s&space_id=%s".formatted(version, URLEncoder.encode(spaceId, Charset.defaultCharset()));
    }

    //
    // Uploads an inputstream to the Cloud Object Storage.
    //
    private void upload(InputStream is, String fileName, TextExtractionParameters parameters, boolean waitForExtraction) {
        requireNonNull(is, "is value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");

        boolean removeOutputFile = false;
        boolean removeUploadedFile = false;
        CosReference documentReference = this.documentReference;

        if (nonNull(parameters)) {
            removeOutputFile = parameters.isRemoveOutputFile();
            removeUploadedFile = parameters.isRemoveUploadedFile();
            documentReference = requireNonNullElse(parameters.getDocumentReference(), this.documentReference);
        }

        if (!waitForExtraction && (removeOutputFile || removeUploadedFile))
            throw new IllegalArgumentException(
                "The asynchronous version of startExtraction doesn't allow the use of the \"removeOutputFile\" and \"removeUploadedFile\" parameters");

        try {
            String encodedFileName = new URI(null, null, fileName, null).toASCIIString();
            URI uri = URI.create(cosUrl + "/%s/%s".formatted(documentReference.bucket(), encodedFileName));
            HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(uri.toASCIIString()))
                .PUT(BodyPublishers.ofInputStream(() -> is))
                .timeout(timeout)
                .build();
            syncHttpClient.send(httpRequest, BodyHandlers.ofString());
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    //
    // Starts the text extraction process.
    //
    private TextExtractionResponse startExtraction(String path, TextExtractionParameters parameters, boolean waitUntilJobIsDone)
        throws TextExtractionException {
        requireNonNull(path);

        String outputFileName = null;
        String projectId = this.projectId;
        String spaceId = this.spaceId;
        boolean removeOutputFile = false;
        boolean removeUploadedFile = false;
        List<String> requestedOutputs = List.of("plain_text");
        CosReference documentReference = this.documentReference;
        CosReference resultReference = this.resultReference;
        Parameters params = null;
        Map<String, Object> custom = null;
        Duration timeout = Duration.ofSeconds(60);

        if (nonNull(parameters)) {
            removeOutputFile = parameters.isRemoveOutputFile();
            removeUploadedFile = parameters.isRemoveUploadedFile();
            outputFileName = parameters.getOutputFileName();
            projectId = nonNull(parameters.getProjectId()) ? parameters.getProjectId() : this.projectId;
            spaceId = nonNull(parameters.getSpaceId()) ? parameters.getSpaceId() : this.spaceId;
            requestedOutputs = requireNonNullElse(parameters.getRequestedOutputs(), requestedOutputs);
            documentReference = requireNonNullElse(parameters.getDocumentReference(), this.documentReference);
            resultReference = requireNonNullElse(parameters.getResultReference(), this.resultReference);
            params = parameters.toParameters();
            custom = parameters.getCustom();
            timeout = requireNonNullElse(parameters.getTimeout(), Duration.ofSeconds(60));
        }

        if (!waitUntilJobIsDone && (removeOutputFile || removeUploadedFile))
            throw new IllegalArgumentException(
                "The asynchronous version of startExtraction doesn't allow the use of the \"removeOutputFile\" and \"removeUploadedFile\" parameters");

        if (isNull(outputFileName)) {

            var isMultiOutput =
                requestedOutputs.size() > 1 || requestedOutputs.get(0).equals(PAGE_IMAGES.value()) ? true : false;

            if (isMultiOutput) {

                outputFileName = "/";

            } else {

                var type = Type.fromValue(requestedOutputs.get(0));
                outputFileName = TextExtractionUtils.addExtension(path, type);
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
            HttpRequest.newBuilder(URI.create(url.toString() + "%s/extractions?version=%s".formatted(ML_API_TEXT_PATH, version)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(toJson(request)))
                .timeout(timeout)
                .build();

        try {

            var httpReponse = syncHttpClient.send(httpRequest, BodyHandlers.ofString());
            var response = fromJson(httpReponse.body(), TextExtractionResponse.class);

            if (!waitUntilJobIsDone)
                return response;

            Status status;
            long sleepTime = 100;
            LocalTime endTime = LocalTime.now().plus(timeout);

            do {

                if (LocalTime.now().isAfter(endTime))
                    throw new TextExtractionException("timeout",
                        "Execution to extract %s file took longer than the timeout set by %s milliseconds"
                            .formatted(path, timeout.toMillis()));

                try {

                    Thread.sleep(sleepTime);
                    sleepTime *= 2;
                    sleepTime = Math.min(sleepTime, 3000);

                } catch (Exception e) {
                    throw new TextExtractionException("interrupted", e.getMessage());
                }

                var processId = response.metadata().id();
                response = fetchExtractionRequest(processId, TextExtractionFetchParameters.builder()
                    .projectId(projectId)
                    .spaceId(spaceId)
                    .build());

                status = Status.fromValue(response.entity().results().status());
                var pagesProcessed = response.entity().results().numberPagesProcessed();
                logger.debug("Extraction status: {} for the file {} (pages processed {})", status, path, pagesProcessed);

            } while (status != Status.FAILED && status != Status.COMPLETED);

            return response;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    //
    // Retrieves the extracted text from a specified Cloud Object Storage file.
    //
    private InputStream getExtractedText(TextExtractionResponse textExtractionResponse, TextExtractionParameters parameters)
        throws TextExtractionException {

        String cosUrl = this.cosUrl;
        String uploadedPath = textExtractionResponse.entity().documentReference().location().fileName();
        String outputPath = textExtractionResponse.entity().resultsReference().location().fileName();
        Status status = Status.fromValue(textExtractionResponse.entity().results().status());
        boolean removeUploadedFile = false;
        boolean removeOutputFile = false;
        CosReference documentReference = this.documentReference;
        CosReference resultsReference = this.resultReference;

        if (nonNull(parameters)) {
            cosUrl = requireNonNullElse(parameters.getCosUrl(), this.cosUrl);
            removeUploadedFile = parameters.isRemoveUploadedFile();
            removeOutputFile = parameters.isRemoveOutputFile();
            documentReference = requireNonNullElse(parameters.getDocumentReference(), this.documentReference);
            resultsReference = requireNonNullElse(parameters.getDocumentReference(), this.resultReference);
        }

        String documentBucketName = documentReference.bucket();
        String resultsBucketName = resultsReference.bucket();

        try {

            InputStream extractedFile = switch(status) {
                case COMPLETED -> {
                    try {
                        var encodedFileName = new URI(null, null, outputPath, null).toASCIIString();
                        var uri = URI.create(cosUrl + "/%s/%s".formatted(resultsBucketName, encodedFileName));
                        var httpRequest = HttpRequest
                            .newBuilder()
                            .uri(URI.create(uri.toASCIIString()))
                            .timeout(timeout)
                            .GET().build();
                        yield syncHttpClient.send(httpRequest, BodyHandlers.ofInputStream()).body();
                    } catch (IOException | InterruptedException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
                case FAILED -> {
                    Error error = textExtractionResponse.entity().results().error();
                    throw new TextExtractionException(error.code(), error.message());
                }
                default -> throw new TextExtractionException("generic_error",
                    "Status %s not managed".formatted(status));
            };

            if (removeOutputFile) {
                try {
                    var encodedFileName = new URI(null, null, outputPath, null).toASCIIString();
                    var uri = URI.create(cosUrl + "/%s/%s".formatted(resultsBucketName, encodedFileName));
                    var httpRequest = HttpRequest.newBuilder(URI.create(uri.toASCIIString()))
                        .timeout(timeout)
                        .DELETE().build();
                    asyncHttpClient.send(httpRequest, BodyHandlers.ofString());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }

            return extractedFile;

        } finally {
            if (removeUploadedFile) {
                try {
                    var encodedFileName = new URI(null, null, uploadedPath, null).toASCIIString();
                    var uri = URI.create(cosUrl + "/%s/%s".formatted(documentBucketName, encodedFileName));
                    var httpRequest = HttpRequest.newBuilder(URI.create(uri.toASCIIString()))
                        .timeout(timeout)
                        .DELETE().build();
                    asyncHttpClient.send(httpRequest, BodyHandlers.ofString());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextExtractionService textExtractionService = TextExtractionService.builder()
     *   .url("https://...") // or use CloudRegion
     *   .cosUrl("https://...") // or use CosUrl
     *   .authenticationProvider(authProvider)
     *   .projectId("my-project-id")
     *   .documentReference("<connection_id>", "<bucket-name")
     *   .resultReference("<connection_id>", "<bucket-name")
     *   .build();
     *
     * TextExtractionResponse response = textExtractionService.startExtraction("myfile.pdf")
     * }</pre>
     *
     * For more information, see the <a href="https://cloud.ibm.com/apidocs/watsonx-ai#text-extraction" target="_blank"> official documentation</a>.
     *
     * @see AuthenticationProvider
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextExtractionService} instances with configurable parameters.
     */
    public static class Builder extends WatsonxService.Builder<Builder> {
        private String cosUrl;
        private CosReference documentReference;
        private CosReference resultReference;

        public Builder cosUrl(String cosUrl) {
            this.cosUrl = cosUrl;
            return this;
        }

        public Builder cosUrl(CosUrl cosUrl) {
            requireNonNull(cosUrl, "cosUrl cannot be null");
            return cosUrl(cosUrl.value());
        }

        /**
         * Specifies the Cloud Object Storage connection and bucket where the input files are stored.
         *
         * @param documentReference Reference to the Cloud Object Storage.
         */
        public Builder documentReference(CosReference documentReference) {
            this.documentReference = documentReference;
            return this;
        }

        /**
         * Specifies the Cloud Object Storage connection and bucket where the input files are stored.
         *
         * @param connectionId The id of the COS connection asset.
         * @param bucket The name of the bucket containing the input documents.
         */
        public Builder documentReference(String connectionId, String bucket) {
            return documentReference(CosReference.of(connectionId, bucket));
        }

        /**
         * Specifies the Cloud Object Storage connection and bucket where the extracted results should be stored.
         *
         * @param resultReference Reference to the Cloud Object Storage.
         */
        public Builder resultReference(CosReference resultReference) {
            this.resultReference = resultReference;
            return this;
        }

        /**
         * Specifies the Cloud Object Storage connection and bucket where the extracted results should be stored.
         *
         * @param connectionId The id of the COS connection asset.
         * @param bucket The name of the bucket where results will be written.
         */
        public Builder resultReference(String connectionId, String bucket) {
            return resultReference(CosReference.of(connectionId, bucket));
        }

        /**
         * <b>Not supported for TextExtractionService</b>
         */
        @Override
        @Deprecated
        public Builder foundationModelService(FoundationModelService foundationModelService) {
            throw new UnsupportedOperationException("Not supported in TextExtractionService");
        }

        /**
         * <b>Not supported for TextExtractionService</b>
         */
        @Override
        @Deprecated
        public Builder modelId(String modelId) {
            throw new UnsupportedOperationException("Not supported in TextExtractionService");
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
