/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.file;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import com.ibm.watsonx.ai.WatsonxService.ProjectService;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service for interacting with IBM watsonx.ai Files APIss.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * FileService fileService = FileService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .projectId("my-project-id")
 *     .build();
 *
 * FileData fileData = fileService.upload(Path.of("mydata.jsonl"));
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public class FileService extends ProjectService {
    private final FileRestClient client;

    private FileService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = FileRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .authenticator(builder.authenticator())
            .httpClient(httpClient)
            .verifySsl(verifySsl)
            .build();
    }

    /**
     * Uploads a file using the provided {@link Path}.
     *
     * @param path the path to the file to upload
     * @return a {@link FileData} object containing the metadata of the uploaded file
     */
    public FileData upload(Path path) {
        return upload(path.toFile());
    }

    /**
     * Uploads a file.
     *
     * @param file the file to upload
     * @return a {@link FileData} object containing the metadata of the uploaded file
     */
    public FileData upload(File file) {
        try (var is = new FileInputStream(file)) {
            return upload(is, file.getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uploads a file using the provided {@link InputStream} and file name.
     *
     * @param is the input stream of the file content to upload
     * @param fileName the name of the file
     * @return a {@link FileData} object containing the metadata of the uploaded file
     */
    public FileData upload(InputStream is, String fileName) {
        return upload(
            FileUploadRequest.builder()
                .inputStream(is)
                .fileName(fileName)
                .purpose(Purpose.BATCH)
                .build());
    }

    /**
     * Uploads a file using the provided {@link FileUploadRequest}.
     *
     * @param request the {@link FileUploadRequest}
     * @return a {@link FileData} object containing the metadata of the uploaded file
     */
    public FileData upload(FileUploadRequest request) {
        requireNonNull(request, "request cannot be null");
        requireNonNull(request.inputStream(), "request.inputStream cannot be null");
        requireNonNull(request.fileName(), "request.fileName cannot be null");
        requireNonNull(request.purpose(), "request.purpose cannot be null");

        ProjectSpace projectSpace = resolveProjectSpace(request);

        return client.upload(
            FileUploadRequest.builder()
                .inputStream(request.inputStream())
                .fileName(request.fileName())
                .purpose(request.purpose())
                .projectId(projectSpace.projectId())
                .spaceId(projectSpace.spaceId())
                .transactionId(request.transactionId())
                .build());
    }

    /**
     * Returns a list of files that have been uploaded.
     *
     * @return a {@link FileListResponse} containing the list of files and pagination metadata
     */
    public FileListResponse list() {
        return list(null);
    }

    /**
     * Returns a list of files that have been uploaded, filtered by the provided request parameters.
     *
     * @param request the {@link FileListRequest} object
     * @return a {@link FileListResponse} containing the list of matching files and pagination metadata
     */
    public FileListResponse list(FileListRequest request) {

        if (isNull(request)) {
            request = FileListRequest.builder()
                .projectId(projectId)
                .spaceId(spaceId)
                .build();
        } else {
            ProjectSpace projectSpace = resolveProjectSpace(request);
            request = FileListRequest.builder()
                .projectId(projectSpace.projectId())
                .spaceId(projectSpace.spaceId())
                .transactionId(request.transactionId())
                .after(request.after())
                .limit(request.limit())
                .order(request.order())
                .purpose(request.purpose())
                .build();
        }

        return client.list(request);
    }

    /**
     * Retrieves the contents of an uploaded file by its identifier.
     *
     * @param fileId the identifier of the file to retrieve
     * @return content of the uploaded file
     */
    public String retrieve(String fileId) {
        var request = FileRetrieveRequest.builder()
            .projectId(projectId)
            .spaceId(spaceId)
            .fileId(fileId)
            .build();

        return retrieve(request);
    }

    /**
     * Retrieves the contents of an uploaded file using the provided {@link FileRetrieveRequest}.
     *
     * @param request the {@link FileRetrieveRequest} object
     * @return content of the uploaded file
     */
    public String retrieve(FileRetrieveRequest request) {
        requireNonNull(request, "request cannot be null");
        requireNonNull(request.fileId(), "request.fileId cannot be null");

        ProjectSpace projectSpace = resolveProjectSpace(request);
        request = FileRetrieveRequest.builder()
            .projectId(projectSpace.projectId())
            .spaceId(projectSpace.spaceId())
            .transactionId(request.transactionId())
            .fileId(request.fileId())
            .build();

        return client.retrieve(request);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * FileService fileService = FileService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
     *     .projectId("my-project-id")
     *     .build();
     *
     * FileUploadResponse response = fileService.upload(Path.of("mydata.jsonl"));
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link FileService} instances with configurable parameters.
     */
    public final static class Builder extends ProjectService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link FileService} instance using the configured parameters.
         *
         * @return a new instance of {@link FileService}
         */
        public FileService build() {
            return new FileService(this);
        }
    }
}
