/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.provider;

import static java.util.Objects.isNull;
import java.net.http.HttpClient;

/**
 * Provides a instance of {@link HttpClient}.
 */
public final class HttpClientProvider {

    private static volatile HttpClient httpClient;

    private HttpClientProvider() {}

    /**
     * Returns HttpClient instance.
     *
     * @return HttpClient instance
     */
    public static synchronized HttpClient httpClient() {
        if (isNull(httpClient)) {
            httpClient = HttpClient.newBuilder()
                .executor(ExecutorProvider.ioExecutor())
                .build();
        }
        return httpClient;
    }
}
