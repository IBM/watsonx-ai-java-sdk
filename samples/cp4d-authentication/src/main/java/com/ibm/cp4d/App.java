/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.cp4d;

import java.net.URI;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import com.ibm.watsonx.ai.chat.ChatService;
import com.ibm.watsonx.ai.core.auth.cp4d.CP4DAuthenticator;
import com.ibm.watsonx.ai.core.provider.ExecutorProvider;

public class App {

    private static final Config config = ConfigProvider.getConfig();

    public static void main(String[] args) throws Exception {

        final var baseUrl = URI.create(config.getValue("CP4D_URL", String.class));
        final var username = config.getValue("CP4D_USERNAME", String.class);
        final var apiKey = config.getValue("CP4D_API_KEY", String.class);
        final var projectId = config.getValue("CP4D_PROJECT_ID", String.class);

        HttpClient httpClient = HttpClient.newBuilder()
            .sslContext(createTrustAllSSLContext())
            .executor(ExecutorProvider.ioExecutor())
            .build();

        ChatService chatService = ChatService.builder()
            .baseUrl(baseUrl)
            .modelId("ibm/granite-3-2-8b-instruct")
            .projectId(projectId)
            .httpClient(httpClient)
            .authenticator(
                CP4DAuthenticator.builder()
                    .baseUrl(baseUrl)
                    .username(username)
                    .apiKey(apiKey)
                    .httpClient(httpClient)
                    .build()
            ).build();

        var assistantMessage = chatService.chat("How are you?").toAssistantMessage();
        System.out.println("USER: ".concat("How are you?"));
        System.out.println("ASSISTANT: ".concat(assistantMessage.content()));
    }

    private static SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext;
    }
}
