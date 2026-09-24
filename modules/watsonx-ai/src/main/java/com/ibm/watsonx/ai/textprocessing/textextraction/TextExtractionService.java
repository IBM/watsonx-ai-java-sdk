/*
 * Copyright 2025 IBM Corporation
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
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.CloudRegion;
import com.ibm.watsonx.ai.WatsonxService.ScopedService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.project.ProjectService;
import com.ibm.watsonx.ai.textprocessing.ContainerReference;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.CosUrl;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.DocumentReference;
import com.ibm.watsonx.ai.textprocessing.ReadFileRequest;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.storage.StorageFactory;
import com.ibm.watsonx.ai.textprocessing.storage.StorageOperations;
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
 *     .baseUrl("https://...")    // or use CloudRegion
 *     .cosUrl("https://...")     // or use CosUrl
 *     .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
 *     .projectId("project-id")
 *     .documentReference(CosReference.of("<connection_id>", "<bucket-name>"))
 *     .resultReference(CosReference.of("<connection_id>", "<bucket-name>"))
 *     .build();
 *
 * TextExtractionResponse response = textExtractionService.startExtraction("myfile.pdf");
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class TextExtractionService extends ScopedService {
    private static final Logger logger = LoggerFactory.getLogger(TextExtractionService.class);
    private final String cosUrl;
    private final DocumentReference documentReference;
    private final DocumentReference resultReference;
    private final TextExtractionRestClient client;
    private volatile StorageOperations cosService;
    private final ReentrantLock cosServiceLock = new ReentrantLock();
    private final ProjectService lazyProjectService;
    private final Authenticator authenticator;
    private final Authenticator cosAuthenticator;

    private TextExtractionService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        boolean needsCos = builder.documentReference instanceof CosReference
            || builder.resultReference instanceof CosReference;
        var tmpUrl = needsCos
            ? requireNonNull(builder.cosUrl, "cosUrl value cannot be null")
            : requireNonNullElse(builder.cosUrl, "");
        cosUrl = tmpUrl.endsWith("/") ? tmpUrl.substring(0, tmpUrl.length() - 1) : tmpUrl;
        documentReference = requireNonNull(builder.documentReference, "documentReference value cannot be null");
        resultReference = requireNonNull(builder.resultReference, "resultReference value cannot be null");
        authenticator = builder.authenticator();
        cosAuthenticator = builder.cosAuthenticator;
        lazyProjectService = builder.projectService;
        client = TextExtractionRestClient.builder()
            .cosUrl(cosUrl)
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .requestLogger(requestLogger, requestLogLevel)
            .responseLogger(responseLogger, responseLogLevel)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .cosAuthenticator(builder.cosAuthenticator)
            .httpClient(httpClient)
            .verifySsl(verifySsl)
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
     * <b>Note:</b> This method does not return the extracted text content. Use {@link #extractAndFetch(String, TextExtractionParameters)} to run the
     * extraction and fetch the result immediately.
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

        try (var inputStream = new BufferedInputStream(new FileInputStream(file))) {
            upload(requestId, inputStream, file.getName(), parameters, false);
            return startExtraction(requestId, file.getName(), parameters, false);
        } catch (FileNotFoundException e) {
            throw new TextExtractionException("file_not_found", e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
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
     * @return A {@link TextExtractionResponse} representing the submitted request and its current status.
     * @see #uploadExtractAndFetch(InputStream, String, TextExtractionParameters)
     */
    public TextExtractionResponse uploadAndStartExtraction(InputStream is, String fileName, TextExtractionParameters parameters)
        throws TextExtractionException {
        requireNonNull(is, "is value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");
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
     * @see #extractAndFetch(String, TextExtractionParameters)
     */
    public String extractAndFetch(String absolutePath) throws TextExtractionException, FileNotFoundException {
        return extractAndFetch(absolutePath, null);
    }

    /**
     * Starts the text extraction process for a file that is already present in the configured {@link #documentReference document reference} and
     * returns the extracted text value. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the
     * {@code .md} extension by default.
     * <p>
     * <b>Note on {@code removeUploadedFile}:</b> if {@code parameters.removeUploadedFile()} is {@code true}, this method deletes the document at
     * {@code absolutePath} from storage after processing, even though no file was uploaded by this call. Use this option with caution when calling
     * this method on a pre-existing document.
     *
     * @param absolutePath The path of the document to extract text from.
     * @param parameters The configuration parameters for text extraction.
     * @return The text extracted.
     */
    public String extractAndFetch(String absolutePath, TextExtractionParameters parameters) throws TextExtractionException, FileNotFoundException {
        return extractAndFetch(UUID.randomUUID().toString(), absolutePath, parameters, absolutePath);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference}, starts text extraction process and returns the extracted
     * text value. The extracted text is saved as a new <b>Markdown</b> file, preserving the original filename but using the {@code .md} extension by
     * default. To customize the output behavior, use the overloaded method with {@link TextExtractionParameters}.
     *
     * @param file The local file to be uploaded and processed.
     * @return The text extracted.
     * @throws FileNotFoundException if the local file does not exist.
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
     * @throws FileNotFoundException if the local file does not exist.
     */
    public String uploadExtractAndFetch(File file, TextExtractionParameters parameters) throws TextExtractionException, FileNotFoundException {
        requireNonNull(file);
        if (file.isDirectory())
            throw new TextExtractionException("directory_not_allowed", "The file can not be a directory");

        validateFetchOutputs(parameters);

        var requestId = UUID.randomUUID().toString();

        try (var inputStream = new BufferedInputStream(new FileInputStream(file))) {
            upload(requestId, inputStream, file.getName(), parameters, true);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return extractAndFetch(requestId, file.getName(), parameters, file.getName());
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
        validateFetchOutputs(parameters);

        var requestId = UUID.randomUUID().toString();
        upload(requestId, is, fileName, parameters, true);

        try {
            return extractAndFetch(requestId, fileName, parameters, fileName);
        } catch (FileNotFoundException e) {
            throw new TextExtractionException("output_file_not_found",
                "The extracted output file could not be read from storage: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the results of a text extraction request by its unique identifier.
     * <p>
     * This operation fetches the details and results of a previously submitted text extraction request.
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
     * This operation fetches the details and results of a previously submitted text extraction request.
     *
     * @param id The unique identifier of the text extraction request.
     * @param parameters Parameters to specify the project or space context in which the request was made.
     * @return A {@link TextExtractionResponse} containing the results of the request.
     */
    public TextExtractionResponse fetchExtractionRequest(String id, TextExtractionFetchParameters parameters) {
        requireNonNull(parameters, "parameters cannot be null");
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
        requireNonNull(file);
        if (file.isDirectory())
            throw new TextExtractionException("directory_not_allowed", "The file can not be a directory");
        try (var inputStream = new BufferedInputStream(new FileInputStream(file))) {
            return uploadFile(inputStream, file.getName());
        } catch (FileNotFoundException e) {
            throw new TextExtractionException("file_not_found", e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        requireNonNull(inputStream, "inputStream value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");
        var requestId = UUID.randomUUID().toString();
        upload(requestId, inputStream, fileName, null, false);
        return true;
    }

    /**
     * Deletes a file from the configured result reference storage.
     * <p>
     * When the service is configured with a {@link CosReference} result reference, {@code bucketName} identifies the COS bucket and {@code fileName}
     * the object key. When configured with a {@link ContainerReference}, the bucket is resolved automatically from the project storage and
     * {@code bucketName} is ignored.
     *
     * @param bucketName The name of the COS bucket. Ignored when using a {@link ContainerReference}.
     * @param fileName The name of the file to delete.
     * @return {@code true} if the file was successfully deleted, {@code false} otherwise.
     */
    public boolean deleteFile(String bucketName, String fileName) throws FileNotFoundException {
        var requestId = UUID.randomUUID().toString();
        if (resultReference instanceof ContainerReference)
            return getOrResolveCosService().deleteFile(requestId, fileName);
        return client.deleteFile(DeleteFileRequest.of(requestId, bucketName, fileName));
    }

    /**
     * Reads the content of a file from the configured result reference storage.
     * <p>
     * When the service is configured with a {@link CosReference} result reference, {@code bucketName} identifies the COS bucket and {@code fileName}
     * the object key. When configured with a {@link ContainerReference}, the bucket is resolved automatically from the project storage and
     * {@code bucketName} is ignored.
     *
     * @param bucketName The name of the COS bucket. Ignored when using a {@link ContainerReference}.
     * @param fileName The path of the file to read.
     * @return The file content as a string.
     * @throws FileNotFoundException if the file does not exist.
     */
    public String readFile(String bucketName, String fileName) throws FileNotFoundException {
        var requestId = UUID.randomUUID().toString();
        if (resultReference instanceof ContainerReference)
            return getOrResolveCosService().readFile(requestId, fileName);
        return client.readFile(ReadFileRequest.of(requestId, bucketName, fileName));
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
        requireNonNull(parameters, "parameters cannot be null");

        var builder = TextExtractionDeleteParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .hardDelete(parameters.hardDelete().orElse(null))
            .build();

        var requestTrackingId = UUID.randomUUID().toString();
        var request = DeleteExtractionRequest.of(requestTrackingId, id, p);
        return client.deleteExtraction(request);
    }

    // Returns the CosStorageService lazily on first use.
    private StorageOperations getOrResolveCosService() {
        return getOrResolveCosService(null);
    }

    private StorageOperations getOrResolveCosService(CosReference cosRef) {

        if (cosRef != null) {
            // Per-call CosReference override.
            return StorageFactory.cos(cosUrl, cosRef.bucket(), authenticator, cosAuthenticator, httpClient,
                timeout, logRequests, logResponses, requestLogger, requestLogLevel, responseLogger, responseLogLevel, verifySsl);
        }

        if (cosService != null)
            return cosService;

        cosServiceLock.lock();

        try {

            if (cosService != null)
                return cosService;

            if (projectId == null)
                throw new IllegalStateException(
                    "ContainerReference storage requires a projectId - spaceId alone is not supported for container uploads.");

            ProjectService ps = lazyProjectService;

            if (ps == null) {
                var region = CloudRegion.fromMlEndpoint(baseUrl).orElseThrow(() -> new IllegalStateException(
                    "ContainerReference storage requires a ProjectService or a known CloudRegion. "
                        + "Either pass projectService(ProjectService) on the builder, "
                        + "or use baseUrl(CloudRegion) so the project storage can be resolved automatically."));
                var psBuilder = ProjectService.builder()
                    .baseUrl(region.wxEndpoint().replace("/wx", ""))
                    .authenticator(authenticator)
                    .httpClient(httpClient)
                    .timeout(timeout)
                    .logRequests(logRequests)
                    .logResponses(logResponses)
                    .verifySsl(verifySsl);

                if (requestLogger != null)
                    psBuilder.logRequests(requestLogger, requestLogLevel);

                if (responseLogger != null)
                    psBuilder.logResponses(responseLogger, responseLogLevel);

                ps = psBuilder.build();
            }

            var props = ps.findProject(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found: " + projectId))
                .storage().properties();

            var resolvedUrl = props.endpointUrl().endsWith("/")
                ? props.endpointUrl().substring(0, props.endpointUrl().length() - 1)
                : props.endpointUrl();

            var resolvedBucket = props.bucketName();

            cosService = StorageFactory.cos(resolvedUrl, resolvedBucket, authenticator, cosAuthenticator, httpClient,
                timeout, logRequests, logResponses, requestLogger, requestLogLevel, responseLogger, responseLogLevel, verifySsl);

            return cosService;
        } finally {
            cosServiceLock.unlock();
        }
    }

    // Retrieves the results of a text extraction request by its unique identifier.
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

    // Starts the text extraction and waits until the result is ready.
    private String extractAndFetch(String requestId, String absolutePath, TextExtractionParameters parameters, String uploadedPath)
        throws TextExtractionException, FileNotFoundException {
        requireNonNull(requestId, "requestId cannot be null");
        requireNonNull(absolutePath, "absolutePath cannot be null");

        validateFetchOutputs(parameters);

        try {
            var textExtractionResponse = startExtraction(requestId, absolutePath, parameters, true);
            return getExtractedText(requestId, textExtractionResponse, parameters);
        } finally {
            if (nonNull(parameters) && parameters.isRemoveUploadedFile()) {
                DocumentReference effectiveDoc = parameters.documentReference() != null
                    ? parameters.documentReference()
                    : this.documentReference;
                cleanUpUploadedFile(requestId, uploadedPath, effectiveDoc);
            }
        }
    }

    // Validates that the requested outputs are compatible with a single-file fetch operation.
    private void validateFetchOutputs(TextExtractionParameters parameters) throws TextExtractionException {

        if (parameters == null)
            return;
        var outputs = parameters.requestedOutputs();

        if (outputs == null || outputs.isEmpty())
            return;

        if (outputs.size() > 1)
            throw new TextExtractionException("fetch_operation_not_allowed",
                "The fetch operation cannot be executed if more than one file is to be generated");
        if (outputs.get(0).equals(PAGE_IMAGES.value()))
            throw new TextExtractionException("fetch_operation_not_allowed",
                "The fetch operation cannot be executed for the type \"page_images\"");
    }

    // Uploads an input stream to COS or the container.
    private void upload(String requestId, InputStream is, String fileName, TextExtractionParameters parameters,
        boolean waitForExtraction) {
        requireNonNull(requestId, "requestId value cannot be null");
        requireNonNull(is, "is value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");

        boolean removeOutputFile = nonNull(parameters) && parameters.isRemoveOutputFile();
        boolean removeUploadedFile = nonNull(parameters) && parameters.isRemoveUploadedFile();
        if (!waitForExtraction && (removeOutputFile || removeUploadedFile))
            throw new IllegalArgumentException(
                "The asynchronous version of startExtraction doesn't allow the use of the \"removeOutputFile\" and \"removeUploadedFile\" parameters");

        DocumentReference effectiveDoc = requireNonNullElse(
            parameters != null ? parameters.documentReference() : null,
            this.documentReference
        );

        if (effectiveDoc instanceof ContainerReference) {
            getOrResolveCosService().upload(requestId, is, fileName);
            return;
        }

        if (!(effectiveDoc instanceof CosReference cosRef))
            throw new UnsupportedOperationException(
                "Unsupported documentReference type: " + effectiveDoc.getClass().getSimpleName());
        if (cosUrl.isBlank())
            throw new IllegalStateException(
                "cosUrl must be set on the service builder to perform COS upload operations.");

        getOrResolveCosService(cosRef).upload(requestId, is, fileName);
    }

    // Starts the text extraction process.
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
        DocumentReference documentReference = this.documentReference;
        DocumentReference resultReference = this.resultReference;
        Parameters params = null;
        Map<String, Object> custom = null;
        Duration timeout = this.timeout;
        String transactionId = null;

        if (nonNull(parameters)) {
            removeOutputFile = parameters.isRemoveOutputFile();
            removeUploadedFile = parameters.isRemoveUploadedFile();
            outputFileName = parameters.outputFileName();
            projectId = parameters.projectId();
            spaceId = parameters.spaceId();
            var paramOutputs = parameters.requestedOutputs();
            if (paramOutputs != null && !paramOutputs.isEmpty())
                requestedOutputs = paramOutputs;
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
            resultReference = requireNonNullElse(parameters.resultReference(), this.resultReference);
            params = parameters.toParameters();
            custom = parameters.custom();
            timeout = requireNonNullElse(parameters.timeout(), timeout);
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

        if ((documentReference instanceof CosReference || resultReference instanceof CosReference) && cosUrl.isBlank())
            throw new IllegalStateException(
                "cosUrl must be set on the service builder when using a CosReference document or result reference.");

        boolean isMultiOutput = requestedOutputs.size() > 1 || requestedOutputs.get(0).equals(PAGE_IMAGES.value());

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
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        String processId = response.metadata().id();

        do {
            if (System.nanoTime() - deadlineNanos >= 0) {
                cleanUpAfterAbortedExtraction(processId, projectId, spaceId, transactionId);
                throw new TextExtractionException("timeout",
                    "Execution to extract %s file took longer than the timeout set by %s milliseconds"
                        .formatted(path, timeout.toMillis()));
            }

            try {
                long remaining = deadlineNanos - System.nanoTime();
                Thread.sleep(Math.min(sleepTime, Math.max(remaining / 1_000_000L, 0L)));
                sleepTime = Math.min(sleepTime * 2, 3000);
            } catch (InterruptedException e) {
                try {
                    cleanUpAfterAbortedExtraction(processId, projectId, spaceId, transactionId);
                } finally {
                    Thread.currentThread().interrupt();
                }
                throw new TextExtractionException("interrupted", e.getMessage(), e);
            }

            try {
                response = fetchExtractionRequest(requestId, processId, TextExtractionFetchParameters.builder()
                    .projectId(projectId)
                    .spaceId(spaceId)
                    .build());
                status = Status.fromValue(response.entity().results().status());
                var pagesProcessed = response.entity().results().numberPagesProcessed();
                logger.debug("Extraction status: {} for the file {} (pages processed {})", status, path, pagesProcessed);
            } catch (Exception e) {
                cleanUpAfterAbortedExtraction(processId, projectId, spaceId, transactionId);
                throw e;
            }

        } while (status != Status.FAILED && status != Status.COMPLETED);

        return response;
    }

    // Cancels the extraction job on timeout, interrupt, or fetch error. Logs but never throws.
    private void cleanUpAfterAbortedExtraction(String processId, String projectId, String spaceId,
        String transactionId) {

        if (nonNull(processId)) {
            try {
                deleteRequest(
                    processId,
                    TextExtractionDeleteParameters.builder()
                        .projectId(projectId)
                        .spaceId(spaceId)
                        .transactionId(transactionId)
                        .build());
            } catch (Exception e) {
                logger.warn("Failed to cancel extraction job {}: {}", processId, e.getMessage(), e);
            }
        }
    }

    // Deletes the uploaded file from COS or the container. All errors are logged and swallowed.
    private void cleanUpUploadedFile(String requestId, String path, DocumentReference effectiveDoc) {
        try {
            if (effectiveDoc instanceof CosReference cosRef)
                client.deleteFileAsync(DeleteFileRequest.of(requestId, cosRef.bucket(), path))
                    .exceptionally(ex -> {
                        logger.warn("Async COS delete failed for {}: {}", path, ex.getMessage());
                        return false;
                    });
            else if (effectiveDoc instanceof ContainerReference) {
                boolean wasInterrupted = Thread.interrupted();
                try {
                    getOrResolveCosService().deleteFileAsync(requestId, path);
                } finally {
                    if (wasInterrupted)
                        Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to delete uploaded file {}: {}", path, e.getMessage(), e);
        }
    }

    // Retrieves the extracted text from COS or the container.
    private String getExtractedText(String requestId, TextExtractionResponse textExtractionResponse, TextExtractionParameters parameters)
        throws TextExtractionException, FileNotFoundException {

        requireNonNull(requestId);

        Status status = Status.fromValue(textExtractionResponse.entity().results().status());
        boolean removeOutputFile = nonNull(parameters) && parameters.isRemoveOutputFile();

        DocumentReference effectiveResult = parameters != null && parameters.resultReference() != null
            ? parameters.resultReference()
            : this.resultReference;

        boolean isContainerResult = effectiveResult instanceof ContainerReference;
        String resultsBucketName = effectiveResult instanceof CosReference cosRes ? cosRes.bucket() : null;

        return switch(status) {
            case COMPLETED -> {
                var resultsLocation = textExtractionResponse.entity().resultsReference().location();
                String outputPath = resultsLocation.path() != null ? resultsLocation.path() : resultsLocation.fileName();
                String extractedFile;
                if (isContainerResult)
                    extractedFile = getOrResolveCosService().readFile(requestId, outputPath);
                else
                    extractedFile = client.readFile(ReadFileRequest.of(requestId, resultsBucketName, outputPath));
                if (removeOutputFile) {
                    try {
                        if (isContainerResult) {
                            getOrResolveCosService().deleteFileAsync(requestId, outputPath);
                        } else {
                            client.deleteFileAsync(DeleteFileRequest.of(requestId, resultsBucketName, outputPath))
                                .exceptionally(ex -> {
                                    logger.warn("Async COS delete failed for {}: {}", outputPath, ex.getMessage());
                                    return false;
                                });
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to delete output file {}: {}", outputPath, e.getMessage(), e);
                    }
                }
                yield extractedFile;
            }
            case FAILED -> {
                var error = textExtractionResponse.entity().results().error();
                if (isNull(error))
                    throw new TextExtractionException("generic_error", "The extraction failed without error details");
                throw new TextExtractionException(error.code(), error.message());
            }
            default -> throw new TextExtractionException("generic_error",
                "Status %s not managed".formatted(status));
        };
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextExtractionService textExtractionService = TextExtractionService.builder()
     *     .baseUrl("https://...")    // or use CloudRegion
     *     .cosUrl("https://...")     // or use CosUrl
     *     .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
     *     .projectId("project-id")
     *     .documentReference(CosReference.of("<connection_id>", "<bucket-name>"))
     *     .resultReference(CosReference.of("<connection_id>", "<bucket-name>"))
     *     .build();
     *
     * TextExtractionResponse response = textExtractionService.startExtraction("myfile.pdf");
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
    public final static class Builder extends ScopedService.Builder<Builder> {
        private String cosUrl;
        private Authenticator cosAuthenticator;
        private DocumentReference documentReference;
        private DocumentReference resultReference;
        private ProjectService projectService;

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
         * Specifies a custom authenticator for Cloud Object Storage (COS) operations.
         * <p>
         * This allows using a different API key or authentication method for COS when it is deployed in a different environment or region than the
         * main service. If not specified, the main service authenticator will be used.
         *
         * @param cosAuthenticator The {@link Authenticator} to use for COS operations.
         */
        public Builder cosAuthenticator(Authenticator cosAuthenticator) {
            this.cosAuthenticator = cosAuthenticator;
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
         * Specifies the container path of the input document.
         * <p>
         * When using a container reference, {@code cosUrl} is not required on the builder.
         *
         * @param documentReference the {@link ContainerReference} for the input file.
         */
        public Builder documentReference(ContainerReference documentReference) {
            this.documentReference = documentReference;
            return this;
        }

        /**
         * Specifies the Cloud Object Storage connection and bucket where the input files are stored.
         *
         * @param connectionId The id of the COS connection asset.
         * @param bucket The name of the bucket containing the input documents.
         * @deprecated Use {@link #documentReference(CosReference)} with {@link CosReference#of(String, String)} instead.
         */
        @Deprecated
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
         * Specifies the container path where the extracted results should be stored.
         * <p>
         * When using a container reference, {@code cosUrl} is not required on the builder.
         *
         * @param resultReference the {@link ContainerReference} for the output location.
         */
        public Builder resultReference(ContainerReference resultReference) {
            this.resultReference = resultReference;
            return this;
        }

        /**
         * Specifies the Cloud Object Storage connection and bucket where the extracted results should be stored.
         *
         * @param connectionId The id of the COS connection asset.
         * @param bucket The name of the bucket where results will be written.
         * @deprecated Use {@link #resultReference(CosReference)} with {@link CosReference#of(String, String)} instead.
         */
        @Deprecated
        public Builder resultReference(String connectionId, String bucket) {
            return resultReference(CosReference.of(connectionId, bucket));
        }

        /**
         * Specifies a {@link ProjectService} to use for resolving the COS bucket and endpoint URL when a {@link ContainerReference} is configured.
         * <p>
         * When not set, the service attempts to resolve the project storage automatically from the {@code baseUrl} if it matches a known
         * {@link com.ibm.watsonx.ai.CloudRegion}. For custom or on-premise deployments where the base URL is not a standard cloud region, provide an
         * explicit {@link ProjectService} instance here.
         *
         * @param projectService the {@link ProjectService} to use for project storage resolution
         */
        public Builder projectService(ProjectService projectService) {
            this.projectService = projectService;
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
