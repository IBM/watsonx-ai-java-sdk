/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import com.ibm.watsonx.ai.batch.BatchCreateRequest;
import com.ibm.watsonx.ai.batch.BatchService;
import com.ibm.watsonx.ai.file.FileService;
import com.ibm.watsonx.ai.core.Polling;
import com.ibm.watsonx.ai.textprocessing.CosReference;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.cluster.ClusterSchemaException;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaException;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaException;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaService;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaException;
import com.ibm.watsonx.ai.textprocessing.Schema;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationException;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationParameters;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationService;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionException;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionService;

/**
 * Verifies service job polling, request deadlines, cancellation and interruption handling.
 */
@DisabledInNativeImage
class ServicePollingTest extends AbstractWatsonxTest {

    enum JobType {
        CLASSIFICATION,
        EXTRACTION,
        BATCH,
        CREATE,
        IMPROVE,
        MERGE,
        CLUSTER
    }

    private final AtomicInteger cancellations = new AtomicInteger();

    @Test
    void should_cancel_without_polling_after_deadline_for_classification() throws Exception {
        should_cancel_without_polling_after_deadline(JobType.CLASSIFICATION);
    }

    @Test
    void should_cancel_without_polling_after_deadline_for_extraction() throws Exception {
        should_cancel_without_polling_after_deadline(JobType.EXTRACTION);
    }

    @Test
    void should_cancel_without_polling_after_deadline_for_batch() throws Exception {
        should_cancel_without_polling_after_deadline(JobType.BATCH);
    }

    @Test
    void should_bound_poll_requests_and_reject_late_success_for_classification() throws Exception {
        should_bound_poll_requests_and_reject_late_success(JobType.CLASSIFICATION);
    }

    @Test
    void should_bound_poll_requests_and_reject_late_success_for_extraction() throws Exception {
        should_bound_poll_requests_and_reject_late_success(JobType.EXTRACTION);
    }

    @Test
    void should_bound_poll_requests_and_reject_late_success_for_batch() throws Exception {
        should_bound_poll_requests_and_reject_late_success(JobType.BATCH);
    }

    @Test
    void should_propagate_transaction_id_to_polling_for_classification() throws Exception {
        should_propagate_transaction_id_to_polling(JobType.CLASSIFICATION);
    }

    @Test
    void should_propagate_transaction_id_to_polling_for_extraction() throws Exception {
        should_propagate_transaction_id_to_polling(JobType.EXTRACTION);
    }

    @Test
    void should_propagate_transaction_id_to_polling_for_batch() throws Exception {
        should_propagate_transaction_id_to_polling(JobType.BATCH);
    }

    @Test
    void should_preserve_http_interrupt_and_cancel_job_for_classification() throws Exception {
        should_preserve_http_interrupt_and_cancel_job(JobType.CLASSIFICATION);
    }

    @Test
    void should_preserve_http_interrupt_and_cancel_job_for_extraction() throws Exception {
        should_preserve_http_interrupt_and_cancel_job(JobType.EXTRACTION);
    }

    @Test
    void should_preserve_http_interrupt_and_cancel_job_for_batch() throws Exception {
        should_preserve_http_interrupt_and_cancel_job(JobType.BATCH);
    }

    @Test
    void should_cancel_without_polling_after_deadline_for_schemas_create() throws Exception {
        should_cancel_without_polling_after_deadline_for_schemas(JobType.CREATE);
    }

    @Test
    void should_cancel_without_polling_after_deadline_for_schemas_improve() throws Exception {
        should_cancel_without_polling_after_deadline_for_schemas(JobType.IMPROVE);
    }

    @Test
    void should_cancel_without_polling_after_deadline_for_schemas_merge() throws Exception {
        should_cancel_without_polling_after_deadline_for_schemas(JobType.MERGE);
    }

    @Test
    void should_cancel_without_polling_after_deadline_for_schemas_cluster() throws Exception {
        should_cancel_without_polling_after_deadline_for_schemas(JobType.CLUSTER);
    }

    private void should_cancel_without_polling_after_deadline_for_schemas(JobType type) throws Exception {
        should_cancel_without_polling_after_deadline(type);
    }

    @Test
    void should_bound_poll_requests_and_reject_late_success_for_schemas_create() throws Exception {
        should_bound_poll_requests_and_reject_late_success_for_schemas(JobType.CREATE);
    }

    @Test
    void should_bound_poll_requests_and_reject_late_success_for_schemas_improve() throws Exception {
        should_bound_poll_requests_and_reject_late_success_for_schemas(JobType.IMPROVE);
    }

    @Test
    void should_bound_poll_requests_and_reject_late_success_for_schemas_merge() throws Exception {
        should_bound_poll_requests_and_reject_late_success_for_schemas(JobType.MERGE);
    }

    @Test
    void should_bound_poll_requests_and_reject_late_success_for_schemas_cluster() throws Exception {
        should_bound_poll_requests_and_reject_late_success_for_schemas(JobType.CLUSTER);
    }

    private void should_bound_poll_requests_and_reject_late_success_for_schemas(JobType type) throws Exception {
        should_bound_poll_requests_and_reject_late_success(type);
    }

    @Test
    void should_propagate_transaction_id_to_polling_for_schemas_create() throws Exception {
        should_propagate_transaction_id_to_polling_for_schemas(JobType.CREATE);
    }

    @Test
    void should_propagate_transaction_id_to_polling_for_schemas_improve() throws Exception {
        should_propagate_transaction_id_to_polling_for_schemas(JobType.IMPROVE);
    }

    @Test
    void should_propagate_transaction_id_to_polling_for_schemas_merge() throws Exception {
        should_propagate_transaction_id_to_polling_for_schemas(JobType.MERGE);
    }

    @Test
    void should_propagate_transaction_id_to_polling_for_schemas_cluster() throws Exception {
        should_propagate_transaction_id_to_polling_for_schemas(JobType.CLUSTER);
    }

    private void should_propagate_transaction_id_to_polling_for_schemas(JobType type) throws Exception {
        should_propagate_transaction_id_to_polling(type);
    }

    @Test
    void should_preserve_http_interrupt_and_cancel_job_for_schemas_create() throws Exception {
        should_preserve_http_interrupt_and_cancel_job_for_schemas(JobType.CREATE);
    }

    @Test
    void should_preserve_http_interrupt_and_cancel_job_for_schemas_improve() throws Exception {
        should_preserve_http_interrupt_and_cancel_job_for_schemas(JobType.IMPROVE);
    }

    @Test
    void should_preserve_http_interrupt_and_cancel_job_for_schemas_merge() throws Exception {
        should_preserve_http_interrupt_and_cancel_job_for_schemas(JobType.MERGE);
    }

    @Test
    void should_preserve_http_interrupt_and_cancel_job_for_schemas_cluster() throws Exception {
        should_preserve_http_interrupt_and_cancel_job_for_schemas(JobType.CLUSTER);
    }

    private void should_preserve_http_interrupt_and_cancel_job_for_schemas(JobType type) throws Exception {
        should_preserve_http_interrupt_and_cancel_job(type);
    }

    @Test
    void should_preserve_timeout_when_schema_cancellation_fails_create() throws Exception {
        should_preserve_timeout_when_schema_cancellation_fails(JobType.CREATE);
    }

    @Test
    void should_preserve_timeout_when_schema_cancellation_fails_improve() throws Exception {
        should_preserve_timeout_when_schema_cancellation_fails(JobType.IMPROVE);
    }

    @Test
    void should_preserve_timeout_when_schema_cancellation_fails_merge() throws Exception {
        should_preserve_timeout_when_schema_cancellation_fails(JobType.MERGE);
    }

    @Test
    void should_preserve_timeout_when_schema_cancellation_fails_cluster() throws Exception {
        should_preserve_timeout_when_schema_cancellation_fails(JobType.CLUSTER);
    }

    private void should_preserve_timeout_when_schema_cancellation_fails(JobType type) throws Exception {
        var nanos = new AtomicLong();
        try (var polling = mockStatic(Polling.class)) {
            polling.when(() -> Polling.start(any(), any())).thenAnswer(invocation -> new Polling<>(
                invocation.getArgument(0), invocation.getArgument(1), nanos::get, nanos::addAndGet));
            mockJob(type, request -> {
                fail("A poll must not start after the deadline");
                return null;
            });
            doAnswer(invocation -> {
                HttpRequest request = invocation.getArgument(0);
                if (request.method().equals("DELETE"))
                    throw new IllegalStateException("Cancellation failed");
                return response(jobResponse(type, false));
            }).when(mockSecureHttpClient).send(any(), any());

            var error = assertThrows(Exception.class, () -> createJob(type).execute(Duration.ofMillis(20)));

            assertTimeout(type, error);
            assertEquals("Cancellation failed", error.getSuppressed()[0].getMessage());
        }
    }

    @Test
    void should_return_initial_completed_document_job_without_polling_classification() throws Exception {
        should_return_initial_completed_document_job_without_polling(JobType.CLASSIFICATION);
    }

    @Test
    void should_return_initial_completed_document_job_without_polling_extraction() throws Exception {
        should_return_initial_completed_document_job_without_polling(JobType.EXTRACTION);
    }

    @Test
    void should_return_initial_completed_document_job_without_polling_create() throws Exception {
        should_return_initial_completed_document_job_without_polling(JobType.CREATE);
    }

    @Test
    void should_return_initial_completed_document_job_without_polling_improve() throws Exception {
        should_return_initial_completed_document_job_without_polling(JobType.IMPROVE);
    }

    @Test
    void should_return_initial_completed_document_job_without_polling_merge() throws Exception {
        should_return_initial_completed_document_job_without_polling(JobType.MERGE);
    }

    @Test
    void should_return_initial_completed_document_job_without_polling_cluster() throws Exception {
        should_return_initial_completed_document_job_without_polling(JobType.CLUSTER);
    }

    private void should_return_initial_completed_document_job_without_polling(JobType type) throws Exception {
        when(mockAuthenticator.token()).thenReturn("token");
        when(mockSecureHttpClient.send(any(), any())).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            if (request.method().equals("POST"))
                return response(jobResponse(type, true));
            assertFalse(request.uri().getPath().endsWith("/job"), "A completed job must not be polled");
            return response("");
        });

        createJob(type).execute(Duration.ofSeconds(5));
    }

    private void should_cancel_without_polling_after_deadline(JobType type) throws Exception {
        var nanos = new AtomicLong();
        try (var polling = mockStatic(Polling.class)) {
            polling.when(() -> Polling.start(any(), any())).thenAnswer(invocation -> new Polling<>(
                invocation.getArgument(0), invocation.getArgument(1), nanos::get, nanos::addAndGet));
            mockJob(type, request -> {
                fail("A poll must not start after the deadline");
                return null;
            });
            var job = createJob(type);

            assertTimeout(type, assertThrows(Exception.class, () -> job.execute(Duration.ofMillis(20))));

            assertEquals(1, cancellations.get());
        }
    }

    private void should_bound_poll_requests_and_reject_late_success(JobType type) throws Exception {
        var nanos = new AtomicLong();
        try (var polling = mockStatic(Polling.class)) {
            polling.when(() -> Polling.start(any(), any())).thenAnswer(invocation -> new Polling<>(
                invocation.getArgument(0), invocation.getArgument(1), nanos::get, nanos::addAndGet));
            var poll = new AtomicReference<HttpRequest>();
            var budget = Duration.ofSeconds(1);
            mockJob(type, request -> {
                poll.set(request);
                nanos.addAndGet(budget.plusMillis(100).toNanos());
                return response(jobResponse(type, true));
            });
            var job = createJob(type);

            assertTimeout(type, assertThrows(Exception.class, () -> job.execute(budget)));

            assertNotNull(poll.get());
            assertTrue(poll.get().timeout().orElseThrow().compareTo(budget) < 0);
            assertEquals(1, cancellations.get());
        }
    }

    private void should_propagate_transaction_id_to_polling(JobType type) throws Exception {
        var nanos = new AtomicLong();
        try (var polling = mockStatic(Polling.class)) {
            polling.when(() -> Polling.start(any(), any())).thenAnswer(invocation -> new Polling<>(
                invocation.getArgument(0), invocation.getArgument(1), nanos::get, nanos::addAndGet));
            var poll = new AtomicReference<HttpRequest>();
            mockJob(type, request -> {
                poll.set(request);
                return response(jobResponse(type, true));
            });

            createJob(type).execute(Duration.ofSeconds(5));

            assertEquals("transaction", poll.get().headers().firstValue(TRANSACTION_ID_HEADER).orElseThrow());
            assertEquals(0, cancellations.get());
        }
    }

    private void should_preserve_http_interrupt_and_cancel_job(JobType type) throws Exception {
        var nanos = new AtomicLong();
        try (var polling = mockStatic(Polling.class)) {
            polling.when(() -> Polling.start(any(), any())).thenAnswer(invocation -> new Polling<>(
                invocation.getArgument(0), invocation.getArgument(1), nanos::get, nanos::addAndGet));
            mockJob(type, request -> {
                throw new InterruptedException("HTTP request interrupted");
            });
            var job = createJob(type);
            try {
                var error = assertThrows(RuntimeException.class, () -> job.execute(Duration.ofSeconds(5)));

                assertInstanceOf(InterruptedException.class, error.getCause());
                assertTrue(Thread.currentThread().isInterrupted());
                assertEquals(1, cancellations.get());
            } finally {
                Thread.interrupted();
            }
        }
    }

    @Test
    void should_preserve_sleep_interrupt_and_cancel_batch() throws Exception {
        should_preserve_sleep_interrupt_and_cancel_job(JobType.BATCH);
    }

    @Test
    void should_preserve_sleep_interrupt_and_cancel_create_schema() throws Exception {
        should_preserve_sleep_interrupt_and_cancel_job(JobType.CREATE);
    }

    @Test
    void should_preserve_sleep_interrupt_and_cancel_improve_schema() throws Exception {
        should_preserve_sleep_interrupt_and_cancel_job(JobType.IMPROVE);
    }

    @Test
    void should_preserve_sleep_interrupt_and_cancel_merge_schema() throws Exception {
        should_preserve_sleep_interrupt_and_cancel_job(JobType.MERGE);
    }

    @Test
    void should_preserve_sleep_interrupt_and_cancel_cluster_schema() throws Exception {
        should_preserve_sleep_interrupt_and_cancel_job(JobType.CLUSTER);
    }

    private void should_preserve_sleep_interrupt_and_cancel_job(JobType type) throws Exception {
        mockJob(type, request -> {
            fail("The job must be interrupted before polling");
            return null;
        });
        var job = createJob(type);
        var failure = new AtomicReference<Exception>();
        var interrupted = new AtomicReference<Boolean>();
        var worker = new Thread(() -> {
            try {
                job.execute(Duration.ofSeconds(5));
            } catch (Exception e) {
                failure.set(e);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        worker.start();
        try {
            waitForTimedWaiting(worker, 5000);
            worker.interrupt();
            worker.join(5000);

            assertFalse(worker.isAlive());
            assertInstanceOf(InterruptedException.class, failure.get().getCause());
            assertTrue(interrupted.get());
            assertEquals(1, cancellations.get());
        } finally {
            worker.interrupt();
            worker.join(5000);
        }
    }

    @Test
    void should_preserve_http_interrupt_in_file_service() throws Exception {
        when(mockSecureHttpClient.send(any(), any())).thenThrow(new InterruptedException("HTTP request interrupted"));
        var service = new FileService[1];
        withWatsonxServiceMock(() -> service[0] = FileService.builder()
            .baseUrl("https://api.example.com").projectId("project").authenticator(mockAuthenticator).build());
        try {
            var error = assertThrows(RuntimeException.class, () -> service[0].list());

            assertInstanceOf(InterruptedException.class, error.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private void mockJob(JobType type, PollResponse poll) throws Exception {
        when(mockAuthenticator.token()).thenReturn("token");
        when(mockSecureHttpClient.send(any(), any())).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            var path = request.uri().getPath();
            if (request.method().equals("DELETE") || path.endsWith("/cancel")) {
                assertFalse(Thread.currentThread().isInterrupted(), "Cancellation must be allowed to send its HTTP request");
                cancellations.incrementAndGet();
                return response(jobResponse(type, false));
            }
            if (request.method().equals("POST"))
                return response(jobResponse(type, false));
            if (path.endsWith("/job"))
                return poll.respond(request);
            return response("");
        });
    }

    private Job createJob(JobType type) {
        var job = new Job[1];
        withWatsonxServiceMock(() -> {
            switch(type) {
                case CLASSIFICATION -> {
                    var service = TextClassificationService.builder()
                        .baseUrl("https://api.example.com").cosUrl("https://cos.example.com")
                        .projectId("project").authenticator(mockAuthenticator).timeout(Duration.ofSeconds(5))
                        .documentReference(CosReference.of("connection", "bucket")).build();
                    job[0] = timeout -> service.classifyAndFetch("document.pdf", TextClassificationParameters.builder()
                        .timeout(timeout).transactionId("transaction").build());
                }
                case EXTRACTION -> {
                    var service = TextExtractionService.builder()
                        .baseUrl("https://api.example.com").cosUrl("https://cos.example.com")
                        .projectId("project").authenticator(mockAuthenticator).timeout(Duration.ofSeconds(5))
                        .documentReference(CosReference.of("connection", "bucket"))
                        .resultReference(CosReference.of("connection", "bucket")).build();
                    job[0] = timeout -> service.extractAndFetch("document.pdf", TextExtractionParameters.builder()
                        .timeout(timeout).transactionId("transaction").build());
                }
                case CREATE -> {
                    var service = CreateSchemaService.builder()
                        .baseUrl("https://api.example.com").cosUrl("https://cos.example.com")
                        .projectId("project").authenticator(mockAuthenticator).timeout(Duration.ofSeconds(5))
                        .documentReference(CosReference.of("connection", "bucket")).build();
                    job[0] = timeout -> service.createSchemaAndFetch("document.pdf", CreateSchemaParameters.builder()
                        .timeout(timeout).transactionId("transaction").build());
                }
                case IMPROVE -> {
                    var service = ImproveSchemaService.builder().baseUrl("https://api.example.com")
                        .projectId("project").authenticator(mockAuthenticator).timeout(Duration.ofSeconds(5)).build();
                    job[0] = timeout -> service.improveSchemaAndFetch(Schema.builder().build(), ImproveSchemaParameters.builder()
                        .timeout(timeout).transactionId("transaction").build());
                }
                case MERGE -> {
                    var service = MergeSchemaService.builder().baseUrl("https://api.example.com")
                        .projectId("project").authenticator(mockAuthenticator).timeout(Duration.ofSeconds(5)).build();
                    job[0] = timeout -> service.mergeSchemaAndFetch(List.of(Schema.builder().build()), MergeSchemaParameters.builder()
                        .timeout(timeout).transactionId("transaction").build());
                }
                case CLUSTER -> job[0] = timeout -> {
                    var service = new ClusterSchemaService[1];
                    withWatsonxServiceMock(() -> service[0] = ClusterSchemaService.builder().baseUrl("https://api.example.com")
                        .projectId("project").authenticator(mockAuthenticator).timeout(timeout).build());
                    service[0].clusterSchemaAndFetch(ClusterSchemaParameters.builder().transactionId("transaction").build(), List.of());
                };
                case BATCH -> {
                    var files = FileService.builder().baseUrl("https://api.example.com")
                        .projectId("project").authenticator(mockAuthenticator).build();
                    var service = BatchService.builder().baseUrl("https://api.example.com")
                        .projectId("project").authenticator(mockAuthenticator).timeout(Duration.ofSeconds(5))
                        .endpoint("/v1/chat/completions").fileService(files)
                        .removeUploadedFile(false).removeOutputFile(false).build();
                    job[0] = timeout -> service.submitAndFetch(BatchCreateRequest.builder()
                        .inputFileId("input").timeout(timeout).transactionId("transaction").build(), String.class);
                }
            }
        });
        return job[0];
    }

    private static void assertTimeout(JobType type, Exception error) {
        switch(type) {
            case CLASSIFICATION -> assertEquals("timeout", assertInstanceOf(TextClassificationException.class, error).code());
            case EXTRACTION -> assertEquals("timeout", assertInstanceOf(TextExtractionException.class, error).code());
            case BATCH -> assertTrue(error.getMessage().contains("timeout"));
            case CREATE -> assertEquals("timeout", assertInstanceOf(CreateSchemaException.class, error).code());
            case IMPROVE -> assertEquals("timeout", assertInstanceOf(ImproveSchemaException.class, error).code());
            case MERGE -> assertEquals("timeout", assertInstanceOf(MergeSchemaException.class, error).code());
            case CLUSTER -> assertEquals("timeout", assertInstanceOf(ClusterSchemaException.class, error).code());
        }
    }

    private static String jobResponse(JobType type, boolean completed) {
        var status = completed ? "completed" : type == JobType.BATCH ? "in_progress" : "submitted";
        if (type == JobType.BATCH)
            return "{\"id\":\"job\",\"status\":\"%s\",\"input_file_id\":\"input\",\"output_file_id\":\"output\"}".formatted(status);
        return "{\"metadata\":{\"id\":\"job\"},\"entity\":{\"results_reference\":{\"location\":{\"file_name\":\"output.txt\"}},\"results\":{\"status\":\"%s\"%s}}}"
            .formatted(status, type == JobType.EXTRACTION ? ",\"location\":[\"output.txt\"]" : "");
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return response;
    }

    private interface Job {
        void execute(Duration timeout) throws Exception;
    }

    private interface PollResponse {
        HttpResponse<String> respond(HttpRequest request) throws Exception;
    }
}
