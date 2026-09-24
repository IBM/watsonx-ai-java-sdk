/*
 * Copyright 2025 IBM Corporation
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
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
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
import com.ibm.watsonx.ai.textprocessing.Status;
import com.ibm.watsonx.ai.textprocessing.storage.StorageFactory;
import com.ibm.watsonx.ai.textprocessing.storage.StorageOperations;
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
 *     .baseUrl("https://...")    // or use CloudRegion
 *     .cosUrl("https://...")     // or use CosUrl
 *     .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
 *     .projectId("project-id")
 *     .documentReference(CosReference.of("<connection_id>", "<bucket-name>"))
 *     .build();
 *
 * TextClassificationResponse response = textClassificationService.startClassification("myfile.pdf");
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class TextClassificationService extends ScopedService {
    private static final Logger logger = LoggerFactory.getLogger(TextClassificationService.class);
    private final String cosUrl;
    private final DocumentReference documentReference;
    private final TextClassificationRestClient client;
    private volatile StorageOperations cosService;
    private final ReentrantLock cosServiceLock = new ReentrantLock();
    private final ProjectService lazyProjectService;
    private final Authenticator authenticator;
    private final Authenticator cosAuthenticator;

    private TextClassificationService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        boolean needsCos = builder.documentReference instanceof CosReference;
        var tmpUrl = needsCos
            ? requireNonNull(builder.cosUrl, "cosUrl value cannot be null")
            : requireNonNullElse(builder.cosUrl, "");
        cosUrl = tmpUrl.endsWith("/") ? tmpUrl.substring(0, tmpUrl.length() - 1) : tmpUrl;
        documentReference = requireNonNull(builder.documentReference, "documentReference value cannot be null");
        authenticator = builder.authenticator();
        cosAuthenticator = builder.cosAuthenticator;
        lazyProjectService = builder.projectService;
        client = TextClassificationRestClient.builder()
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
     * <b>Note:</b> This method does not return the classification result. Use {@link #classifyAndFetch(String, TextClassificationParameters)} to run
     * the classification and fetch the result immediately.
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
     * <b>Note:</b> This method does not return the classification result. Use {@link #uploadClassifyAndFetch(File, TextClassificationParameters)} to
     * get the classification immediately.
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

        try (var inputStream = new BufferedInputStream(new FileInputStream(file))) {
            upload(requestId, inputStream, file.getName(), parameters, false);
            return startClassification(requestId, file.getName(), parameters, false);
        } catch (FileNotFoundException e) {
            throw new TextClassificationException("file_not_found", e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
     * @return A {@link TextClassificationResponse} representing the submitted request and its current status.
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
     * @return A {@link TextClassificationResponse} representing the submitted request and its current status.
     * @see #uploadClassifyAndFetch(InputStream, String, TextClassificationParameters)
     */
    public TextClassificationResponse uploadAndStartClassification(InputStream is, String fileName, TextClassificationParameters parameters)
        throws TextClassificationException {
        requireNonNull(is, "is value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");
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
     * @see #classifyAndFetch(String, TextClassificationParameters)
     */
    public ClassificationResult classifyAndFetch(String absolutePath) throws TextClassificationException {
        return classifyAndFetch(absolutePath, null);
    }

    /**
     * Starts the text classification process for a file that is already present in the configured {@link #documentReference document reference} and
     * returns the classification result.
     * <p>
     * <b>Note on {@code removeUploadedFile}:</b> if {@code parameters.removeUploadedFile()} is {@code true}, this method deletes the document at
     * {@code absolutePath} from storage after processing, even though no file was uploaded by this call. Use this option with caution when calling
     * this method on a pre-existing document.
     *
     * @param absolutePath The path of the document to be classified.
     * @param parameters The configuration parameters for text classification.
     * @return The classification result.
     */
    public ClassificationResult classifyAndFetch(String absolutePath, TextClassificationParameters parameters) throws TextClassificationException {
        return classifyAndFetch(UUID.randomUUID().toString(), absolutePath, parameters, absolutePath);
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
        requireNonNull(file);
        if (file.isDirectory())
            throw new TextClassificationException("directory_not_allowed", "The file can not be a directory");

        var requestId = UUID.randomUUID().toString();

        try (var inputStream = new BufferedInputStream(new FileInputStream(file))) {
            upload(requestId, inputStream, file.getName(), parameters, true);
        } catch (FileNotFoundException e) {
            throw new TextClassificationException("file_not_found", e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return classifyAndFetch(requestId, file.getName(), parameters, file.getName());
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
        return classifyAndFetch(requestId, fileName, parameters, fileName);
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
        requireNonNull(parameters, "parameters cannot be null");
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
        requireNonNull(file);

        if (file.isDirectory())
            throw new TextClassificationException("directory_not_allowed", "The file can not be a directory");

        try (var inputStream = new BufferedInputStream(new FileInputStream(file))) {
            return uploadFile(inputStream, file.getName());
        } catch (FileNotFoundException e) {
            throw new TextClassificationException("file_not_found", e.getMessage(), e);
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
     * Deletes a file from the configured document reference storage.
     * <p>
     * When the service is configured with a {@link CosReference} document reference, {@code bucketName} identifies the COS bucket and
     * {@code fileName} the object key. When configured with a {@link ContainerReference}, the bucket is resolved automatically from the project
     * storage and {@code bucketName} is ignored.
     *
     * @param bucketName The name of the COS bucket. Ignored when using a {@link ContainerReference}.
     * @param fileName The name of the file to delete.
     * @return {@code true} if the file was successfully deleted, {@code false} otherwise.
     */
    public boolean deleteFile(String bucketName, String fileName) throws FileNotFoundException {
        var requestId = UUID.randomUUID().toString();
        if (documentReference instanceof ContainerReference)
            return getOrResolveCosService().deleteFile(requestId, fileName);
        return client.deleteFile(DeleteFileRequest.of(requestId, bucketName, fileName));
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
        requireNonNull(parameters, "parameters cannot be null");

        var builder = TextClassificationDeleteParameters.builder();
        ofNullable(parameters.projectId()).ifPresent(builder::projectId);
        ofNullable(parameters.spaceId()).ifPresent(builder::spaceId);

        if (isNull(parameters.projectId()) && isNull(parameters.spaceId()))
            builder.projectId(projectId).spaceId(spaceId);

        var p = builder
            .transactionId(parameters.transactionId())
            .hardDelete(parameters.hardDelete().orElse(null))
            .build();

        var requestTrackingId = UUID.randomUUID().toString();
        var request = DeleteClassificationRequest.of(requestTrackingId, id, p);
        return client.deleteClassification(request);
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
                .storage()
                .properties();

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

    // Retrieves the classification result for the given request id.
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

    // Starts the text classification and waits until the result is ready.
    private ClassificationResult classifyAndFetch(String requestId, String absolutePath, TextClassificationParameters parameters, String uploadedPath)
        throws TextClassificationException {
        requireNonNull(requestId, "requestId cannot be null");
        requireNonNull(absolutePath, "absolutePath cannot be null");

        try {
            var textClassificationResponse = startClassification(requestId, absolutePath, parameters, true);
            return getClassificationResult(textClassificationResponse);
        } finally {
            if (nonNull(parameters) && parameters.isRemoveUploadedFile()) {
                DocumentReference effectiveDoc = parameters.documentReference() != null
                    ? parameters.documentReference()
                    : this.documentReference;
                cleanUpUploadedFile(requestId, uploadedPath, effectiveDoc);
            }
        }
    }

    // Uploads an input stream to COS or the container.
    private void upload(String requestId, InputStream is, String fileName, TextClassificationParameters parameters,
        boolean waitForClassification) {
        requireNonNull(requestId, "requestId value cannot be null");
        requireNonNull(is, "is value cannot be null");
        requireNonNull(fileName, "fileName value cannot be null");

        boolean removeUploadedFile = nonNull(parameters) && parameters.isRemoveUploadedFile();

        if (!waitForClassification && removeUploadedFile)
            throw new IllegalArgumentException(
                "The asynchronous version of startClassification doesn't allow the use of the \"removeUploadedFile\" parameter");

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

    // Starts the text classification process.
    private TextClassificationResponse startClassification(String requestId, String path, TextClassificationParameters parameters,
        boolean waitUntilJobIsDone)
        throws TextClassificationException {
        requireNonNull(path);
        requireNonNull(requestId);

        String projectId = null;
        String spaceId = null;
        boolean removeUploadedFile = false;
        DocumentReference documentReference = this.documentReference;
        Parameters params = null;
        Map<String, Object> custom = null;
        Duration timeout = this.timeout;
        String transactionId = null;

        if (nonNull(parameters)) {
            removeUploadedFile = parameters.isRemoveUploadedFile();
            projectId = parameters.projectId();
            spaceId = parameters.spaceId();
            documentReference = requireNonNullElse(parameters.documentReference(), this.documentReference);
            params = parameters.toParameters();
            custom = parameters.custom();
            timeout = requireNonNullElse(parameters.timeout(), timeout);
            transactionId = parameters.transactionId();
        }

        if (isNull(projectId) && isNull(spaceId)) {
            projectId = this.projectId;
            spaceId = this.spaceId;
        }

        if (!waitUntilJobIsDone && removeUploadedFile)
            throw new IllegalArgumentException(
                "The asynchronous version of startClassification doesn't allow the use of the \"removeUploadedFile\" parameter");

        if (documentReference instanceof CosReference && cosUrl.isBlank())
            throw new IllegalStateException(
                "cosUrl must be set on the service builder when using a CosReference document reference.");

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
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        String processId = response.metadata().id();

        do {
            if (System.nanoTime() - deadlineNanos >= 0) {
                cleanUpAfterAbortedClassification(processId, projectId, spaceId, transactionId);
                throw new TextClassificationException("timeout",
                    "The execution of the classification %s file took longer than the timeout set by %s milliseconds"
                        .formatted(path, timeout.toMillis()));
            }

            try {
                long remaining = deadlineNanos - System.nanoTime();
                Thread.sleep(Math.min(sleepTime, Math.max(remaining / 1_000_000L, 0L)));
                sleepTime = Math.min(sleepTime * 2, 3000);
            } catch (InterruptedException e) {
                try {
                    cleanUpAfterAbortedClassification(processId, projectId, spaceId, transactionId);
                } finally {
                    Thread.currentThread().interrupt();
                }
                throw new TextClassificationException("interrupted", e.getMessage(), e);
            }

            try {
                response = fetchClassificationRequest(requestId, processId, TextClassificationFetchParameters.builder()
                    .projectId(projectId)
                    .spaceId(spaceId)
                    .build());
                status = Status.fromValue(response.entity().results().status());
                logger.debug("Classification status: {} for the file {}", status, path);
            } catch (Exception e) {
                cleanUpAfterAbortedClassification(processId, projectId, spaceId, transactionId);
                throw e;
            }

        } while (status != Status.FAILED && status != Status.COMPLETED);

        return response;
    }

    // Cancels the classification job on timeout, interrupt, or fetch error. Logs but never throws.
    private void cleanUpAfterAbortedClassification(String processId, String projectId, String spaceId,
        String transactionId) {

        if (nonNull(processId)) {
            try {
                deleteRequest(
                    processId,
                    TextClassificationDeleteParameters.builder()
                        .projectId(projectId)
                        .spaceId(spaceId)
                        .transactionId(transactionId)
                        .build());
            } catch (Exception e) {
                logger.warn("Failed to cancel classification job {}: {}", processId, e.getMessage(), e);
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

    // Extracts the ClassificationResult from a completed-or-failed response.
    private ClassificationResult getClassificationResult(TextClassificationResponse textClassificationResponse)
        throws TextClassificationException {

        Status status = Status.fromValue(textClassificationResponse.entity().results().status());

        if (status != Status.COMPLETED) {
            var error = textClassificationResponse.entity().results().error();
            if (isNull(error))
                throw new TextClassificationException("generic_error", "The classification failed without error details");

            throw new TextClassificationException(error.code(), error.message());
        }

        return textClassificationResponse.entity().results();
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextClassificationService textClassificationService = TextClassificationService.builder()
     *     .baseUrl("https://...")    // or use CloudRegion
     *     .cosUrl("https://...")     // or use CosUrl
     *     .apiKey("my-api-key")      // creates an IBM Cloud Authenticator
     *     .projectId("project-id")
     *     .documentReference(CosReference.of("<connection_id>", "<bucket-name>"))
     *     .build();
     *
     * TextClassificationResponse response = textClassificationService.startClassification("myfile.pdf");
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
    public final static class Builder extends ScopedService.Builder<Builder> {
        private String cosUrl;
        private Authenticator cosAuthenticator;
        private DocumentReference documentReference;
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
         * Specifies a {@link ProjectService} to use for resolving the COS bucket and endpoint URL when a {@link ContainerReference} is configured.
         * <p>
         * When not set, the service attempts to resolve the project storage automatically from the {@code baseUrl} if it matches a known
         * {@link CloudRegion}. For custom or on-premise deployments where the base URL is not a standard cloud region, provide an explicit
         * {@link ProjectService} instance here.
         *
         * @param projectService the {@link ProjectService} to use for project storage resolution
         */
        public Builder projectService(ProjectService projectService) {
            this.projectService = projectService;
            return this;
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
