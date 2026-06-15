/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.it;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.ibm.watsonx.ai.core.auth.ibmcloud.IBMCloudAuthenticator;
import com.ibm.watsonx.ai.core.exception.WatsonxException;
import com.ibm.watsonx.ai.textprocessing.Language;
import com.ibm.watsonx.ai.textprocessing.Mode;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaResponse.CreateSchemaResult;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaService;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters.KvpMode;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;

@EnabledIfEnvironmentVariable(named = "WATSONX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_PROJECT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "WATSONX_DOCUMENT_REFERENCE_BUCKET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "CLOUD_OBJECT_STORAGE_URL", matches = ".+")
public class CreateSchemaIT {

    static final String API_KEY = System.getenv("WATSONX_API_KEY");
    static final String COS_API_KEY = System.getenv("CLOUD_OBJECT_STORAGE_API_KEY");
    static final String PROJECT_ID = System.getenv("WATSONX_PROJECT_ID");
    static final String URL = System.getenv("WATSONX_URL");
    static final String DOCUMENT_REFERENCE_CONNECTION_ID = System.getenv("WATSONX_DOCUMENT_REFERENCE_CONNECTION_ID");
    static final String DOCUMENT_REFERENCE_BUCKET = System.getenv("WATSONX_DOCUMENT_REFERENCE_BUCKET");
    static final String CLOUD_OBJECT_STORAGE_URL = System.getenv("CLOUD_OBJECT_STORAGE_URL");

    static final CreateSchemaService createSchemaService = CreateSchemaService.builder()
        .baseUrl(URL)
        .apiKey(API_KEY)
        .projectId(PROJECT_ID)
        .cosUrl(CLOUD_OBJECT_STORAGE_URL)
        .cosAuthenticator(nonNull(COS_API_KEY) ? IBMCloudAuthenticator.withKey(COS_API_KEY) : IBMCloudAuthenticator.withKey(API_KEY))
        .documentReference(DOCUMENT_REFERENCE_CONNECTION_ID, DOCUMENT_REFERENCE_BUCKET)
        .timeout(Duration.ofMinutes(5))
        .logRequests(true)
        .logResponses(true)
        .build();

    @Test
    void should_use_the_auto_generated_schema_in_text_extraction() throws Exception {

        TextExtractionService textExtractionService = TextExtractionService.builder()
            .apiKey(API_KEY)
            .projectId(PROJECT_ID)
            .baseUrl(URL)
            .cosUrl(CLOUD_OBJECT_STORAGE_URL)
            .documentReference(DOCUMENT_REFERENCE_CONNECTION_ID, DOCUMENT_REFERENCE_BUCKET)
            .resultReference(DOCUMENT_REFERENCE_CONNECTION_ID, DOCUMENT_REFERENCE_BUCKET)
            .logRequests(true)
            .logResponses(true)
            .build();

        CreateSchemaService createSchemaService = CreateSchemaService.builder()
            .apiKey(API_KEY)
            .projectId(PROJECT_ID)
            .baseUrl(URL)
            .cosUrl(CLOUD_OBJECT_STORAGE_URL)
            .documentReference(DOCUMENT_REFERENCE_CONNECTION_ID, DOCUMENT_REFERENCE_BUCKET)
            .logRequests(true)
            .logResponses(true)
            .build();

        var file = Path.of(ClassLoader.getSystemResource("invoice.pdf").toURI()).toFile();

        CreateSchemaResult result = createSchemaService.uploadCreateSchemaAndFetch(
            file,
            CreateSchemaParameters.builder()
                .languages(Language.ENGLISH)
                .mode(Mode.HIGH_QUALITY)
                .timeout(Duration.ofMinutes(10))
                .removeUploadedFile(true)
                .build()
        );

        var textExtractionParameters = TextExtractionParameters.builder()
            .languages(Language.ENGLISH)
            .mode(Mode.HIGH_QUALITY)
            .timeout(Duration.ofMinutes(10))
            .removeUploadedFile(true)
            .removeOutputFile(true)
            .kvpMode(KvpMode.GENERIC_WITH_SEMANTIC)
            .semanticConfig(
                TextExtractionSemanticConfig.builder()
                    .schemas(result.schema())
                    .build()
            ).build();

        var extraction = textExtractionService.uploadExtractAndFetch(file, textExtractionParameters);
        assertTrue(extraction.startsWith("## CPB SOFTWARE (GERMANY) GMBH"));
    }

    @Test
    void should_upload_file_and_complete_creation_schema_successfully() throws Exception {

        var file = Path.of(ClassLoader.getSystemResource("invoice.pdf").toURI()).toFile();

        var parameters = CreateSchemaParameters.builder()
            .languages(Language.ENGLISH)
            .mode(Mode.HIGH_QUALITY)
            .enableGrounding(true)
            .additionalPromptInstructions("The document description must be at max 20 words")
            .build();

        var response = createSchemaService.uploadCreateSchemaAndFetch(file, parameters);
        assertNotNull(response.completedAt());
        assertNull(response.error());
        assertNotNull(response.groundingHints());
        assertTrue(response.groundingHints().fieldNames().size() > 0);

        var fieldData = response.groundingHints().field(response.groundingHints().fieldNames().iterator().next());
        assertNotNull(fieldData);
        assertNotNull(fieldData.normalizedBbox());
        assertNotNull(fieldData.pageNumber());

        assertNotNull(response.numberPagesProcessed());
        assertNotNull(response.runningAt());
        assertNotNull(response.schema());
        assertNull(response.schema().additionalPromptInstructions());
        assertNotNull(response.schema().documentDescription());
        assertNotNull(response.schema().documentType());
        assertNull(response.schema().pages());
        assertTrue(response.schema().fields().size() > 0);

        var field = response.schema().fields().get(response.schema().fields().keySet().iterator().next());
        assertNotNull(field);
        assertNull(field.availableOptions());
        assertNotNull(field.description());
        assertNotNull(field.example());

        assertNotNull(response.status());
        assertNotNull(response.totalPages());

        assertTrue(createSchemaService.deleteFile(DOCUMENT_REFERENCE_BUCKET, "invoice.pdf"));
    }

    @Test
    void should_upload_file_and_fetch_create_schema_from_inputstream() throws Exception {

        var filename = "invoice.pdf";
        var inputstream = ClassLoader.getSystemResourceAsStream(filename);

        var result = createSchemaService.uploadCreateSchemaAndFetch(inputstream, filename);
        assertEquals("Invoice", result.schema().documentType());
        assertTrue(createSchemaService.deleteFile(DOCUMENT_REFERENCE_BUCKET, filename));

        var parameters = CreateSchemaParameters.builder()
            .removeUploadedFile(true)
            .build();

        inputstream = ClassLoader.getSystemResourceAsStream(filename);
        result = createSchemaService.uploadCreateSchemaAndFetch(inputstream, filename, parameters);
        assertEquals("Invoice", result.schema().documentType());

        // Wait for async deletion
        Thread.sleep(500);
    }

    @Test
    void should_delete_create_schema_request() throws Exception {

        var file = Path.of(ClassLoader.getSystemResource("invoice.pdf").toURI()).toFile();

        var parameters = CreateSchemaParameters.builder()
            .languages(Language.ENGLISH)
            .build();

        var response = createSchemaService.uploadAndStartCreateSchema(file, parameters);
        assertTrue(
            createSchemaService.deleteRequest(
                response.metadata().id(),
                CreateSchemaDeleteParameters.builder()
                    .hardDelete(true)
                    .build()
            )
        );

        var ex = assertThrows(WatsonxException.class, () -> createSchemaService.fetchRequest(response.metadata().id()));
        assertEquals(404, ex.statusCode());
    }

    @Test
    void should_delete_a_create_schema_job_that_does_not_exist() {
        assertFalse(createSchemaService.deleteRequest("non-existing-id"));
    }
}
