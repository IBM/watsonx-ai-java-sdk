/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.provider;

import static java.util.Objects.isNull;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Provides instances of {@link HttpClient}.
 */
public final class HttpClientProvider {

    private static volatile HttpClient secureClient;
    private static volatile HttpClient insecureClient;

    private HttpClientProvider() {}

    /**
     * Returns an HttpClient instance with configurable SSL verification.
     *
     * @param verifySsl if {@code true}, SSL certificates are validated; if {@code false}, all certificates are accepted (insecure)
     * @return HttpClient instance
     */
    public static HttpClient httpClient(boolean verifySsl) {
        return verifySsl ? secureClient() : insecureClient();
    }

    /**
     * Returns the singleton secure {@link HttpClient} instance that validates SSL/TLS certificates.
     *
     * @return the singleton secure {@link HttpClient} instance
     */
    private static synchronized HttpClient secureClient() {
        if (isNull(secureClient)) {
            secureClient = HttpClient.newBuilder()
                .executor(ExecutorProvider.ioExecutor())
                .build();
        }
        return secureClient;
    }

    /**
     * Returns the singleton insecure {@link HttpClient} instance that accepts all SSL/TLS certificates.
     *
     * @return the singleton insecure {@link HttpClient} instance
     */
    private static synchronized HttpClient insecureClient() {
        if (isNull(insecureClient)) {
            insecureClient = HttpClient.newBuilder()
                .executor(ExecutorProvider.ioExecutor())
                .sslContext(createInsecureSslContext())
                .build();
        }
        return insecureClient;
    }

    /**
     * Creates an {@link SSLContext} that trusts all SSL/TLS certificates without validation.
     *
     * @return an {@link SSLContext} configured to accept all certificates
     */
    private static SSLContext createInsecureSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create insecure SSL context", e);
        }
    }
}