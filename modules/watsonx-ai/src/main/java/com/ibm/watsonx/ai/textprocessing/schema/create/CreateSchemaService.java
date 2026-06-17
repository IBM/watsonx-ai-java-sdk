/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.schema.create;

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
import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.CosUrl;
import com.ibm.watsonx.ai.textprocessing.DeleteFileRequest;
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.UploadRequest;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaResponse.CreateSchemaResult;

/**
 * Service class to interact with IBM watsonx.ai Create Schema APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * CreateSchemaService createSchemaService = CreateSchemaService.builder()
 *   .baseUrl("https://...")    // or use CloudRegion
 *   .cosUrl("https://...")     // or use CosUrl
 *   .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
 *   .projectId("project-id")
 *   .documentReference("<connection_id>", "<bucket-name>")
 *   .build();
 *
 * CreateSchemaResponse response = createSchemaService.startCreateSchema("myfile.pdf")
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class CreateSchemaService extends ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(CreateSchemaService.class);
    private final String cosUrl;
    private final CosReference documentReference;
    private final CreateSchemaRestClient client;

    private CreateSchemaService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        var tmpUrl = requireNonNull(builder.cosUrl, "cosUrl value cannot be null");
        cosUrl = tmpUrl.endsWith("/") ? tmpUrl.substring(0, tmpUrl.length() - 1) : tmpUrl;
        documentReference = requireNonNull(builder.documentReference, "documentReference value cannot be null");
        client = CreateSchemaRestClient.builder()
            .cosUrl(cosUrl)
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .cosAuthenticator(builder.cosAuthenticator)
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
    }

    /**
     * Starts the create schema process for a document that already exists in the configured {@link #documentReference document reference}.
     * <p>
     * The {@code absolutePath} parameter identifies the location of the file <b>inside the document reference</b> (not a local filesystem path).
     * <p>
     * If you want to process a <b>local file</b>, use {@link #uploadAndStartCreateSchema(File)} instead.
     *
     * @param absolutePath The location of the document to be processed.
     * @return A {@link CreateSchemaResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartCreateSchema(File)
     */
    public CreateSchemaResponse startCreateSchema(String absolutePath) throws CreateSchemaException {
        return startCreateSchema(absolutePath, null);
    }

    /**
     * Starts the create schema process for a document that already exists in the configured {@link #documentReference document reference}.
     * <p>
     * The {@code absolutePath} parameter identifies the location of the file <b>inside the document reference</b> (not a local filesystem path).
     * <p>
     * If you want to process a <b>local file</b>, use {@link #uploadAndStartCreateSchema(File, CreateSchemaParameters)} instead.
     *
     * @param absolutePath The location of the document to be processed.
     * @param parameters The configuration parameters for create schema.
     * @return A {@link CreateSchemaResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartCreateSchema(File, CreateSchemaParameters)
     */
    public CreateSchemaResponse startCreateSchema(String absolutePath, CreateSchemaParameters parameters) throws CreateSchemaException {
        return startCreateSchema(UUID.randomUUID().toString(), absolutePath, parameters, false);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference} and starts the create schema process.
     *
     * @param file The local file to be uploaded and processed.
     * @return A {@link CreateSchemaResponse} representing the submitted request and its current status.
     *
     * @see #uploadAndStartCreateSchema(File, CreateSchemaParameters)
     */
    public CreateSchemaResponse uploadAndStartCreateSchema(File file) throws CreateSchemaException {
        return uploadAndStartCreateSchema(file, null);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference} and starts the create schema process.
     *
     * @param file The local file to be uploaded and processed.
     * @param parameters The configuration parameters for create schema.
     * @return A {@link CreateSchemaResponse} representing the submitted request and its current status.
     */
    public CreateSchemaResponse uploadAndStartCreateSchema(File file, CreateSchemaParameters parameters) throws CreateSchemaException {
        requireNonNull(file);

        if (file.isDirectory())
            throw new CreateSchemaException("directory_not_allowed", "The file can not be a directory");

        var requestId = UUID.randomUUID().toString();

        try {
            upload(requestId, new BufferedInputStream(new FileInputStream(file)), file.getName(), parameters, false);
            return startCreateSchema(requestId, file.getName(), parameters, false);
        } catch (FileNotFoundException e) {
            throw new CreateSchemaException("file_not_found", e.getMessage(), e);
        }
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference} and starts the create schema process.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @return A {@link CreateSchemaResponse} representing the submitted request and its current status.
     * @see #uploadAndStartCreateSchema(InputStream, String, CreateSchemaParameters)
     */
    public CreateSchemaResponse uploadAndStartCreateSchema(InputStream is, String fileName) throws CreateSchemaException {
        return uploadAndStartCreateSchema(is, fileName, null);
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference} and starts the create schema process.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @param parameters The configuration parameters for create schema.
     * @return A {@link CreateSchemaResponse} representing the submitted request and its current status.
     */
    public CreateSchemaResponse uploadAndStartCreateSchema(InputStream is, String fileName, CreateSchemaParameters parameters)
        throws CreateSchemaException {
        var requestId = UUID.randomUUID().toString();
        upload(requestId, is, fileName, parameters, false);
        return startCreateSchema(requestId, fileName, parameters, false);
    }

    /**
     * Starts the create schema process for a file that is already present in the configured {@link #documentReference document reference} and waits
     * until the schema is created.
     *
     * @param absolutePath The absolute path of the file.
     * @return A {@link CreateSchemaResult} with the generated schema.
     * @see #createSchemaAndFetch(String, CreateSchemaParameters)
     */
    public CreateSchemaResult createSchemaAndFetch(String absolutePath) throws CreateSchemaException {
        return createSchemaAndFetch(absolutePath, null);
    }

    /**
     * Starts the create schema process for a file that is already present in the configured {@link #documentReference document reference} and waits
     * until the schema is created.
     *
     * @param absolutePath The path of the document to create schema from.
     * @param parameters The configuration parameters for create schema.
     * @return A {@link CreateSchemaResult} with the generated schema.
     */
    public CreateSchemaResult createSchemaAndFetch(String absolutePath, CreateSchemaParameters parameters) throws CreateSchemaException {
        return createSchemaAndFetch(UUID.randomUUID().toString(), absolutePath, parameters);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference}, starts create schema process and waits until the schema
     * is created.
     *
     * @param file The local file to be uploaded and processed.
     * @return A {@link CreateSchemaResult} with the generated schema.
     * @see #uploadCreateSchemaAndFetch(File, CreateSchemaParameters)
     */
    public CreateSchemaResult uploadCreateSchemaAndFetch(File file) throws CreateSchemaException {
        return uploadCreateSchemaAndFetch(file, null);
    }

    /**
     * Uploads a local file in the configured {@link #documentReference document reference}, starts create schema process and waits until the schema
     * is created.
     *
     * @param file The local file to be uploaded and processed.
     * @param parameters The configuration parameters for create schema.
     * @return A {@link CreateSchemaResult} with the generated schema.
     */
    public CreateSchemaResult uploadCreateSchemaAndFetch(File file, CreateSchemaParameters parameters) throws CreateSchemaException {
        var requestId = UUID.randomUUID().toString();

        try {
            upload(requestId, new BufferedInputStream(new FileInputStream(file)), file.getName(), parameters, true);
            return createSchemaAndFetch(requestId, file.getName(), parameters);
        } catch (FileNotFoundException e) {
            throw new CreateSchemaException("file_not_found", e.getMessage(), e);
        }
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference}, starts create schema process and waits until
     * the schema is created.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @return A {@link CreateSchemaResult} with the generated schema.
     * @see #uploadCreateSchemaAndFetch(InputStream, String, CreateSchemaParameters)
     */
    public CreateSchemaResult uploadCreateSchemaAndFetch(InputStream is, String fileName) throws CreateSchemaException {
        return uploadCreateSchemaAndFetch(is, fileName, null);
    }

    /**
     * Uploads an {@code InputStream} in the configured {@link #documentReference document reference}, starts create schema process and waits until
     * the schema is created.
     *
     * @param is The input stream of the file to be uploaded and processed.
     * @param fileName The name of the file to be uploaded and processed.
     * @param parameters The configuration parameters for create schema.
     * @return A {@link CreateSchemaResult} with the generated schema.
     */
    public CreateSchemaResult uploadCreateSchemaAndFetch(InputStream is, String fileName, CreateSchemaParameters parameters)
        throws CreateSchemaException {
        var requestId = UUID.randomUUID().toString();
        upload(requestId, is, fileName, parameters, true);
        return createSchemaAndFetch(requestId, fileName, parameters);
    }

    /**
     * Retrieves the results of a create schema request by its unique identifier.
     * <p>
     * This operation fetches the details and results of a previously submitted create schema request. Note that the retention period for results is 2
     * days. If the request is older than 2 days, the results will no longer be available.
     *
     * @param id The unique identifier of the create schema request.
     * @return A {@link CreateSchemaResponse} containing the results of the request.
     */
    public CreateSchemaResponse fetchRequest(String id) {
        return fetchRequest(id, CreateSchemaFetchParameters.builder().build());
    }

    /**
     * Retrieves the results of a create schema request by its unique identifier.
     * <p>
     * This operation fetches the details and results of a previously submitted create schema request. Note that the retention period for results is 2
     * days. If the request is older than 2 days, the results will no longer be available.
     *
     * @param id The unique identifier of the create schema request.
     * @param parameters Parameters to specify the project or space context in which the request was made.
     * @return A {@link CreateSchemaResponse} containing the results of the request.
     */
    public CreateSchemaResponse fetchRequest(String id, CreateSchemaFetchParameters parameters) {
        return fetchCreateSchemaRequest(UUID.randomUUID().toString(), id, parameters);
    }

    /**
     * Uploads a file in the configured {@link #documentReference document reference}.
     *
     * @param file the file to be uploaded
     * @return {@code true} if the upload request was successfully sent
     * @throws CreateSchemaException if the file cannot be found or an error occurs during upload
     */
    public boolean uploadFile(File file) throws CreateSchemaException {
        try {
            return uploadFile(new BufferedInputStream(new FileInputStream(file)), file.getName());
        } catch (FileNotFoundException e) {
            throw new CreateSchemaException("file_not_found", e.getMessage(), e);
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
        return deleteFile(null, bucketName, fileName);
    }

    /**
     * Deletes a file from the specified bucket.
     *
     * @param bucketName The name of the bucket.
     * @param fileName The name of the file to delete.
     * @return true if the file was successfully deleted, false otherwise.
     */
    public boolean deleteFile(String transactionId, String bucketName, String fileName) throws FileNotFoundException {
        return client.deleteFile(DeleteFileRequest.of(transactionId, bucketName, fileName));
    }

    /**
     * Deletes a create schema request.
     *
     * @param id The unique identifier of the create schema request to delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id) {
        return deleteRequest(id, CreateSchemaDeleteParameters.builder().build());
    }

    /**
     * Deletes a create schema request.
     * <p>
     * This operation cancels the specified create schema request. If the {@code hardDelete} parameter is set to {@code true}, it will also delete the
     * associated job metadata.
     *
     * @param id The unique identifier of the create schema request to delete.
     * @param parameters Parameters specifying the space or project context, and whether to perform a hard delete.
     * @return {@code true} if the request was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteRequest(String id, CreateSchemaDeleteParameters parameters) {

        requireNonNull(id, "The id can not be null");

        var builder = CreateSchemaDeleteParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .hardDelete(parameters.hardDelete().orElse(null))
            .build();

        var request = new DeleteRequest(parameters.transactionId(), id, p);
        return client.deleteRequest(request);
    }

    //
    // Retrieves the results of a create schema request by its unique identifier.
    //
    private CreateSchemaResponse fetchCreateSchemaRequest(String requestId, String id, CreateSchemaFetchParameters parameters) {
        requireNonNull(requestId, "The requestId can not be null");
        requireNonNull(id, "The id can not be null");

        var builder = CreateSchemaFetchParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .build();

        var request = new FetchDetailsRequest(requestId, id, p);
        return client.fetchRequestDetails(request);
    }

    //
    // Start the create schema and wait until the result is ready.
    //
    private CreateSchemaResult createSchemaAndFetch(String requestId, String absolutePath, CreateSchemaParameters parameters)
        throws CreateSchemaException {
        requireNonNull(requestId, "requestId cannot be null");

        var createSchemaResponse = startCreateSchema(requestId, absolutePath, parameters, true);
        return waitForCompletion(requestId, createSchemaResponse, parameters).entity().results();
    }

    //
    // Uploads an inputstream to the Cloud Object Storage.
    //
    private void upload(String requestId, InputStream is, String fileName, CreateSchemaParameters parameters, boolean waitForCompletion) {
        requireNonNull(requestId, "requestId value cannot be null");
        requireNonNull(is, "is value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");

        boolean removeUploadedFile = false;
        CosReference documentReference = this.documentReference;

        if (nonNull(parameters)) {
            removeUploadedFile = parameters.isRemoveUploadedFile();
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
        }

        if (!waitForCompletion && removeUploadedFile)
            throw new IllegalArgumentException(
                "The asynchronous version of startCreateSchema doesn't allow the use of the \"removeUploadedFile\" parameter");


        var request = UploadRequest.of(requestId, documentReference.bucket(), is, fileName);
        client.uploadFile(request);
    }

    //
    // Starts the create schema process.
    //
    private CreateSchemaResponse startCreateSchema(String requestId, String path, CreateSchemaParameters parameters, boolean waitUntilJobIsDone)
        throws CreateSchemaException {
        requireNonNull(path);
        requireNonNull(requestId);

        String projectId = null;
        String spaceId = null;
        boolean removeUploadedFile = false;
        CosReference documentReference = this.documentReference;
        Parameters params = null;
        Duration timeout = this.timeout;
        String transactionId = null;

        if (nonNull(parameters)) {
            removeUploadedFile = parameters.isRemoveUploadedFile();
            projectId = parameters.projectId();
            spaceId = parameters.spaceId();
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
            params = parameters.toParameters();
            timeout = requireNonNullElse(parameters.timeout(), timeout);
            transactionId = parameters.transactionId();
        } else
            // This is needed to avoid "ocr_mode" equals "null" or "" in request body.
            params = CreateSchemaParameters.builder().build().toParameters();

        if (isNull(projectId) && isNull(spaceId)) {
            projectId = this.projectId;
            spaceId = this.spaceId;
        }

        if (!waitUntilJobIsDone && removeUploadedFile)
            throw new IllegalArgumentException(
                "The asynchronous version of startCreateSchema doesn't allow the use of the \"removeUploadedFile\" parameter");

        var createSchemaRequest = new CreateSchemaRequest(
            projectId,
            spaceId,
            documentReference.toDataReference(path),
            params
        );

        var request = new StartCreateSchemaRequest(requestId, transactionId, createSchemaRequest);
        var response = client.startRequest(request);

        if (!waitUntilJobIsDone)
            return response;

        Status status;
        long sleepTime = 100;
        LocalTime endTime = LocalTime.now().plus(timeout);
        String processId = null;

        do {

            if (LocalTime.now().isAfter(endTime)) {

                if (nonNull(processId)) {
                    deleteRequest(
                        processId,
                        CreateSchemaDeleteParameters.builder()
                            .projectId(projectId)
                            .spaceId(spaceId)
                            .transactionId(transactionId)
                            .build()
                    );
                }

                if (removeUploadedFile) {
                    try {
                        var encodedFileName = new URI(null, null, path, null).toASCIIString();
                        client.deleteFileAsync(DeleteFileRequest.of(requestId, documentReference.bucket(), encodedFileName));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                throw new CreateSchemaException("timeout",
                    "Execution to create schema for %s file took longer than the timeout set by %s milliseconds"
                        .formatted(path, timeout.toMillis()));
            }

            try {

                Thread.sleep(sleepTime);
                sleepTime *= 2;
                sleepTime = Math.min(sleepTime, 3000);

            } catch (Exception e) {
                throw new CreateSchemaException("interrupted", e.getMessage());
            }

            processId = response.metadata().id();
            response = fetchCreateSchemaRequest(requestId, processId, CreateSchemaFetchParameters.builder()
                .projectId(projectId)
                .spaceId(spaceId)
                .build());

            status = Status.fromValue(response.entity().results().status());
            var pagesProcessed = response.entity().results().numberPagesProcessed();
            logger.debug("Create schema status: {} for the file {} (pages processed {})", status, path, pagesProcessed);

        } while (status != Status.FAILED && status != Status.COMPLETED);

        return response;
    }

    //
    // Waits for the create schema process to complete.
    //
    private CreateSchemaResponse waitForCompletion(String requestId, CreateSchemaResponse createSchemaResponse, CreateSchemaParameters parameters)
        throws CreateSchemaException {

        requireNonNull(requestId);

        String uploadedPath = createSchemaResponse.entity().documentReference().location().fileName();
        Status status = Status.fromValue(createSchemaResponse.entity().results().status());
        boolean removeUploadedFile = false;
        CosReference documentReference = this.documentReference;

        if (nonNull(parameters)) {
            removeUploadedFile = parameters.isRemoveUploadedFile();
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
        }

        String documentBucketName = documentReference.bucket();

        try {

            return switch(status) {
                case COMPLETED -> createSchemaResponse;
                case FAILED -> {
                    var error = createSchemaResponse.entity().results().error();
                    throw new CreateSchemaException(error.code(), error.message());
                }
                default -> throw new CreateSchemaException("generic_error",
                    "Status %s not managed".formatted(status));
            };

        } finally {
            if (removeUploadedFile) {
                try {
                    var encodedFileName = new URI(null, null, uploadedPath, null).toASCIIString();
                    var request = DeleteFileRequest.of(requestId, documentBucketName, encodedFileName);
                    client.deleteFileAsync(request);
                } catch (Exception e) {
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
     * CreateSchemaService createSchemaService = CreateSchemaService.builder()
     *   .baseUrl("https://...")    // or use CloudRegion
     *   .cosUrl("https://...")     // or use CosUrl
     *   .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
     *   .projectId("project-id")
     *   .documentReference("<connection_id>", "<bucket-name>")
     *   .build();
     *
     * CreateSchemaResponse response = createSchemaService.startCreateSchema("myfile.pdf")
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link CreateSchemaService} instances with configurable parameters.
     */
    public final static class Builder extends ProjectService.Builder<Builder> {
        private String cosUrl;
        private Authenticator cosAuthenticator;
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
         * Specifies the Cloud Object Storage connection and bucket where the input files are stored.
         *
         * @param connectionId The id of the COS connection asset.
         * @param bucket The name of the bucket containing the input documents.
         */
        public Builder documentReference(String connectionId, String bucket) {
            return documentReference(CosReference.of(connectionId, bucket));
        }

        /**
         * Builds a {@link CreateSchemaService} instance using the configured parameters.
         *
         * @return a new instance of {@link CreateSchemaService}
         */
        public CreateSchemaService build() {
            return new CreateSchemaService(this);
        }
    }
}
