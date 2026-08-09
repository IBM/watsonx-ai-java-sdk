/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.image;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.ibm.watsonx.ai.utils.HttpUtils.bodyPublisherToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.ibm.watsonx.ai.AbstractWatsonxTest;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Background;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Moderation;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.OutputFormat;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Quality;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.ResponseFormat;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Size;
import com.ibm.watsonx.ai.gateway.image.ModelGatewayImageParameters.Style;

@SuppressWarnings("unchecked")
public class ModelGatewayImageServiceTest extends AbstractWatsonxTest {

    private static final String MODEL_ID = "gpt-image-1";

    private static final String SIMPLE_RESPONSE = """
        {
            "created": 1741570283,
            "data": [
                {
                    "b64_json": "abc123",
                    "url": null,
                    "revised_prompt": null
                }
            ],
            "background": "transparent",
            "output_format": "png",
            "size": "1024x1024",
            "quality": "high",
            "usage": {
                "total_tokens": 100,
                "input_tokens": 50,
                "output_tokens": 50,
                "input_tokens_details": {
                    "text_tokens": 10,
                    "image_tokens": 40
                }
            }
        }""";

    private static final String URL_RESPONSE = """
        {
            "created": 1741570283,
            "data": [
                {
                    "url": "https://example.com/image.png",
                    "revised_prompt": "A city at sunset"
                }
            ],
            "background": "opaque",
            "output_format": "png",
            "size": "1024x1024",
            "quality": "standard",
            "usage": null
        }""";

    @BeforeEach
    void beforeEach() {
        when(mockAuthenticator.token()).thenReturn("my-super-token");
    }

    private void stubHttpResponse(String body) {
        try {
            when(mockHttpResponse.statusCode()).thenReturn(200);
            when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("application/json")), (k, v) -> true));
            when(mockHttpResponse.body()).thenReturn(body);
            when(mockSecureHttpClient.send(mockHttpRequest.capture(), any(BodyHandler.class)))
                .thenReturn(mockHttpResponse);
        } catch (Exception e) {
            fail(e);
        }
    }

    private ModelGatewayImageService buildService() {
        return ModelGatewayImageService.builder()
            .authenticator(mockAuthenticator)
            .modelId(MODEL_ID)
            .baseUrl("http://localhost")
            .build();
    }

    @Test
    void should_parse_b64_json_response() {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var response = buildService().generate("A futuristic city at sunset");
            assertNotNull(response);
            assertEquals(1741570283L, response.created());
            assertNotNull(response.data());
            assertEquals(1, response.data().size());
            assertEquals("abc123", response.data().get(0).b64Json());
            assertNull(response.data().get(0).url());
            assertNull(response.data().get(0).revisedPrompt());
            assertEquals("transparent", response.background());
            assertEquals("png", response.outputFormat());
            assertEquals("1024x1024", response.size());
            assertEquals("high", response.quality());
            assertNotNull(response.usage());
            assertEquals(100, response.usage().totalTokens());
            assertEquals(50, response.usage().inputTokens());
            assertEquals(50, response.usage().outputTokens());
            assertNotNull(response.usage().inputTokensDetails());
            assertEquals(10, response.usage().inputTokensDetails().textTokens());
            assertEquals(40, response.usage().inputTokensDetails().imageTokens());
        });
    }

    @Test
    void should_parse_url_response_with_null_usage() {
        stubHttpResponse(URL_RESPONSE);
        withWatsonxServiceMock(() -> {
            var response = buildService().generate("A city");
            assertNotNull(response);
            assertEquals("https://example.com/image.png", response.data().get(0).url());
            assertNull(response.data().get(0).b64Json());
            assertEquals("A city at sunset", response.data().get(0).revisedPrompt());
            assertNull(response.usage());
        });
    }

    @Test
    void should_handle_null_data_in_response() {
        stubHttpResponse("""
            {
                "created": 1741570283,
                "data": null,
                "background": "opaque",
                "output_format": "png",
                "size": "1024x1024",
                "quality": "standard",
                "usage": null
            }""");
        withWatsonxServiceMock(() -> {
            var response = buildService().generate("A city");
            assertNotNull(response);
            assertNull(response.data());
        });
    }

    @Test
    void should_return_immutable_data_list() {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var response = buildService().generate("A city");
            assertThrows(UnsupportedOperationException.class, () -> response.data().clear());
        });
    }

    @Test
    void should_send_model_and_prompt_in_request_body() {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            buildService().generate("A futuristic city");
            JSONAssert.assertEquals("""
                { "model": "gpt-image-1", "prompt": "A futuristic city" }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_send_all_optional_fields_when_set() {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var parameters = ModelGatewayImageParameters.builder()
                .background(Background.TRANSPARENT)
                .moderation(Moderation.LOW)
                .n(2)
                .outputCompression(80)
                .outputFormat(OutputFormat.PNG)
                .partialImages(3)
                .quality(Quality.HIGH)
                .responseFormat(ResponseFormat.B64_JSON)
                .size(Size.SIZE_1024X1024)
                .style(Style.VIVID)
                .user("user-123")
                .build();

            buildService().generate("A landscape", parameters);
            JSONAssert.assertEquals("""
                {
                    "model": "gpt-image-1",
                    "prompt": "A landscape",
                    "background": "transparent",
                    "moderation": "low",
                    "n": 2,
                    "output_compression": 80,
                    "output_format": "png",
                    "partial_images": 3,
                    "quality": "high",
                    "response_format": "b64_json",
                    "size": "1024x1024",
                    "style": "vivid",
                    "user": "user-123"
                }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_send_only_prompt_when_request_has_no_parameters() {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            buildService().generate(ModelGatewayImageRequest.builder().prompt("Test").build());
            JSONAssert.assertEquals("""
                { "model": "gpt-image-1", "prompt": "Test" }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_send_request_body_built_from_request_object() {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var request = ModelGatewayImageRequest.builder()
                .prompt("A landscape")
                .parameters(ModelGatewayImageParameters.builder().size(Size.SIZE_512X512).build())
                .build();

            buildService().generate(request);
            JSONAssert.assertEquals("""
                { "model": "gpt-image-1", "prompt": "A landscape", "size": "512x512" }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_accept_raw_string_values_for_enum_fields() {
        stubHttpResponse(SIMPLE_RESPONSE);
        withWatsonxServiceMock(() -> {
            var parameters = ModelGatewayImageParameters.builder()
                .background("opaque")
                .moderation("auto")
                .outputFormat("webp")
                .quality("standard")
                .responseFormat("url")
                .size("512x512")
                .style("natural")
                .build();

            buildService().generate("x", parameters);
            JSONAssert.assertEquals("""
                {
                    "model": "gpt-image-1",
                    "prompt": "x",
                    "background": "opaque",
                    "moderation": "auto",
                    "output_format": "webp",
                    "quality": "standard",
                    "response_format": "url",
                    "size": "512x512",
                    "style": "natural"
                }""",
                bodyPublisherToString(mockHttpRequest), true);
        });
    }

    @Test
    void should_ignore_null_enum_values() {
        var parameters = ModelGatewayImageParameters.builder()
            .background((Background) null)
            .moderation((Moderation) null)
            .outputFormat((OutputFormat) null)
            .quality((Quality) null)
            .responseFormat((ResponseFormat) null)
            .size((Size) null)
            .style((Style) null)
            .build();

        assertNull(parameters.background());
        assertNull(parameters.moderation());
        assertNull(parameters.outputFormat());
        assertNull(parameters.quality());
        assertNull(parameters.responseFormat());
        assertNull(parameters.size());
        assertNull(parameters.style());
    }

    @Test
    void should_return_correct_enum_string_values() {
        assertEquals("transparent", Background.TRANSPARENT.value());
        assertEquals("opaque", Background.OPAQUE.value());
        assertEquals("auto", Background.AUTO.value());
        assertEquals("low", Moderation.LOW.value());
        assertEquals("auto", Moderation.AUTO.value());
        assertEquals("png", OutputFormat.PNG.value());
        assertEquals("jpeg", OutputFormat.JPEG.value());
        assertEquals("webp", OutputFormat.WEBP.value());
        assertEquals("auto", OutputFormat.AUTO.value());
        assertEquals("auto", Quality.AUTO.value());
        assertEquals("high", Quality.HIGH.value());
        assertEquals("medium", Quality.MEDIUM.value());
        assertEquals("low", Quality.LOW.value());
        assertEquals("hd", Quality.HD.value());
        assertEquals("standard", Quality.STANDARD.value());
        assertEquals("url", ResponseFormat.URL.value());
        assertEquals("b64_json", ResponseFormat.B64_JSON.value());
        assertEquals("256x256", Size.SIZE_256X256.value());
        assertEquals("512x512", Size.SIZE_512X512.value());
        assertEquals("1024x1024", Size.SIZE_1024X1024.value());
        assertEquals("1536x1024", Size.SIZE_1536X1024.value());
        assertEquals("1024x1536", Size.SIZE_1024X1536.value());
        assertEquals("1792x1024", Size.SIZE_1792X1024.value());
        assertEquals("1024x1792", Size.SIZE_1024X1792.value());
        assertEquals("auto", Size.AUTO.value());
        assertEquals("vivid", Style.VIVID.value());
        assertEquals("natural", Style.NATURAL.value());
    }

    @Test
    void should_return_to_string() {
        var parameters = ModelGatewayImageParameters.builder()
            .n(1)
            .size(Size.SIZE_1024X1024)
            .quality(Quality.HIGH)
            .build();

        var request = ModelGatewayImageRequest.builder()
            .prompt("A city")
            .parameters(parameters)
            .build();

        assertTrue(request.toString().contains("prompt=A city"));
        assertTrue(request.toString().contains("ModelGatewayImageParameters ["));
        assertTrue(parameters.toString().contains("n=1"));
        assertTrue(parameters.toString().contains("size=1024x1024"));
        assertTrue(parameters.toString().contains("quality=high"));
    }

    @Test
    void should_throw_when_request_is_null() {
        withWatsonxServiceMock(() -> {
            assertThrows(NullPointerException.class, () -> buildService().generate((ModelGatewayImageRequest) null));
        });
    }

    @Test
    void should_throw_when_prompt_string_is_null() {
        withWatsonxServiceMock(() -> {
            assertThrows(NullPointerException.class, () -> buildService().generate((String) null));
        });
    }

    @Test
    void should_throw_when_prompt_in_request_is_null() {
        withWatsonxServiceMock(() -> {
            assertThrows(NullPointerException.class,
                () -> buildService().generate(ModelGatewayImageRequest.builder().build()));
        });
    }

    @Test
    void should_throw_when_prompt_is_blank() {
        withWatsonxServiceMock(() -> {
            var ex = assertThrows(IllegalArgumentException.class, () -> buildService().generate("  "));
            assertEquals("The prompt must not be blank", ex.getMessage());
        });
    }

    @Test
    void should_throw_when_model_id_is_null() {
        withWatsonxServiceMock(() -> {
            assertThrows(NullPointerException.class, () -> ModelGatewayImageService.builder()
                .authenticator(mockAuthenticator)
                .baseUrl("http://localhost")
                .build());
        });
    }

    @Test
    void should_throw_when_authenticator_is_null() {
        withWatsonxServiceMock(() -> {
            assertThrows(NullPointerException.class, () -> ModelGatewayImageService.builder()
                .modelId(MODEL_ID)
                .baseUrl("http://localhost")
                .build());
        });
    }

    @Test
    void should_wrap_io_exception_in_runtime_exception() throws Exception {

        when(mockSecureHttpClient.send(any(), any())).thenThrow(new IOException("IOException"));

        withWatsonxServiceMock(() -> {
            var service = buildService();
            var ex = assertThrows(RuntimeException.class, () -> service.generate("A city"));
            assertInstanceOf(IOException.class, ex.getCause());
        });
    }

    @Test
    void should_call_correct_endpoint() {
        wireMock.stubFor(post("/ml/gateway/v1/images/generations?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(SIMPLE_RESPONSE)));

        var response = buildWireMockService().generate("A city at sunset");
        assertNotNull(response);
        assertEquals(1741570283L, response.created());
        assertEquals("abc123", response.data().get(0).b64Json());
    }

    @Test
    void should_throw_watsonx_exception_on_gateway_client_error() {

        wireMock.stubFor(post("/ml/gateway/v1/images/generations?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": {
                            "code": "model_not_found",
                            "message": "The model `gpt-image-1` does not exist.",
                            "request_id": "req-12345"
                        }
                    }""")));

        var service = buildWireMockService();
        var ex = assertThrows(WatsonxException.class, () -> service.generate("A city"));

        assertEquals(404, ex.statusCode());
        assertNotNull(ex.details().orElse(null));
        assertEquals("req-12345", ex.details().orElse(null).trace());
        assertEquals(1, ex.details().orElse(null).errors().size());
        assertEquals("model_not_found", ex.details().orElse(null).errors().get(0).code());
        assertEquals("The model `gpt-image-1` does not exist.", ex.details().orElse(null).errors().get(0).message());
    }

    @Test
    void should_throw_watsonx_exception_on_gateway_server_error() {

        final String ERROR_BODY = """
            {
                "error": {
                    "code": "internal_server_error",
                    "message": "Upstream provider failed.",
                    "request_id": "req-500"
                }
            }""";

        wireMock.stubFor(post("/ml/gateway/v1/images/generations?version=%s".formatted(API_VERSION))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody(ERROR_BODY)));

        var service = buildWireMockService();
        var ex = assertThrows(WatsonxException.class, () -> service.generate("A city"));

        assertEquals(500, ex.statusCode());
        JSONAssert.assertEquals(ERROR_BODY, ex.getMessage(), true);
    }

    private ModelGatewayImageService buildWireMockService() {
        return ModelGatewayImageService.builder()
            .authenticator(mockAuthenticator)
            .modelId(MODEL_ID)
            .baseUrl(URI.create("http://localhost:%s".formatted(wireMock.getPort())))
            .build();
    }
}
