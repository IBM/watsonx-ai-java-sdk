/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Defines the storage operations used internally by text processing services.
 */
public interface StorageOperations {

    /**
     * Uploads a file from an {@link InputStream}.
     *
     * @param is the input stream of the file content
     * @param fileName the name of the file in the storage backend
     * @return {@code true} if the upload succeeded
     */
    default boolean upload(InputStream is, String fileName) {
        return upload(UUID.randomUUID().toString(), is, fileName);
    }

    /**
     * Uploads a file from an {@link InputStream}.
     *
     * @param requestId a tracking id used to correlate log entries for this request
     * @param is the input stream of the file content
     * @param fileName the name of the file in the storage backend
     * @return {@code true} if the upload succeeded
     */
    boolean upload(String requestId, InputStream is, String fileName);

    /**
     * Uploads a local {@link File}.
     *
     * @param file the local file to upload
     * @return {@code true} if the upload succeeded
     */
    default boolean upload(File file) {
        return upload(UUID.randomUUID().toString(), file);
    }

    /**
     * Uploads a local {@link File}.
     *
     * @param requestId a tracking id used to correlate log entries for this request
     * @param file the local file to upload
     * @return {@code true} if the upload succeeded
     */
    boolean upload(String requestId, File file);

    /**
     * Uploads a file from an {@link InputStream} and returns an identifier for the stored file, or {@code null} if the backend does not assign one.
     *
     * @param is the input stream of the file content
     * @param fileName the name of the file in the storage backend
     * @return an opaque file identifier, or {@code null} if not applicable for this backend
     */
    default String uploadAndGetId(InputStream is, String fileName) {
        return uploadAndGetId(UUID.randomUUID().toString(), is, fileName);
    }

    /**
     * Uploads a file from an {@link InputStream} and returns an identifier for the stored file, or {@code null} if the backend does not assign one.
     *
     * @param requestId a tracking id used to correlate log entries for this request
     * @param is the input stream of the file content
     * @param fileName the name of the file in the storage backend
     * @return an opaque file identifier, or {@code null} if not applicable for this backend
     */
    String uploadAndGetId(String requestId, InputStream is, String fileName);

    /**
     * Reads the content of a file as a string.
     *
     * @param fileName the path of the file within the storage location
     * @return the file content
     * @throws FileNotFoundException if the file does not exist
     */
    default String readFile(String fileName) throws FileNotFoundException {
        return readFile(UUID.randomUUID().toString(), fileName);
    }

    /**
     * Reads the content of a file as a string.
     *
     * @param requestId a tracking id used to correlate log entries for this request
     * @param fileName the path of the file within the storage location
     * @return the file content
     * @throws FileNotFoundException if the file does not exist
     */
    String readFile(String requestId, String fileName) throws FileNotFoundException;

    /**
     * Deletes a file.
     *
     * @param fileName the path of the file within the storage location
     * @return {@code true} if the file was successfully deleted
     * @throws FileNotFoundException if the file does not exist
     */
    default boolean deleteFile(String fileName) throws FileNotFoundException {
        return deleteFile(UUID.randomUUID().toString(), fileName);
    }

    /**
     * Deletes a file.
     *
     * @param requestId a tracking id used to correlate log entries for this request
     * @param fileName the path of the file within the storage location
     * @return {@code true} if the file was successfully deleted
     * @throws FileNotFoundException if the file does not exist
     */
    boolean deleteFile(String requestId, String fileName) throws FileNotFoundException;

    /**
     * Asynchronously deletes a file. Errors are logged as warnings and not propagated to the caller.
     *
     * @param fileName the path of the file within the storage location
     */
    default void deleteFileAsync(String fileName) {
        deleteFileAsync(UUID.randomUUID().toString(), fileName);
    }

    /**
     * Asynchronously deletes a file. Errors are logged as warnings and not propagated to the caller.
     *
     * @param requestId a tracking id used to correlate log entries for this request
     * @param fileName the path of the file within the storage location
     */
    void deleteFileAsync(String requestId, String fileName);
}
