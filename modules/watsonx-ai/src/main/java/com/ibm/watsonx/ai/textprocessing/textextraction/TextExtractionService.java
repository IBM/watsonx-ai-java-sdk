/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import static com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters.Type.MD;
import static com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters.Type.PAGE_IMAGES;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.CosUrl;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.Error;
import com.ibm.watsonx.ai.textprocessing.ReadFileRequest;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters.Type;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionRestClient.DeleteExtractionRequest;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionRestClient.FetchExtractionDetailsRequest;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionRestClient.StartExtractionRequest;

/**
 * Service class to interact with IBM watsonx.ai Text Extraction APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextExtractionService textExtractionService = TextExtractionService.builder()
 *   .baseUrl("https://...")    // or use CloudRegion
 *   .cosUrl("https://...")     // or use CosUrl
 *   .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
 *   .projectId("my-project-id")
 *   .documentReference("<connection_id>", "<bucket-name>")
 *   .resultReference("<connection_id>", "<bucket-name>")
 *   .build();
 *
 * TextExtractionResponse response = textExtractionService.startExtraction("myfile.pdf")
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class TextExtractionService extends ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(TextExtractionService.class);
    private final String cosUrl;
    private final CosReference documentReference;
    private final CosReference resultReference;
    private final TextExtractionRestClient client;

    private TextExtractionService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        var tmpUrl = requireNonNull(builder.cosUrl, "cosUrl value cannot be null");
        cosUrl = tmpUrl.endsWith("/") ? tmpUrl.substring(0, tmpUrl.length() - 1) : tmpUrl;
        documentReference = requireNonNull(builder.documentReference, "documentReference value cannot be null");
        resultReference = requireNonNull(builder.resultReference, "resultReference value cannot be null");
        client = TextExtractionRestClient.builder()
            .cosUrl(cosUrl)
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .build();
    }

    /**
     * Starts the text extraction process for a document that already exists in the configured {@link #documentReference document reference}.
     * <p>
     * The {@code absolutePath} parameter identifies the location of the file <b>inside the document reference</b> (not a local filesystem path). The
     * extracted text is saved as a new <b>Markdown</b> file in the configured {@link #resultReference result reference}, preserving the original
     * filename but using the {@code .md} extension. To customize the output behavior, use the overloaded method with
     * {@link TextExtractionParameters}.
     * <p>
     * If you want to process a <b>local file</b>, use {@link #uploadAndStartExtraction(File)} instead.
     * <p>
     * <b>Note:</b> This method does not return the extracted text content. Use {@link #extractAndFetch(String)} to run the extraction and fetch the
     * result immediately.
     *
     * @param absolutePath The location of the document to be processed.
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartExtraction(File)
     * @see #extractAndFetch(String)
     */
    public TextExtractionResponse startExtraction(String absolutePath) throws TextExtractionException {
        return startExtraction(absolutePath, null);
    }

    /**
     * Starts the text extraction process for a document that already exists in the configured {@link #documentReference document reference}.
     * <p>
     * The {@code absolutePath} parameter identifies the location of the file <b>inside the document reference</b> (not a local filesystem path). The
     * extracted text is saved as a new <b>Markdown</b> file in the configured {@link #resultReference result reference}, preserving the original
     * filename but using the {@code .md} extension.
     * <p>
     * If you want to process a <b>local file</b>, use {@link #uploadAndStartExtraction(File, TextExtractionParameters)} instead.
     * <p>
     * <b>Note:</b> This method does not return the extracted text content. Use {@link #extractAndFetch(String, String, TextExtractionParameters)} to
     * run the extraction and fetch the result immediately.
     *
     * @param absolutePath The location of the document to be processed.
     * @param parameters The configuration parameters for text extraction.
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartExtraction(File, TextExtractionParameters)
     * @see #extractAndFetch(String, TextExtractionParameters)
     */
    public TextExtractionResponse startExtraction(String absolutePath, TextExtractionParameters parameters) throws TextExtractionException {
        return startExtraction(UUID.randomUUID().toString(), absolutePath, parameters, false);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference} and starts the text extraction process. The extracted text
     * is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by default. To customize the
     * output behavior, use the overloaded method with {@link TextExtractionParameters}.
     * <p>
     * <b>Note:</b> This method does not return the extracted text. Use {@link #uploadExtractAndFetch(File)} to extract the text immediately.
     *
     * @param file The local file to be uploaded and processed.
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartExtraction(File, TextExtractionParameters)
     * @see #uploadExtractAndFetch(File)
     */
    public TextExtractionResponse uploadAndStartExtraction(File file) throws TextExtractionException {
        return uploadAndStartExtraction(file, null);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference} and starts the text extraction process. The extracted text
     * is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by default.
     * <p>
     * <b>Note:</b> This method does not return the extracted text. Use {@link #uploadExtractAndFetch(File, TextExtractionParameters)} to extract the
     * text immediately.
     *
     * @param file The local file to be uploaded and processed.
     * @param parameters The configuration parameters for text extraction.
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     * @see #uploadExtractAndFetch(File, TextExtractionParameters)
     */
    public TextExtractionResponse uploadAndStartExtraction(File file, TextExtractionParameters parameters) throws TextExtractionException {
        requireNonNull(file);

        if (file.isDirectory())
            throw new TextExtractionException("directory_not_allowed", "The file can not be a directory");

        var requestId = UUID.randomUUID().toString();

        try {
            upload(requestId, new BufferedInputStream(new FileInputStream(file)), file.getName(), parameters, false);
            return startExtraction(requestId, file.getName(), parameters, false);
        } catch (FileNotFoundException e) {
            throw new TextExtractionException("file_not_found", e.getMessage(), e);
        }
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference} and starts the asynchronous text extraction
     * process. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by
     * default. To customize the output behavior, use the overloaded method with {@link TextExtractionParameters}.
     * <p>
     * <b>Note:</b> This method does not return the extracted text. Use {@link #uploadExtractAndFetch(InputStream, String)} to extract the text
     * immediately.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @return The unique identifier of the text extraction process.
     * @see #uploadAndStartExtraction(InputStream, String, TextExtractionParameters)
     * @see #uploadExtractAndFetch(InputStream, String)
     */
    public TextExtractionResponse uploadAndStartExtraction(InputStream is, String fileName) throws TextExtractionException {
        return uploadAndStartExtraction(is, fileName, null);
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference} and starts the asynchronous text extraction
     * process. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by
     * default.
     * <p>
     * <b>Note:</b> This method does not return the extracted text. Use {@link #uploadExtractAndFetch(InputStream, String, TextExtractionParameters)}
     * to extract the text immediately.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @param parameters The configuration parameters for text extraction.
     * @return The unique identifier of the text extraction process.
     * @see #uploadExtractAndFetch(InputStream, String, TextExtractionParameters)
     */
    public TextExtractionResponse uploadAndStartExtraction(InputStream is, String fileName, TextExtractionParameters parameters)
        throws TextExtractionException {
        var requestId = UUID.randomUUID().toString();
        upload(requestId, is, fileName, parameters, false);
        return startExtraction(requestId, fileName, parameters, false);
    }

    /**
     * Starts the text extraction process for a file that is already present in the configured {@link #documentReference document reference} and
     * returns the extracted text value. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the
     * {@code .md} extension by default. To customize the output behavior, use the overloaded method with {@link TextExtractionParameters}.
     *
     * @param absolutePath The absolute path of the file.
     * @return The text extracted.
     * @see #extractAndFetch(String, String, TextExtractionParameters)
     */
    public String extractAndFetch(String absolutePath) throws TextExtractionException, FileNotFoundException {
        return extractAndFetch(absolutePath, null);
    }

    /**
     * Starts the text extraction process for a file that is already present in the configured {@link #documentReference document reference} and
     * returns the extracted text value. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the
     * {@code .md} extension by default.
     *
     * @param absolutePath The path of the document to extract text from.
     * @param parameters The configuration parameters for text extraction.
     * @return The text extracted.
     */
    public String extractAndFetch(String absolutePath, TextExtractionParameters parameters) throws TextExtractionException, FileNotFoundException {
        return extractAndFetch(UUID.randomUUID().toString(), absolutePath, parameters);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference}, starts text extraction process and returns the extracted
     * text value. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by
     * default. To customize the output behavior, use the overloaded method with {@link TextExtractionParameters}.
     *
     * @param file The local file to be uploaded and processed.
     * @return The text extracted.
     * @see #uploadExtractAndFetch(File, TextExtractionParameters)
     */
    public String uploadExtractAndFetch(File file) throws TextExtractionException, FileNotFoundException {
        return uploadExtractAndFetch(file, null);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference}, starts text extraction process and returns the extracted
     * text value. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by
     * default.
     *
     * @param file The local file to be uploaded and processed.
     * @param parameters The configuration parameters for text extraction.
     * @return The text extracted.
     */
    public String uploadExtractAndFetch(File file, TextExtractionParameters parameters) throws TextExtractionException, FileNotFoundException {

        if (nonNull(parameters)) {
            if (parameters.requestedOutputs().size() > 1) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed if more than one file is to be generated");
            }
            if (parameters.requestedOutputs().size() == 1 && parameters.requestedOutputs().get(0).equals(PAGE_IMAGES.value())) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed for the type \"page_images\"");
            }
        }

        var requestId = UUID.randomUUID().toString();

        upload(requestId, new BufferedInputStream(new FileInputStream(file)), file.getName(), parameters, true);
        return extractAndFetch(requestId, file.getName(), parameters);
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference}, starts text extraction process and returns the
     * extracted text value. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md}
     * extension by default. To customize the output behavior, use the overloaded method with {@link TextExtractionParameters}.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @return The text extracted.
     * @see #uploadExtractAndFetch(InputStream, String, TextExtractionParameters)
     */
    public String uploadExtractAndFetch(InputStream is, String fileName) throws TextExtractionException {
        return uploadExtractAndFetch(is, fileName, null);
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference}, starts text extraction process and returns the
     * extracted text value. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md}
     * extension by default.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @param parameters The configuration parameters for text extraction.
     * @return The text extracted.
     */
    public String uploadExtractAndFetch(InputStream is, String fileName, TextExtractionParameters parameters) throws TextExtractionException {

        if (nonNull(parameters)) {
            if (parameters.requestedOutputs().size() > 1) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed if more than one file is to be generated");
            }
            if (parameters.requestedOutputs().size() == 1 && parameters.requestedOutputs().get(0).equals(PAGE_IMAGES.value())) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed for the type \"page_images\"");
            }
        }

        var requestId = UUID.randomUUID().toString();
        upload(requestId, is, fileName, parameters, true);

        try {
            return extractAndFetch(requestId, fileName, parameters);
        } catch (FileNotFoundException e) {
            // This should never happen.
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
        return fetchExtractionRequest(UUID.randomUUID().toString(), id, parameters);
    }

    /**
     * Uploads a file in the configured {@link #documentReference document reference}.
     *
     * @param file the file to be uploaded
     * @return {@code true} if the upload request was successfully sent
     * @throws TextExtractionException if the file cannot be found or an error occurs during upload
     */
    public boolean uploadFile(File file) throws TextExtractionException {
        try {
            return uploadFile(new BufferedInputStream(new FileInputStream(file)), file.getName());
        } catch (FileNotFoundException e) {
            throw new TextExtractionException("file_not_found", e.getMessage(), e);
        }
    }

    /**
     * Uploads an input stream in the configured {@link #documentReference document reference}.
     *
     * @param inputStream the input stream to be uploaded
     * @param fileName the name of the file associated with the input stream
     * @return {@code true} if the upload request was successfully sent
     */
    public boolean uploadFile(InputStream inputStream, String fileName) {
        var requestId = UUID.randomUUID().toString();
        upload(requestId, inputStream, fileName, null, false);
        return true;
    }

    /**
     * Deletes a file from the specified bucket.
     *
     * @param bucketName The name of the bucket.
     * @param fileName The name of the file to delete.
     * @return true if the file was successfully deleted, false otherwise.
     */
    public boolean deleteFile(String bucketName, String fileName) throws FileNotFoundException {
        return client.deleteFile(DeleteFileRequest.of(bucketName, fileName));
    }

    /*
    * Reads a file from the specified bucket.
    *
    * @param bucketName The name of the bucket.
    * @param fileName The name of the file to read.
    */
    public String readFile(String bucketName, String fileName) throws FileNotFoundException {
        return client.readFile(ReadFileRequest.of(bucketName, fileName));
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

        var builder = TextExtractionDeleteParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .hardDelete(parameters.hardDelete().orElse(null))
            .build();

        var request = DeleteExtractionRequest.of(parameters.transactionId(), id, p);
        return client.deleteExtraction(request);
    }

    //
    // Retrieves the results of a text extraction request by its unique identifier.
    //
    private TextExtractionResponse fetchExtractionRequest(String requestId, String id, TextExtractionFetchParameters parameters) {
        requireNonNull(requestId, "The requestId can not be null");
        requireNonNull(id, "The id can not be null");

        var builder = TextExtractionFetchParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .build();

        var request = FetchExtractionDetailsRequest.of(requestId, id, p);
        return client.fetchExtractionDetails(request);
    }

    //
    // Start the text extraction and wait until the result is ready.
    //
    private String extractAndFetch(String requestId, String absolutePath, TextExtractionParameters parameters)
        throws TextExtractionException, FileNotFoundException {
        requireNonNull(requestId, "requestId cannot be null");

        if (nonNull(parameters)) {
            if (parameters.requestedOutputs().size() > 1) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed if more than one file is to be generated");
            }
            if (parameters.requestedOutputs().size() == 1 && parameters.requestedOutputs().get(0).equals(PAGE_IMAGES.value())) {
                throw new TextExtractionException("fetch_operation_not_allowed",
                    "The fetch operation cannot be executed for the type \"page_images\"");
            }
        }

        var textExtractionResponse = startExtraction(requestId, absolutePath, parameters, true);
        return getExtractedText(requestId, textExtractionResponse, parameters);
    }

    //
    // Uploads an inputstream to the Cloud Object Storage.
    //
    private void upload(String requestId, InputStream is, String fileName, TextExtractionParameters parameters, boolean waitForExtraction) {
        requireNonNull(requestId, "requestId value cannot be null");
        requireNonNull(is, "is value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");

        boolean removeOutputFile = false;
        boolean removeUploadedFile = false;
        CosReference documentReference = this.documentReference;

        if (nonNull(parameters)) {
            removeOutputFile = parameters.isRemoveOutputFile();
            removeUploadedFile = parameters.isRemoveUploadedFile();
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
        }

        if (!waitForExtraction && (removeOutputFile || removeUploadedFile))
            throw new IllegalArgumentException(
                "The asynchronous version of startExtraction doesn't allow the use of the \"removeOutputFile\" and \"removeUploadedFile\" parameters");
        var request = UploadRequest.of(requestId, documentReference.bucket(), is, fileName);
        client.upload(request);
    }

    //
    // Starts the text extraction process.
    //
    private TextExtractionResponse startExtraction(String requestId, String path, TextExtractionParameters parameters, boolean waitUntilJobIsDone)
        throws TextExtractionException {
        requireNonNull(path);
        requireNonNull(requestId);

        String outputFileName = null;
        String projectId = null;
        String spaceId = null;
        boolean removeOutputFile = false;
        boolean removeUploadedFile = false;
        List<String> requestedOutputs = List.of(MD.value());
        CosReference documentReference = this.documentReference;
        CosReference resultReference = this.resultReference;
        Parameters params = null;
        Map<String, Object> custom = null;
        Duration timeout = Duration.ofSeconds(60);
        String transactionId = null;

        if (nonNull(parameters)) {
            removeOutputFile = parameters.isRemoveOutputFile();
            removeUploadedFile = parameters.isRemoveUploadedFile();
            outputFileName = parameters.outputFileName();
            projectId = parameters.projectId();
            spaceId = parameters.spaceId();
            requestedOutputs = requireNonNullElse(parameters.requestedOutputs(), requestedOutputs);
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
            resultReference = requireNonNullElse(parameters.resultReference(), this.resultReference);
            params = parameters.toParameters();
            custom = parameters.custom();
            timeout = requireNonNullElse(parameters.timeout(), Duration.ofSeconds(60));
            transactionId = parameters.transactionId();
        } else {
            params = Parameters.of(requestedOutputs);
        }

        if (isNull(projectId) && isNull(spaceId)) {
            projectId = this.projectId;
            spaceId = this.spaceId;
        }

        if (!waitUntilJobIsDone && (removeOutputFile || removeUploadedFile))
            throw new IllegalArgumentException(
                "The asynchronous version of startExtraction doesn't allow the use of the \"removeOutputFile\" and \"removeUploadedFile\" parameters");

        var isMultiOutput =
            requestedOutputs.size() > 1 || requestedOutputs.get(0).equals(PAGE_IMAGES.value()) ? true : false;

        if (isNull(outputFileName)) {

            if (isMultiOutput) {

                outputFileName = "/";

            } else {

                var type = Type.fromValue(requestedOutputs.get(0));
                outputFileName = TextExtractionUtils.addExtension(path, type);
            }

        } else {

            var isDirectory = outputFileName.endsWith("/");
            if (isDirectory && !isMultiOutput) {
                var type = Type.fromValue(requestedOutputs.get(0));
                outputFileName = outputFileName + TextExtractionUtils.addExtension(path, type);
            }
        }

        var textExtractionRequest = new TextExtractionRequest(
            projectId,
            spaceId,
            documentReference.toDataReference(path),
            resultReference.toDataReference(outputFileName),
            params,
            custom
        );

        var request = StartExtractionRequest.of(requestId, transactionId, textExtractionRequest);
        var response = client.startExtraction(request);

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
            response = fetchExtractionRequest(requestId, processId, TextExtractionFetchParameters.builder()
                .projectId(projectId)
                .spaceId(spaceId)
                .build());

            status = Status.fromValue(response.entity().results().status());
            var pagesProcessed = response.entity().results().numberPagesProcessed();
            logger.debug("Extraction status: {} for the file {} (pages processed {})", status, path, pagesProcessed);

        } while (status != Status.FAILED && status != Status.COMPLETED);

        return response;
    }

    //
    // Retrieves the extracted text from a specified Cloud Object Storage file.
    //
    private String getExtractedText(String requestId, TextExtractionResponse textExtractionResponse, TextExtractionParameters parameters)
        throws TextExtractionException, FileNotFoundException {

        requireNonNull(requestId);

        String uploadedPath = textExtractionResponse.entity().documentReference().location().fileName();
        String outputPath = textExtractionResponse.entity().resultsReference().location().fileName();
        Status status = Status.fromValue(textExtractionResponse.entity().results().status());
        boolean removeUploadedFile = false;
        boolean removeOutputFile = false;
        CosReference documentReference = this.documentReference;
        CosReference resultsReference = this.resultReference;

        if (nonNull(parameters)) {
            removeUploadedFile = parameters.isRemoveUploadedFile();
            removeOutputFile = parameters.isRemoveOutputFile();
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
            resultsReference = requireNonNullElse(parameters.documentReference(), this.resultReference);
        }

        String documentBucketName = documentReference.bucket();
        String resultsBucketName = resultsReference.bucket();

        try {

            String extractedFile = switch(status) {
                case COMPLETED -> {
                    var request = ReadFileRequest.of(requestId, documentBucketName, outputPath);
                    yield client.readFile(request);
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
                    var request = DeleteFileRequest.of(requestId, resultsBucketName, encodedFileName);
                    client.asyncDeleteFile(request);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }

            return extractedFile;

        } finally {
            if (removeUploadedFile) {
                try {
                    var encodedFileName = new URI(null, null, uploadedPath, null).toASCIIString();
                    var request = DeleteFileRequest.of(requestId, resultsBucketName, encodedFileName);
                    client.asyncDeleteFile(request);
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
     *   .baseUrl("https://...")    // or use CloudRegion
     *   .cosUrl("https://...")     // or use CosUrl
     *   .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
     *   .projectId("my-project-id")
     *   .documentReference("<connection_id>", "<bucket-name>")
     *   .resultReference("<connection_id>", "<bucket-name>")
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
    public final static class Builder extends ProjectService.Builder<Builder> {
        private String cosUrl;
        private CosReference documentReference;
        private CosReference resultReference;

        private Builder() {}

        /**
         * Specifies the Cloud Object Storage (COS) base URL to be used for reading and writing files.
         *
         * @param cosUrl The base COS URL as a string.
         */
        public Builder cosUrl(String cosUrl) {
            this.cosUrl = cosUrl;
            return this;
        }

        /**
         * Specifies the Cloud Object Storage (COS) base URL to be used for reading and writing files.
         *
         * @param cosUrl A {@link CosUrl} instance wrapping the COS base URL.
         */
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
         * Builds a {@link TextExtractionService} instance using the configured parameters.
         *
         * @return a new instance of {@link TextExtractionService}
         */
        public TextExtractionService build() {
            return new TextExtractionService(this);
        }
    }
}
