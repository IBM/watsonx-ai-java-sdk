/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.storage.cos.internal;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.event.Level;
import com.ibm.watsonx.ai.core.auth.Authenticator;
import com.ibm.watsonx.ai.core.http.logging.HttpRequestLogger;
import com.ibm.watsonx.ai.core.http.logging.HttpResponseLogger;
import com.ibm.watsonx.ai.textprocessing.storage.cos.CosStorageRestClient;

/**
 * Internal cache of lazily initialized COS upload clients, sharing one configuration across buckets.
 */
public final class CosUploadClients {

    private final Map<String, CosStorageRestClient> clients = new HashMap<>();
    private final Function<String, CosStorageRestClient> clientFactory;

    /**
     * Captures upload configuration without loading the COS client SPI or validating storage settings before the first upload.
     *
     * @param cosUrl the COS endpoint
     * @param authenticator the service authenticator
     * @param cosAuthenticator the COS authenticator
     * @param httpClient the shared HTTP client
     * @param timeout the upload timeout
     * @param logRequests whether to log requests
     * @param logResponses whether to log responses
     * @param requestLogger the optional request logger
     * @param requestLogLevel the request logging level
     * @param responseLogger the optional response logger
     * @param responseLogLevel the response logging level
     */
    public CosUploadClients(String cosUrl, Authenticator authenticator, Authenticator cosAuthenticator, HttpClient httpClient, Duration timeout,
        boolean logRequests, boolean logResponses, HttpRequestLogger requestLogger, Level requestLogLevel, HttpResponseLogger responseLogger,
        Level responseLogLevel) {
        clientFactory = bucket -> CosStorageRestClient.builder()
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
            .build();
    }

    /**
     * Returns the upload client for a bucket, creating it once on first use.
     *
     * @param bucket the destination bucket
     * @return the configured COS client
     */
    public synchronized CosStorageRestClient get(String bucket) {
        return clients.computeIfAbsent(bucket, clientFactory);
    }
}
