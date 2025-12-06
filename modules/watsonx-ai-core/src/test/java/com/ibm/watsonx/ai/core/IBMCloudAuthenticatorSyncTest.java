/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.core.utils.Utils;

@ExtendWith(MockitoExtension.class)
public class IBMCloudAuthenticatorSyncTest extends AbstractWatsonxTest {


    @Test
    void should_return_a_valid_token() throws Exception {

        var response = Utils.okResponse();

        withWatsonxServiceMock(() -> {
            try {
                when(mockHttpClient.<String>send(mockHttpRequest.capture(), any())).thenReturn(response);

                AuthenticationProvider authenticator = IBMCloudAuthenticator.builder()
                    .apiKey("my_super_api_key")
                    .build();

                assertEquals("my_super_token", authenticator.token());
                assertEquals("https://iam.cloud.ibm.com/identity/token", mockHttpRequest.getValue().uri().toString());
                assertEquals("application/x-www-form-urlencoded",
                    mockHttpRequest.getValue().headers().firstValue("Content-Type").get());
                assertEquals("grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=my_super_api_key",
                    Utils.bodyPublisherToString(mockHttpRequest));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_raise_an_exception() throws Exception {

        var response = Utils.koResponse();

        withWatsonxServiceMock(() -> {
            try {
                when(mockHttpClient.<String>send(any(), any())).thenReturn(response);

                AuthenticationProvider authenticator = IBMCloudAuthenticator.builder()
                    .apiKey("my_super_api_key")
                    .build();

                var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                assertEquals(ex.getMessage(), Utils.WRONG_RESPONSE);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_use_cached_token() throws Exception {

        var response = Utils.okResponse();

        withWatsonxServiceMock(() -> {
            try {
                when(mockHttpClient.<String>send(any(), any())).thenReturn(response);

                AuthenticationProvider authenticator = IBMCloudAuthenticator.builder()
                    .apiKey("my_super_api_key")
                    .build();

                // Execute the http request.
                authenticator.token();
                // Get the value from the cache.
                authenticator.token();

                verify(mockHttpClient, times(1)).send(any(), any());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void should_refresh_token_when_existing_token_is_expired() throws Exception {

        var expiredResponse = Utils.expiredResponse();

        withWatsonxServiceMock(() -> {
            try {
                when(mockHttpClient.<String>send(any(), any()))
                    .thenReturn(expiredResponse);

                AuthenticationProvider authenticator = IBMCloudAuthenticator.builder()
                    .apiKey("my_super_api_key")
                    .build();

                authenticator.token();

                assertEquals("my_super_token", authenticator.token());
                verify(mockHttpClient, times(2)).send(any(), any());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void test_custom_parameters() throws Exception {

        var response = Utils.okResponse();
        var httpRequest = ArgumentCaptor.forClass(HttpRequest.class);

        withWatsonxServiceMock(() -> {
            try {
                when(mockHttpClient.<String>send(httpRequest.capture(), any())).thenReturn(response);

                AuthenticationProvider authenticator = IBMCloudAuthenticator.builder()
                    .grantType("new_grant_type")
                    .timeout(Duration.ofSeconds(1))
                    .baseUrl(URI.create("http://mytest.com"))
                    .apiKey("my_super_api_key")
                    .build();

                assertEquals("my_super_token", authenticator.token());
                assertEquals("http://mytest.com/identity/token", httpRequest.getValue().uri().toString());
                assertEquals("application/x-www-form-urlencoded",
                    httpRequest.getValue().headers().firstValue("Content-Type").get());
                assertEquals("grant_type=new_grant_type&apikey=my_super_api_key", Utils.bodyPublisherToString(httpRequest));
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_error_response() throws Exception {

        var response = mock(HttpResponse.class);
        var httpRequest = ArgumentCaptor.forClass(HttpRequest.class);
        var errorMessage = """
            {
                "errorCode": "BXNIM0415E",
                "errorMessage": "Provided API key could not be found.",
                "errorDetails": "More details",
                "context": {
                    "requestId": "xxx"
                }
            }""";

        when(response.body()).thenReturn(errorMessage);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (k, v) -> true));

        withWatsonxServiceMock(() -> {
            try {
                when(mockHttpClient.<String>send(httpRequest.capture(), any())).thenReturn(response);

                AuthenticationProvider authenticator = IBMCloudAuthenticator.builder()
                    .grantType("new_grant_type")
                    .timeout(Duration.ofSeconds(1))
                    .baseUrl(URI.create("http://mytest.com"))
                    .apiKey("my_super_api_key")
                    .build();

                var ex = assertThrows(WatsonxException.class, () -> authenticator.token());
                assertEquals(400, ex.statusCode());
                assertEquals(errorMessage, ex.getMessage());
                var detail = ex.details().orElseThrow().errors().get(0);
                assertEquals("BXNIM0415E", detail.code());
                assertEquals("Provided API key could not be found.", detail.message());
                assertEquals("More details", detail.moreInfo());

            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void test_builder() throws Exception {

        var authenticator = IBMCloudAuthenticator.builder()
            .apiKey("test")
            .grantType("test")
            .timeout(Duration.ofSeconds(3))
            .baseUrl(URI.create("http://test"))
            .build();


        Field apiKey = IBMCloudAuthenticator.class.getDeclaredField("apiKey");
        apiKey.setAccessible(true);
        assertEquals("test", (String) apiKey.get(authenticator));

        Field grantType = IBMCloudAuthenticator.class.getDeclaredField("grantType");
        grantType.setAccessible(true);
        assertEquals("test", (String) grantType.get(authenticator));

        Field timeout = IBMCloudAuthenticator.class.getDeclaredField("timeout");
        timeout.setAccessible(true);
        assertEquals(Duration.ofSeconds(3), (Duration) timeout.get(authenticator));

        Field url = IBMCloudAuthenticator.class.getDeclaredField("baseUrl");
        url.setAccessible(true);
        assertEquals(URI.create("http://test"), (URI) url.get(authenticator));
    }

    @Test
    void test_exception_during_http_client() throws Exception {

        withWatsonxServiceMock(() -> {
            try {
                when(mockHttpClient.<String>send(any(), any()))
                    .thenThrow(new IOException("IOException"))
                    .thenReturn(Utils.okResponse());

                AuthenticationProvider authenticator = IBMCloudAuthenticator.builder()
                    .apiKey("my_super_api_key")
                    .build();

                var ex = assertThrows(RuntimeException.class, () -> authenticator.token());
                assertEquals(ex.getMessage(), "IOException");

                // Test lock release.
                assertEquals("my_super_token", authenticator.token());
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
