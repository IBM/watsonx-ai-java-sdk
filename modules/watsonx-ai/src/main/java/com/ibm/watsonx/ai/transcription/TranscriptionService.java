/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.transcription;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import com.ibm.watsonx.ai.WatsonxService.ModelService;
import com.ibm.watsonx.ai.core.Language;
import com.ibm.watsonx.ai.core.auth.Authenticator;

/**
 * Service class to interact with IBM watsonx.ai Transcribe Audio APIs.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TranscriptionService transcriptionService = TranscriptionService.builder()
 *     .baseUrl("https://...")  // or use CloudRegion
 *     .apiKey("my-api-key")    // creates an IBM Cloud Authenticator
 *     .projectId("my-project-id")
 *     .modelId("openai/whisper-tiny")
 *     .build();
 *
 * TranscriptionResult response = transcriptionService.transcribe(
 *     TranscriptionRequest.builder()
 *         .file("path-to-file")
 *         .language(Language.ITALIAN)
 *         .build();
 * );
 * }</pre>
 *
 * To use a custom authentication mechanism, configure it explicitly with {@code authenticator(Authenticator)}.
 *
 * @see Authenticator
 */
public final class TranscriptionService extends ModelService {

    private final TranscriptionRestClient client;

    private TranscriptionService(Builder builder) {
        super(builder);
        requireNonNull(builder.authenticator(), "authenticator cannot be null");
        client = TranscriptionRestClient.builder()
            .baseUrl(baseUrl)
            .version(version)
            .logRequests(logRequests)
            .logResponses(logResponses)
            .timeout(timeout)
            .httpClient(httpClient)
            .authenticator(builder.authenticator())
            .build();
    }

    /**
     * Transcribes the audio file located at the given path.
     *
     * @param path the path to the audio file
     * @param language the target {@link Language} for transcription
     * @return the resulting {@link TranscriptionResult}
     */
    public TranscriptionResult transcribe(String path, Language language) {
        return transcribe(Paths.get(path).toFile(), language);
    }

    /**
     * Transcribes the given audio {@link File}.
     *
     * @param file the audio file to transcribe
     * @param language the target {@link Language} for transcription
     * @return the resulting {@link TranscriptionResult}
     */
    public TranscriptionResult transcribe(File file, Language language) {
        try {
            return transcribe(new FileInputStream(file), language);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transcribes audio data from an {@link InputStream}.
     *
     * @param is the {@link InputStream} containing audio data
     * @param language the target {@link Language} for transcription
     * @return the resulting {@link TranscriptionResult}
     */
    public TranscriptionResult transcribe(InputStream is, Language language) {
        return transcribe(TranscriptionRequest.builder()
            .file(is)
            .language(language)
            .build());
    }

    /**
     * Executes an audio transcription request using the provided {@link TranscriptionRequest}.
     *
     * @param request the {@link TranscriptionRequest}
     * @return the resulting {@link TranscriptionResult}
     */
    public TranscriptionResult transcribe(TranscriptionRequest request) {
        var projectSpace = resolveProjectSpace(request);
        var transactionId = request.transactionId();

        request = TranscriptionRequest.builder()
            .file(request.inputStream())
            .language(request.language())
            .modelId(nonNull(request.modelId()) ? request.modelId() : modelId)
            .projectId(projectSpace.projectId())
            .spaceId(projectSpace.spaceId())
            .transactionId(nonNull(request.transactionId()) ? request.transactionId() : transactionId)
            .build();

        return client.transcribe(request);
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TranscriptionService transcriptionService = TranscriptionService.builder()
     *     .baseUrl("https://...")  // or use CloudRegion
     *     .apiKey("my-api-key")    // creates an IAM-based AuthenticationProvider
     *     .projectId("my-project-id")
     *     .modelId("openai/whisper-tiny")
     *     .build();
     *
     * TranscriptionResult response = transcriptionService.transcribe(
     *     TranscriptionRequest.builder()
     *         .file("path-to-file")
     *         .language(Language.ITALIAN)
     *         .build();
     * );
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TranscriptionService} instances with configurable parameters.
     */
    public final static class Builder extends ModelService.Builder<Builder> {

        private Builder() {}

        /**
         * Builds a {@link TranscriptionService} instance using the configured parameters.
         *
         * @return a new instance of {@link TranscriptionService}
         */
        public TranscriptionService build() {
            return new TranscriptionService(this);
        }
    }
}
