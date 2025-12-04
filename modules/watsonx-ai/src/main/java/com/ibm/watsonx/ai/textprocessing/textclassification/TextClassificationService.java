/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textclassification;

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
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.CosUrl;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.Error;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationResponse.ClassificationResult;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationRestClient.DeleteClassificationRequest;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationRestClient.FetchClassificationDetailsRequest;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationRestClient.StartClassificationRequest;

/**
 * Service class to interact with IBM watsonx.ai Text Classification APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextClassificationService textClassificationService = TextClassificationService.builder()
 *   .baseUrl("https://...")    // or use CloudRegion
 *   .cosUrl("https://...")     // or use CosUrl
 *   .apiKey("my-api-key")      // creates an IAM-based AuthenticationProvider
 *   .projectId("my-project-id")
 *   .documentReference("<connection_id>", "<bucket-name>")
 *   .build();
 *
 * TextClassificationResponse response = textClassificationService.startClassification("myfile.pdf")
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticationProvider(AuthenticationProvider)}.
 *
 * @see AuthenticationProvider
 */
public class TextClassificationService extends ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(TextClassificationService.class);
    private final String cosUrl;
    private final CosReference documentReference;
    private final TextClassificationRestClient client;

    private TextClassificationService(Builder builder) {
        super(builder);
        requireNonNull(builder.getAuthenticationProvider(), "authenticationProvider cannot be null");
        var tmpUrl = requireNonNull(builder.cosUrl, "cosUrl value cannot be null");
        cosUrl = tmpUrl.endsWith("/") ? tmpUrl.substring(0, tmpUrl.length() - 1) : tmpUrl;
        documentReference = requireNonNull(builder.documentReference, "documentReference value cannot be null");
        client = TextClassificationRestClient.builder()
            .cosUrl(cosUrl)
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticationProvider(builder.getAuthenticationProvider())
            .build();
    }

    /**
     * Starts the text classification process for a document that already exists in the configured {@link #documentReference document reference}.
     * <p>
     * The {@code absolutePath} parameter identifies the location of the file <b>inside the document reference</b> (not a local filesystem path). To
     * customize the output behavior, use the overloaded method with {@link TextClassificationParameters}.
     * <p>
     * If you want to process a <b>local file</b>, use {@link #uploadAndStartClassification(File)} instead.
     * <p>
     * <b>Note:</b> This method does not return the classification result. Use {@link #classifyAndFetch(String)} to run the classification and fetch
     * the result immediately.
     *
     * @param absolutePath The location of the document to be processed.
     * @return A {@link TextClassificationResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartClassification(File)
     * @see #classifyAndFetch(String)
     */
    public TextClassificationResponse startClassification(String absolutePath) throws TextClassificationException {
        return startClassification(absolutePath, null);
    }

    /**
     * Starts the text classification process for a document that already exists in the configured {@link #documentReference document reference}.
     * <p>
     * The {@code absolutePath} parameter identifies the location of the file <b>inside the document reference</b> (not a local filesystem path).
     * <p>
     * If you want to process a <b>local file</b>, use {@link #uploadAndStartClassification(File, TextClassificationParameters)} instead.
     * <p>
     * <b>Note:</b> This method does not return the classification result. Use {@link #classifyAndFetch(String, String, TextClassificationParameters)}
     * to run the classification and fetch the result immediately.
     *
     * @param absolutePath The location of the document to be processed.
     * @param parameters The configuration parameters for text classification.
     * @return A {@link TextClassificationResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartClassification(File, TextClassificationParameters)
     * @see #classifyAndFetch(String, TextClassificationParameters)
     */
    public TextClassificationResponse startClassification(String absolutePath, TextClassificationParameters parameters)
        throws TextClassificationException {
        return startClassification(UUID.randomUUID().toString(), absolutePath, parameters, false);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference} and starts the text classification process. To customize
     * the output behavior, use the overloaded method with {@link TextClassificationParameters}.
     * <p>
     * <b>Note:</b> This method does not return the classification result. Use {@link #uploadClassifyAndFetch(File)} to get the classification
     * immediately.
     *
     * @param file The local file to be uploaded and processed.
     * @return A {@link TextClassificationResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartClassification(File, TextClassificationParameters)
     * @see #uploadClassifyAndFetch(File)
     */
    public TextClassificationResponse uploadAndStartClassification(File file) throws TextClassificationException {
        return uploadAndStartClassification(file, null);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference} and starts the text classification process.
     * <p>
     * <b>Note:</b> This method does not return the classification result. Use {@link #uploadClassifyAndFetch(File, TextClassifyParameters)} to get
     * the classification immediately.
     *
     * @param file The local file to be uploaded and processed.
     * @param parameters The configuration parameters for text classification.
     * @return A {@link TextClassificationResponse} representing the submitted request and its current status.
     * @see #uploadClassifyAndFetch(File, TextClassificationParameters)
     */
    public TextClassificationResponse uploadAndStartClassification(File file, TextClassificationParameters parameters)
        throws TextClassificationException {
        requireNonNull(file);

        if (file.isDirectory())
            throw new TextClassificationException("directory_not_allowed", "The file can not be a directory");

        var requestId = UUID.randomUUID().toString();

        try {
            upload(requestId, new BufferedInputStream(new FileInputStream(file)), file.getName(), parameters, false);
            return startClassification(requestId, file.getName(), parameters, false);
        } catch (FileNotFoundException e) {
            throw new TextClassificationException("file_not_found", e.getMessage(), e);
        }
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference} and starts the asynchronous text classification
     * process. To customize the output behavior, use the overloaded method with {@link TextClassificationParameters}.
     * <p>
     * <b>Note:</b> This method does not return the classification result. Use {@link #uploadClassifyAndFetch(InputStream, String)} to get the
     * classification immediately.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @return The unique identifier of the text classification process.
     * @see #uploadAndStartClassification(InputStream, String, TextClassificationParameters)
     * @see #uploadClassifyAndFetch(InputStream, String)
     */
    public TextClassificationResponse uploadAndStartClassification(InputStream is, String fileName) throws TextClassificationException {
        return uploadAndStartClassification(is, fileName, null);
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference} and starts the asynchronous text classification
     * process.
     * <p>
     * <b>Note:</b> This method does not return the classification result. Use
     * {@link #uploadClassifyAndFetch(InputStream, String, TextClassificationParameters)} to get the classification immediately.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @param parameters The configuration parameters for text classification.
     * @return The unique identifier of the text classification process.
     * @see #uploadClassifyAndFetch(InputStream, String, TextClassificationParameters)
     */
    public TextClassificationResponse uploadAndStartClassification(InputStream is, String fileName, TextClassificationParameters parameters)
        throws TextClassificationException {
        var requestId = UUID.randomUUID().toString();
        upload(requestId, is, fileName, parameters, false);
        return startClassification(requestId, fileName, parameters, false);
    }

    /**
     * Starts the text classification process for a file that is already present in the configured {@link #documentReference document reference} and
     * returns the classification result. To customize the output behavior, use the overloaded method with {@link TextClassificationParameters}.
     *
     * @param absolutePath The absolute path of the file.
     * @return The classification result.
     * @see #classifyAndFetch(String, String, TextClassificationParameters)
     */
    public ClassificationResult classifyAndFetch(String absolutePath) throws TextClassificationException {
        return classifyAndFetch(absolutePath, null);
    }

    /**
     * Starts the text classification process for a file that is already present in the configured {@link #documentReference document reference} and
     * returns the classification result.
     *
     * @param absolutePath The path of the document to be classified.
     * @param parameters The configuration parameters for text classification.
     * @return The classification result.
     */
    public ClassificationResult classifyAndFetch(String absolutePath, TextClassificationParameters parameters) throws TextClassificationException {
        return classifyAndFetch(UUID.randomUUID().toString(), absolutePath, parameters);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference}, starts text classification process and returns the
     * classification result. To customize the output behavior, use the overloaded method with {@link TextClassificationParameters}.
     *
     * @param file The local file to be uploaded and processed.
     * @return The classification result.
     * @see #uploadClassifyAndFetch(File, TextClassificationParameters)
     */
    public ClassificationResult uploadClassifyAndFetch(File file) throws TextClassificationException {
        return uploadClassifyAndFetch(file, null);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference}, starts text classification process and returns the
     * classification result.
     *
     * @param file The local file to be uploaded and processed.
     * @param parameters The configuration parameters for text classification.
     * @return The classification result.
     */
    public ClassificationResult uploadClassifyAndFetch(File file, TextClassificationParameters parameters) throws TextClassificationException {

        var requestId = UUID.randomUUID().toString();

        try {
            upload(requestId, new BufferedInputStream(new FileInputStream(file)), file.getName(), parameters, true);
        } catch (FileNotFoundException e) {
            throw new TextClassificationException("file_not_found", e.getMessage(), e);
        }
        return classifyAndFetch(requestId, file.getName(), parameters);
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference}, starts text classification process and returns
     * the classification result. To customize the output behavior, use the overloaded method with {@link TextClassificationParameters}.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @return The classification result.
     * @see #uploadClassifyAndFetch(InputStream, String, TextClassificationParameters)
     */
    public ClassificationResult uploadClassifyAndFetch(InputStream is, String fileName) throws TextClassificationException {
        return uploadClassifyAndFetch(is, fileName, null);
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference}, starts text classification process and returns
     * the classification result.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @param parameters The configuration parameters for text classification.
     * @return The classification result.
     */
    public ClassificationResult uploadClassifyAndFetch(InputStream is, String fileName, TextClassificationParameters parameters)
        throws TextClassificationException {
        var requestId = UUID.randomUUID().toString();
        upload(requestId, is, fileName, parameters, true);
        return classifyAndFetch(requestId, fileName, parameters);
    }

    /**
     * Retrieves the results of a text classification request by its unique identifier.
     * <p>
     * This operation fetches the details and results of a previously submitted text classification request.
     *
     * @param id The unique identifier of the text classification request.
     * @return A {@link TextClassificationResponse} containing the results of the request.
     */
    public TextClassificationResponse fetchClassificationRequest(String id) {
        return fetchClassificationRequest(id, TextClassificationFetchParameters.builder().build());
    }

    /**
     * Retrieves the results of a text classification request by its unique identifier.
     * <p>
     * This operation fetches the details and results of a previously submitted text classification request.
     *
     * @param id The unique identifier of the text classification request.
     * @param parameters Parameters to specify the project or space context in which the request was made.
     * @return A {@link TextClassificationResponse} containing the results of the request.
     */
    public TextClassificationResponse fetchClassificationRequest(String id, TextClassificationFetchParameters parameters) {
        return fetchClassificationRequest(UUID.randomUUID().toString(), id, parameters);
    }

    /**
     * Uploads a file in the configured {@link #documentReference document reference}.
     *
     * @param file the file to be uploaded
     * @return {@code true} if the upload request was successfully sent
     * @throws TextClassificationException if the file cannot be found or an error occurs during upload
     */
    public boolean uploadFile(File file) throws TextClassificationException {
        try {
            return uploadFile(new BufferedInputStream(new FileInputStream(file)), file.getName());
        } catch (FileNotFoundException e) {
            throw new TextClassificationException("file_not_found", e.getMessage(), e);
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
    public boolean deleteFile(String bucketName, String fileName) {
        return client.deleteFile(DeleteFileRequest.of(bucketName, fileName));
    }

    /**
     * Deletes a text classification request.
     *
     * @param id The unique identifier of the text classification request to delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id) {
        return deleteRequest(id, TextClassificationDeleteParameters.builder().build());
    }

    /**
     * Deletes a text classification request.
     * <p>
     * This operation cancels the specified text classification request. If the {@code hardDelete} parameter is set to {@code true}, it will also
     * delete the associated job metadata.
     *
     * @param id The unique identifier of the text classification request to delete.
     * @param parameters Parameters specifying the space or project context, and whether to perform a hard delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id, TextClassificationDeleteParameters parameters) {
        requireNonNull(id, "The id can not be null");

        var builder = TextClassificationDeleteParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .hardDelete(parameters.hardDelete().orElse(null))
            .build();

        var request = DeleteClassificationRequest.of(parameters.transactionId(), id, p);
        return client.deleteClassification(request);
    }

    //
    // Retrieves the results of a text classification request by its unique identifier.
    //
    private TextClassificationResponse fetchClassificationRequest(String requestId, String id, TextClassificationFetchParameters parameters) {
        requireNonNull(requestId, "The requestId can not be null");
        requireNonNull(id, "The id can not be null");

        var builder = TextClassificationFetchParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .build();

        var request = FetchClassificationDetailsRequest.of(requestId, id, p);
        return client.fetchClassificationDetails(request);
    }

    //
    // Start the text classification and wait until the result is ready.
    //
    private ClassificationResult classifyAndFetch(String requestId, String absolutePath, TextClassificationParameters parameters)
        throws TextClassificationException {
        requireNonNull(requestId, "requestId cannot be null");

        var textClassificationResponse = startClassification(requestId, absolutePath, parameters, true);
        return getClassificationResult(requestId, textClassificationResponse, parameters);
    }

    //
    // Uploads an inputstream to the Cloud Object Storage.
    //
    private void upload(String requestId, InputStream is, String fileName, TextClassificationParameters parameters, boolean waitForClassification) {
        requireNonNull(requestId, "requestId value cannot be null");
        requireNonNull(is, "is value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");

        boolean removeOutputFile = false;
        CosReference documentReference = this.documentReference;

        if (nonNull(parameters))
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);

        if (!waitForClassification && removeOutputFile)
            throw new IllegalArgumentException(
                "The asynchronous version of startClassification doesn't allow the use of the \"removeUploadedFile\" parameter");

        var request = UploadRequest.of(requestId, documentReference.bucket(), is, fileName);
        client.upload(request);
    }

    //
    // Starts the text classification process.
    //
    private TextClassificationResponse startClassification(String requestId, String path, TextClassificationParameters parameters,
        boolean waitUntilJobIsDone)
        throws TextClassificationException {
        requireNonNull(path);
        requireNonNull(requestId);

        String projectId = null;
        String spaceId = null;
        boolean removeUploadedFile = false;
        CosReference documentReference = this.documentReference;
        Parameters params = null;
        Map<String, Object> custom = null;
        Duration timeout = Duration.ofSeconds(60);
        String transactionId = null;

        if (nonNull(parameters)) {
            removeUploadedFile = parameters.isRemoveUploadedFile();
            projectId = parameters.projectId();
            spaceId = parameters.spaceId();
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
            params = parameters.toParameters();
            custom = parameters.custom();
            timeout = requireNonNullElse(parameters.timeout(), Duration.ofSeconds(60));
            transactionId = parameters.transactionId();
        }

        if (isNull(projectId) && isNull(spaceId)) {
            projectId = this.projectId;
            spaceId = this.spaceId;
        }

        if (!waitUntilJobIsDone && removeUploadedFile)
            throw new IllegalArgumentException(
                "The asynchronous version of startClassification doesn't allow the use of the \"removeUploadedFile\" parameter");

        var textClassificationRequest = new TextClassificationRequest(
            projectId,
            spaceId,
            documentReference.toDataReference(path),
            params,
            custom
        );

        var request = StartClassificationRequest.of(requestId, transactionId, textClassificationRequest);
        var response = client.startClassification(request);

        if (!waitUntilJobIsDone)
            return response;

        Status status;
        long sleepTime = 100;
        LocalTime endTime = LocalTime.now().plus(timeout);

        do {

            if (LocalTime.now().isAfter(endTime))
                throw new TextClassificationException("timeout",
                    "The execution of the classification %s file took longer than the timeout set by %s milliseconds"
                        .formatted(path, timeout.toMillis()));

            try {

                Thread.sleep(sleepTime);
                sleepTime *= 2;
                sleepTime = Math.min(sleepTime, 3000);

            } catch (Exception e) {
                throw new TextClassificationException("interrupted", e.getMessage());
            }

            var processId = response.metadata().id();
            response = fetchClassificationRequest(requestId, processId, TextClassificationFetchParameters.builder()
                .projectId(projectId)
                .spaceId(spaceId)
                .build());

            status = Status.fromValue(response.entity().results().status());
            logger.debug("Classification status: {} for the file {}", status, path);

        } while (status != Status.FAILED && status != Status.COMPLETED);

        return response;
    }

    //
    // Retrieves the classification result.
    //
    private ClassificationResult getClassificationResult(String requestId, TextClassificationResponse textClassificationResponse,
        TextClassificationParameters parameters)
        throws TextClassificationException {

        requireNonNull(requestId);

        String uploadedPath = textClassificationResponse.entity().documentReference().location().fileName();
        Status status = Status.fromValue(textClassificationResponse.entity().results().status());
        boolean removeUploadedFile = false;
        CosReference documentReference = this.documentReference;

        if (nonNull(parameters)) {
            removeUploadedFile = parameters.isRemoveUploadedFile();
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
        }

        String documentBucketName = documentReference.bucket();

        try {

            return switch(status) {
                case COMPLETED -> {
                    yield textClassificationResponse.entity().results();
                }
                default -> {
                    Error error = textClassificationResponse.entity().results().error();
                    throw new TextClassificationException(error.code(), error.message());
                }
            };

        } finally {
            if (removeUploadedFile) {
                try {
                    var encodedFileName = new URI(null, null, uploadedPath, null).toASCIIString();
                    var request = DeleteFileRequest.of(requestId, documentBucketName, encodedFileName);
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
     * TextClassificationService textClassificationService = TextClassificationService.builder()
     *   .baseUrl("https://...")    // or use CloudRegion
     *   .cosUrl("https://...")     // or use CosUrl
     *   .apiKey("my-api-key")      // creates an IAM-based AuthenticationProvider
     *   .projectId("my-project-id")
     *   .documentReference("<connection_id>", "<bucket-name>")
     *   .build();
     *
     * TextClassificationResponse response = textClassificationService.startClassification("myfile.pdf")
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextClassificationService} instances with configurable parameters.
     */
    public final static class Builder extends ProjectService.Builder<Builder> {
        private String cosUrl;
        private CosReference documentReference;

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
         * Builds a {@link TextClassificationService} instance using the configured parameters.
         *
         * @return a new instance of {@link TextClassificationService}
         */
        public TextClassificationService build() {
            return new TextClassificationService(this);
        }
    }
}
