/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.AuthenticationProvider;
import com.ibm.watsonx.ai.core.auth.iam.IAMAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.textprocessing.Language;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters.Mode;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters.Type;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;


@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_DOCUMENT_REFERENCE_BUCKET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_RESULTS_REFERENCE_CONNECTION_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_RESULTS_REFERENCE_BUCKET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "CLOUD_OBJECT_STORAGE_URL", matches = ".+")
public class TextExtractionServiceIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");
    static final String DOCUMENT_REFERENCE_CONNECTION_ID = System.getenv("WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID");
    static final String DOCUMENT_REFERENCE_BUCKET = System.getenv("WATSONX_DOCUMENT_REFERENCE_BUCKET");
    static final String RESULTS_REFERENCE_CONNECTION_ID = System.getenv("WATSONX_RESULTS_REFERENCE_CONNECTION_ID");
    static final String RESULTS_REFERENCE_BUCKET = System.getenv("WATSONX_RESULTS_REFERENCE_BUCKET");
    static final String CLOUD_OBJECT_STORAGE_URL = System.getenv("CLOUD_OBJECT_STORAGE_URL");

    static final AuthenticationProvider authentication = IAMAuthenticator.builder()
        .apiKey(API_KEY)
        .build();

    static final TextExtractionService textExtractionService = TextExtractionService.builder()
        .baseUrl(URL)
        .cosUrl(CLOUD_OBJECT_STORAGE_URL)
        .projectId(PROJECT_ID)
        .documentReference(DOCUMENT_REFERENCE_CONNECTION_ID, DOCUMENT_REFERENCE_BUCKET)
        .resultReference(RESULTS_REFERENCE_CONNECTION_ID, RESULTS_REFERENCE_BUCKET)
        .authenticationProvider(authentication)
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void test_upload_and_start_extraction_with_file() throws Exception {

        var file = Path.of(ClassLoader.getSystemResource("test.pdf").toURI()).toFile();

        var parameters = TextExtractionParameters.builder()
            .languages(Language.ENGLISH)
            .build();

        var response = textExtractionService.uploadAndStartExtraction(file, parameters);
        assertNotNull(response.entity());
        assertNull(response.entity().custom());
        assertNotNull(response.entity().documentReference().connection());
        assertNotNull(response.entity().documentReference().connection().id());
        assertNotNull(response.entity().resultsReference().connection());
        assertNotNull(response.entity().resultsReference().connection().id());
        assertNotNull(response.entity().parameters());
        assertNotNull(response.entity().parameters().languages());
        assertTrue(response.entity().parameters().languages().size() == 1);
        assertNotNull(response.entity().results());
        assertNotNull(response.entity().results().status());
        assertNotNull(response.entity().results().numberPagesProcessed());
        assertNotNull(response.metadata().id());
        assertNotNull(response.metadata().createdAt());
        assertNotNull(response.metadata().projectId());

        var status = response.entity().results().status();
        while (!status.equals("failed") && !status.equals("completed")) {
            Thread.sleep(2000);
            response = textExtractionService.fetchExtractionRequest(response.metadata().id());
            status = response.entity().results().status();
        }

        assertEquals("completed", status);
        assertNotNull(response.entity());
        assertNull(response.entity().custom());
        assertNotNull(response.entity().documentReference().connection());
        assertNotNull(response.entity().documentReference().connection().id());
        assertNotNull(response.entity().resultsReference().connection());
        assertNotNull(response.entity().resultsReference().connection().id());
        assertNotNull(response.entity().parameters());
        assertNotNull(response.entity().parameters().languages());
        assertTrue(response.entity().parameters().languages().size() == 1);
        assertNotNull(response.entity().results());
        assertNotNull(response.entity().results().completedAt());
        assertNotNull(response.entity().results().location());
        assertNotNull(response.entity().results().runningAt());
        assertNotNull(response.entity().results().status());
        assertNotNull(response.entity().results().numberPagesProcessed());
        assertNotNull(response.metadata().id());
        assertNotNull(response.metadata().createdAt());
        assertNotNull(response.metadata().modifiedAt());
        assertNotNull(response.metadata().projectId());

        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test.pdf"));
    }


    @Test
    void test_upload_extract_and_fetch_with_file() throws Exception {

        var file = Path.of(ClassLoader.getSystemResource("test.pdf").toURI()).toFile();

        var parameters = TextExtractionParameters.builder()
            .languages(Language.ENGLISH)
            .build();

        var text = textExtractionService.uploadExtractAndFetch(file, parameters);
        assertEquals("PDF TEST", text);
        assertEquals("PDF TEST", textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test.pdf"));
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.pdf"));

        parameters = TextExtractionParameters.builder()
            .mode(Mode.HIGH_QUALITY)
            .removeUploadedFile(true)
            .removeOutputFile(true)
            .build();

        text = textExtractionService.uploadExtractAndFetch(file, parameters);
        assertEquals("PDF TEST", text);

        // Wait for async deletion
        Thread.sleep(500);
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.pdf"));
    }

    @Test
    void test_delete_request() throws Exception {

        var file = Path.of(ClassLoader.getSystemResource("test.pdf").toURI()).toFile();

        var parameters = TextExtractionParameters.builder()
            .languages(Language.ENGLISH)
            .build();

        var response = textExtractionService.uploadAndStartExtraction(file, parameters);
        assertTrue(
            textExtractionService.deleteRequest(
                response.metadata().id(),
                TextExtractionDeleteParameters.builder()
                    .hardDelete(true)
                    .build()
            )
        );

        var ex = assertThrows(WatsonxException.class, () -> textExtractionService.fetchExtractionRequest(response.metadata().id()));
        assertEquals(404, ex.statusCode());
    }

    @Test
    void test_upload_extract_and_fetch_with_inputstream() throws Exception {

        var filename = "test.pdf";
        var inputstream = ClassLoader.getSystemResourceAsStream(filename);

        var parameters = TextExtractionParameters.builder()
            .languages(Language.ENGLISH)
            .build();

        var text = textExtractionService.uploadExtractAndFetch(inputstream, filename);
        assertEquals("PDF TEST", text);
        assertEquals("PDF TEST", textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test.pdf"));
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.pdf"));

        parameters = TextExtractionParameters.builder()
            .mode(Mode.HIGH_QUALITY)
            .removeUploadedFile(true)
            .removeOutputFile(true)
            .build();

        inputstream = ClassLoader.getSystemResourceAsStream(filename);
        text = textExtractionService.uploadExtractAndFetch(inputstream, filename, parameters);
        assertEquals("PDF TEST", text);

        // Wait for async deletion
        Thread.sleep(500);
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.md"));
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test.pdf"));
    }

    @Test
    void test_multiple_outputs() throws Exception {

        var file = Path.of(ClassLoader.getSystemResource("test.pdf").toURI()).toFile();

        var parameters = TextExtractionParameters.builder()
            .requestedOutputs(Type.PLAIN_TEXT, Type.JSON, Type.HTML)
            .outputFileName("test/")
            .build();

        assertTrue(textExtractionService.uploadFile(file));
        var response = textExtractionService.startExtraction("test.pdf", parameters);
        var status = response.entity().results().status();
        while (!status.equals("failed") && !status.equals("completed")) {
            Thread.sleep(2000);
            response = textExtractionService.fetchExtractionRequest(response.metadata().id());
            status = response.entity().results().status();
        }

        assertEquals("completed", status);
        assertTrue(textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test/assembly.html").contains("PDF TEST"));
        assertTrue(textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test/assembly.json").contains("PDF TEST"));
        assertEquals("PDF TEST", textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test/plain.txt"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test/assembly.html"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test/assembly.json"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test/plain.txt"));
        assertTrue(textExtractionService.deleteFile(RESULTS_REFERENCE_BUCKET, "test.pdf"));
    }

    @Test
    void test_ocr() throws Exception {

        var file = Path.of(ClassLoader.getSystemResource("ocr.jpg").toURI()).toFile();
        var parameters = TextExtractionParameters.builder()
            .mode(Mode.HIGH_QUALITY)
            .requestedOutputs(Type.PLAIN_TEXT)
            .outputFileName("test_ocr.txt")
            .removeUploadedFile(true)
            .removeOutputFile(true)
            .build();

        var text = textExtractionService.uploadExtractAndFetch(file, parameters);
        assertEquals("OCR TEST", text);

        // Wait for async deletion
        Thread.sleep(500);
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "ocr.jpg"));
        assertThrows(WatsonxException.class, () -> textExtractionService.readFile(RESULTS_REFERENCE_BUCKET, "test_ocr.txt"));
    }
}
