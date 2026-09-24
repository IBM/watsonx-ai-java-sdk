/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage;

import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.http.logging.HttpRequestLogger;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;
import com.ibm.watsonx.ai.textprocessing.storage.cos.CosStorageService;

/**
 * Factory for creating {@link StorageOperations} instances.
 */
public final class StorageFactory {

    private StorageFactory() {}

    /**
     * Creates a {@link StorageOperations} backed by IBM Cloud Object Storage, scoped to the given bucket.
     *
     * @param cosUrl the COS endpoint URL
     * @param bucket the COS bucket name for all operations on this instance
     * @param authenticator the primary authenticator
     * @param cosAuthenticator optional dedicated COS authenticator (may be {@code null})
     * @param httpClient optional custom HTTP client (may be {@code null})
     * @param timeout request timeout
     * @param logRequests whether to log outgoing requests
     * @param logResponses whether to log incoming responses
     * @param requestLogger custom request logger (may be {@code null})
     * @param requestLogLevel log level for requests
     * @param responseLogger custom response logger (may be {@code null})
     * @param responseLogLevel log level for responses
     * @param verifySsl whether to verify SSL certificates
     * @return a new {@link StorageOperations} backed by COS, scoped to {@code bucket}
     */
    public static StorageOperations cos(
        String cosUrl,
        String bucket,
        Authenticator authenticator,
        Authenticator cosAuthenticator,
        HttpClient httpClient,
        Duration timeout,
        boolean logRequests,
        boolean logResponses,
        HttpRequestLogger requestLogger,
        Level requestLogLevel,
        HttpResponseLogger responseLogger,
        Level responseLogLevel,
        boolean verifySsl) {
        return CosStorageService.builder()
            .cosUrl(cosUrl)
            .bucket(bucket)
            .authenticator(authenticator)
            .cosAuthenticator(cosAuthenticator)
            .httpClient(httpClient)
            .timeout(timeout)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .requestLogger(requestLogger, requestLogLevel)
            .responseLogger(responseLogger, responseLogLevel)
            .verifySsl(verifySsl)
            .build();
    }

}
