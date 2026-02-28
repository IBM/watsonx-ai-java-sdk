/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ibm.watsonx.ai.core.http.MultipartBody;

@ExtendWith(MockitoExtension.class)
public class MultipartBodyTest {

    @Test
    void should_build_multipart_body_with_parts_and_input_stream() {

        MultipartBody multipartBody = MultipartBody.builder()
            .addPart("model", "openai/whisper-tiny")
            .addPart("language", "it")
            .addInputStream("file", "audio.mp3", new ByteArrayInputStream("test".getBytes()))
            .build();

        assertEquals("multipart/form-data; boundary=----watsonx-ai-sdk", multipartBody.contentType());
        assertNotNull(multipartBody.body());
    }

    @Test
    void should_throw_null_pointer_exception_when_adding_part_with_null_name_or_value() {

        var ex = assertThrows(NullPointerException.class, () -> MultipartBody.builder().addPart(null, "test").build());
        assertEquals("The name must be provided", ex.getMessage());

        ex = assertThrows(NullPointerException.class, () -> MultipartBody.builder().addPart("model", null).build());
        assertEquals("The value must be provided", ex.getMessage());
    }

    @Test
    void should_throw_null_pointer_exception_when_adding_input_stream_with_null_arguments() {

        var ex = assertThrows(NullPointerException.class,
            () -> MultipartBody.builder().addInputStream(null, "audio.mp3", new ByteArrayInputStream("test".getBytes())).build());
        assertEquals("The name must be provided", ex.getMessage());

        ex = assertThrows(NullPointerException.class,
            () -> MultipartBody.builder().addInputStream("file", null, new ByteArrayInputStream("test".getBytes())).build());
        assertEquals("The fileName must be provided", ex.getMessage());

        ex = assertThrows(NullPointerException.class,
            () -> MultipartBody.builder().addInputStream("file", "audio.mp3", null).build());
        assertEquals("The inputstream must be provided", ex.getMessage());
    }

    @Test
    void should_throw_illegal_state_exception_when_building_with_no_parts() {

        var ex = assertThrows(IllegalStateException.class,
            () -> MultipartBody.builder().build());
        assertEquals("Cannot build multipart body with no parts", ex.getMessage());
    }

    @Test
    void should_throw_unchecked_io_exception_when_input_stream_fails() throws Exception {

        var mockInputStream = mock(InputStream.class);
        when(mockInputStream.readAllBytes()).thenThrow(new IOException("ex"));

        var ex = assertThrows(UncheckedIOException.class,
            () -> MultipartBody.builder()
                .addPart("model", "openai/whisper-tiny")
                .addPart("language", "it")
                .addInputStream("file", "audio.mp3", mockInputStream)
                .build());

        assertEquals("ex", ex.getCause().getMessage());
    }
}
