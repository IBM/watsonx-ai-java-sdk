/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.batch;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.Json;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.spi.json.TypeToken;
import com.ibm.watsonx.ai.file.FileService;

/**
 * Service for interacting with IBM watsonx.ai Batches API.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * BatchService batchService = BatchService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .projectId("my-project-id")
 *     .endpoint("/v1/chat/completions")
 *     .fileService(fileService)
 *     .build();
 *
 * BatchData batchData = batchService.submit(Path.of("mydata.jsonl"));
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 * @see FileService
 */
public class BatchService extends ProjectService {
    private final BatchRestClient client;
    private final FileService fileService;
    private final String endpoint;

    private BatchService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        endpoint = requireNonNull(builder.endpoint, "the default endpoint cannot be null");
        client = BatchRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
        fileService = builder.fileService;
    }

    /**
     * Submits a batch job using the file at the given {@link Path}.
     *
     * @param path the path to the JSONL file to upload and submit
     * @return a {@link BatchData} object containing the metadata of the submitted batch job
     */
    public BatchData submit(Path path) {
        return submit(path, null);
    }

    /**
     * Submits a batch job using the file at the given {@link Path} and waits for completion, then returns the results deserialized as a list of
     * {@link BatchResult}.
     *
     * @param <T> the type of the response body in each batch result
     * @param path the path to the JSONL file to upload and submit
     * @param clazz the class to deserialize each result's response body into
     * @return a list of {@link BatchResult} containing the deserialized responses
     */
    public <T> List<BatchResult<T>> submitAndFetch(Path path, Class<T> clazz) {
        return submitAndFetch(path, null, clazz);
    }

    /**
     * Submits a batch job using the file at the given {@link Path} with additional parameters.
     *
     * @param path the path to the JSONL file to upload and submit
     * @param parameters optional {@link BatchCreateRequest} providing additional parameters
     * @return a {@link BatchData} object containing the metadata of the submitted batch job
     */
    public BatchData submit(Path path, BatchCreateRequest parameters) {
        requireNonNull(path, "path cannot be null");
        return submit(path.toFile(), parameters);
    }

    /**
     * Submits a batch job using the file at the given {@link Path} with additional parameters, waits for completion, then returns the results
     * deserialized as a list of {@link BatchResult}.
     *
     * @param <T> the type of the response body in each batch result
     * @param path the path to the JSONL file to upload and submit
     * @param parameters optional {@link BatchCreateRequest} providing additional parameters
     * @param clazz the class to deserialize each result's response body into
     * @return a list of {@link BatchResult} containing the deserialized responses
     */
    public <T> List<BatchResult<T>> submitAndFetch(Path path, BatchCreateRequest parameters, Class<T> clazz) {
        requireNonNull(path, "path cannot be null");
        return submitAndFetch(path.toFile(), parameters, clazz);
    }

    /**
     * Submits a batch job using the given {@link File}.
     *
     * @param file the JSONL file to upload and submit
     * @return a {@link BatchData} object containing the metadata of the submitted batch job
     */
    public BatchData submit(File file) {
        return submit(file, null);
    }

    /**
     * Submits a batch job using the given {@link File} and waits for completion, then returns the results deserialized as a list of
     * {@link BatchResult}.
     *
     * @param <T> the type of the response body in each batch result
     * @param file the JSONL file to upload and submit
     * @param clazz the class to deserialize each result's response body into
     * @return a list of {@link BatchResult} containing the deserialized responses
     */
    public <T> List<BatchResult<T>> submitAndFetch(File file, Class<T> clazz) {
        return submitAndFetch(file, null, clazz);
    }

    /**
     * Submits a batch job using the given {@link File} with additional parameters.
     *
     * @param file the JSONL file to upload and submit
     * @param parameters optional {@link BatchCreateRequest} providing additional parameters
     * @return a {@link BatchData} object containing the metadata of the submitted batch job
     */
    public BatchData submit(File file, BatchCreateRequest parameters) {
        requireNonNull(file, "file cannot be null");
        try (var is = new FileInputStream(file)) {
            return submit(is, file.getName(), parameters);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Submits a batch job using the given {@link File} with additional parameters, waits for completion, then returns the results deserialized as a
     * list of {@link BatchResult}.
     *
     * @param <T> the type of the response body in each batch result
     * @param file the JSONL file to upload and submit
     * @param parameters optional {@link BatchCreateRequest} providing additional parameters
     * @param clazz the class to deserialize each result's response body into
     * @return a list of {@link BatchResult} containing the deserialized responses
     */
    public <T> List<BatchResult<T>> submitAndFetch(File file, BatchCreateRequest parameters, Class<T> clazz) {
        requireNonNull(file, "file cannot be null");
        try (var is = new FileInputStream(file)) {
            return submitAndFetch(is, file.getName(), parameters, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Submits a batch job using the given {@link InputStream}.
     *
     * @param inputStream the input stream of the JSONL file content to upload and submit
     * @return a {@link BatchData} object containing the metadata of the submitted batch job
     */
    public BatchData submit(InputStream inputStream) {
        return submit(inputStream, UUID.randomUUID().toString());
    }

    /**
     * Submits a batch job using the given {@link InputStream} and file name.
     *
     * @param inputStream the input stream of the JSONL file content to upload and submit
     * @param fileName the name of the file
     * @return a {@link BatchData} object containing the metadata of the submitted batch job
     */
    public BatchData submit(InputStream inputStream, String fileName) {
        return submit(inputStream, fileName, null);
    }

    /**
     * Submits a batch job using the given {@link InputStream}, waits for completion, then returns the results deserialized as a list of
     * {@link BatchResult}.
     *
     * @param <T> the type of the response body in each batch result
     * @param inputStream the input stream of the JSONL file content to upload and submit
     * @param clazz the class to deserialize each result's response body into
     * @return a list of {@link BatchResult} containing the deserialized responses
     */
    public <T> List<BatchResult<T>> submitAndFetch(InputStream inputStream, Class<T> clazz) {
        return submitAndFetch(inputStream, UUID.randomUUID().toString(), clazz);
    }

    /**
     * Submits a batch job using the given {@link InputStream} and file name, waits for completion, then returns the results deserialized as a list of
     * {@link BatchResult}.
     *
     * @param <T> the type of the response body in each batch result
     * @param inputStream the input stream of the JSONL file content to upload and submit
     * @param fileName the name of the file
     * @param clazz the class to deserialize each result's response body into
     * @return a list of {@link BatchResult} containing the deserialized responses
     */
    public <T> List<BatchResult<T>> submitAndFetch(InputStream inputStream, String fileName, Class<T> clazz) {
        return submitAndFetch(inputStream, fileName, null, clazz);
    }

    /**
     * Submits a batch job using the given {@link InputStream}, file name, and additional parameters.
     *
     * @param inputStream the input stream of the JSONL file content to upload and submit
     * @param fileName the name of the file
     * @param parameters optional {@link BatchCreateRequest} providing additional parameters
     * @return a {@link BatchData} object containing the metadata of the submitted batch job
     */
    public BatchData submit(InputStream inputStream, String fileName, BatchCreateRequest parameters) {
        return submit(prepareRequest(inputStream, fileName, parameters));
    }

    /**
     * Submits a batch job using the given {@link InputStream}, file name, and additional parameters, waits for completion, then returns the results
     * deserialized as a list of {@link BatchResult}.
     *
     * @param <T> the type of the response body in each batch result
     * @param inputStream the input stream of the JSONL file content to upload and submit
     * @param fileName the name of the file
     * @param parameters optional {@link BatchCreateRequest} providing additional parameters
     * @param clazz the class to deserialize each result's response body into
     * @return a list of {@link BatchResult} containing the deserialized responses
     */
    public <T> List<BatchResult<T>> submitAndFetch(InputStream inputStream, String fileName, BatchCreateRequest parameters, Class<T> clazz) {
        return submitAndFetch(prepareRequest(inputStream, fileName, parameters), clazz);
    }

    /**
     * Submits a batch job using the provided {@link BatchCreateRequest}, waits for completion, then returns the results deserialized as a list of
     * {@link BatchResult}.
     *
     * @param <T> the type of the response body in each batch result
     * @param request the {@link BatchCreateRequest} describing the batch job
     * @param clazz the class to deserialize each result's response body into
     * @return a list of {@link BatchResult} containing the deserialized responses
     */
    public <T> List<BatchResult<T>> submitAndFetch(BatchCreateRequest request, Class<T> clazz) {
        requireNonNull(fileService, "To wait for the completion of a batch operation, it is necessary to set the FileService");

        var batchData = submit(request);
        var timeout = requireNonNullElse(request.timeout(), this.timeout);
        var sleepTime = 100;
        var endTime = LocalTime.now().plus(timeout);
        var projectSpace = resolveProjectSpace(request);
        var status = Status.fromValue(batchData.status());

        while (status != Status.COMPLETED && status != Status.FAILED) {

            if (LocalTime.now().isAfter(endTime))
                throw new RuntimeException(
                    "The execution of the batch operation for the file \"%s\" took longer than the timeout set by %s milliseconds"
                        .formatted(request.inputFileId(), timeout.toMillis()));

            try {

                Thread.sleep(sleepTime);
                sleepTime *= 2;
                sleepTime = Math.min(sleepTime, 3000);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            batchData = client.retrieve(
                BatchRetrieveRequest.builder()
                    .batchId(batchData.id())
                    .projectId(projectSpace.projectId())
                    .spaceId(projectSpace.spaceId())
                    .build());

            status = Status.fromValue(batchData.status());
        }

        if (status == Status.FAILED)
            throw new RuntimeException("The batch operation failed: %s".formatted(batchData));

        var batchOutput = fileService.retrieve(batchData.outputFileId());

        return batchOutput.lines()
            .filter(line -> !line.isBlank())
            .map(line -> Json.<BatchResult<T>>fromJson(line, TypeToken.parameterizedOf(BatchResult.class, clazz)))
            .toList();
    }

    /**
     * Submits a batch job using the provided {@link BatchCreateRequest}.
     *
     * @param request the {@link BatchCreateRequest} describing the batch job
     * @return a {@link BatchData} object containing the metadata of the submitted batch job
     */
    public BatchData submit(BatchCreateRequest request) {
        requireNonNull(request, "request cannot be null");
        requireNonNull(request.inputFileId(), "request.inputFileId cannot be null");

        var projectSpace = resolveProjectSpace(request);
        var endpoint = nonNull(request.endpoint()) ? request.endpoint() : this.endpoint;

        return client.submit(
            BatchCreateRequest.builder(request)
                .projectId(projectSpace.projectId())
                .spaceId(projectSpace.spaceId())
                .endpoint(endpoint)
                .build());
    }

    /**
     * Retrieves the details of a batch job by its identifier.
     *
     * @param batchId the unique identifier of the batch job to retrieve
     * @return a {@link BatchData} object containing the current metadata of the batch job
     */
    public BatchData retrieve(String batchId) {
        return retrieve(BatchRetrieveRequest.builder().batchId(batchId).build());
    }

    /**
     * Retrieves the details of a batch job by its identifier.
     *
     * @param request the {@link BatchRetrieveRequest} containing the batch job identifier
     * @return a {@link BatchData} object containing the current metadata of the batch job
     */
    public BatchData retrieve(BatchRetrieveRequest request) {
        requireNonNull(request, "request cannot be null");
        requireNonNull(request.batchId(), "request.batchId cannot be null");

        var projectSpace = resolveProjectSpace(request);

        return client.retrieve(
            BatchRetrieveRequest.builder()
                .projectId(projectSpace.projectId())
                .spaceId(projectSpace.spaceId())
                .batchId(request.batchId())
                .transactionId(request.transactionId())
                .build());
    }

    /**
     * Lists all batch jobs.
     *
     * @return a {@link BatchListResponse} containing the list of batch jobs
     */
    public BatchListResponse list() {
        return list(BatchListRequest.builder().build());
    }

    /**
     * Lists batch jobs according to the provided {@link BatchListRequest}.
     *
     * @param request the {@link BatchListRequest} containing optional filtering and pagination parameters
     * @return a {@link BatchListResponse} containing the list of batch jobs
     */
    public BatchListResponse list(BatchListRequest request) {
        requireNonNull(request, "request cannot be null");
        var projectSpace = resolveProjectSpace(request);
        return client.list(
            BatchListRequest.builder()
                .limit(request.limit())
                .projectId(projectSpace.projectId())
                .spaceId(projectSpace.spaceId())
                .transactionId(request.transactionId())
                .build());
    }

    /**
     * Cancels a batch job by its identifier.
     *
     * @param batchId the unique identifier of the batch job to cancel
     * @return a {@link BatchData} object containing the updated metadata of the cancelled batch job
     */
    public BatchData cancel(String batchId) {
        return cancel(BatchCancelRequest.builder().batchId(batchId).build());
    }

    /**
     * Cancels a batch job using the provided {@link BatchCancelRequest}.
     *
     * @param request the {@link BatchCancelRequest} containing the batch job identifier and optional parameters
     * @return a {@link BatchData} object containing the updated metadata of the cancelled batch job
     */
    public BatchData cancel(BatchCancelRequest request) {
        requireNonNull(request, "request cannot be null");
        requireNonNull(request.batchId(), "request.batchId cannot be null");

        var projectSpace = resolveProjectSpace(request);

        return client.cancel(
            BatchCancelRequest.builder()
                .projectId(projectSpace.projectId())
                .spaceId(projectSpace.spaceId())
                .batchId(request.batchId())
                .transactionId(request.transactionId())
                .build());
    }

    /**
     * Validates the inputs, uploads the file via {@link FileService}, and builds a {@link BatchCreateRequest} with the assigned {@code fileId}.
     *
     * @param inputStream the input stream of the file to upload
     * @param fileName the name of the file to upload
     * @param parameters optional {@link BatchCreateRequest} providing additional parameters; {@code inputFileId} must not be set
     * @return a {@link BatchCreateRequest} ready to be submitted, with {@code inputFileId} set
     * @throws IllegalArgumentException if {@code parameters.inputFileId} is already set
     */
    private BatchCreateRequest prepareRequest(InputStream inputStream, String fileName, BatchCreateRequest parameters) {
        requireNonNull(fileService, "To upload a file it is mandatory to set the FileService");
        requireNonNull(inputStream, "inputstream cannot be null");
        requireNonNull(fileName, "fileName cannot be null");
        parameters = requireNonNullElse(parameters, BatchCreateRequest.builder().build());

        if (nonNull(parameters.inputFileId()))
            throw new IllegalArgumentException(
                "parameters.inputFileId must not be set when submitting via file upload, the inputFileId is assigned automatically after upload");

        var fileId = fileService.upload(inputStream, fileName).id();
        return BatchCreateRequest.builder(parameters).inputFileId(fileId).build();
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * BatchService batchService = BatchService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .projectId("my-project-id")
     *     .endpoint("/v1/chat/completions")
     *     .fileService(fileService)
     *     .build();
     * }</pre>
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link BatchService} instances with configurable parameters.
     */
    public final static class Builder extends ProjectService.Builder<Builder> {
        private FileService fileService;
        private String endpoint;

        private Builder() {}

        /**
         * Sets the {@link FileService} used to upload files and retrieve batch output.
         * <p>
         * Required when submitting jobs via file upload (i.e., using {@link Path}, {@link File}, or {@link InputStream} overloads) or when using
         * {@code submitAndFetch}.
         *
         * @param fileService the {@link FileService} instance to use
         * @return this builder
         */
        public Builder fileService(FileService fileService) {
            this.fileService = fileService;
            return this;
        }

        /**
         * Sets the default endpoint for batch inference requests (e.g., {@code /v1/chat/completions}).
         * <p>
         * This value is used when the submitted {@link BatchCreateRequest} does not specify an endpoint.
         *
         * @param endpoint the default endpoint path
         * @return this builder
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Builds a {@link BatchService} instance using the configured parameters.
         *
         * @return a new instance of {@link BatchService}
         */
        public BatchService build() {
            return new BatchService(this);
        }
    }
}
