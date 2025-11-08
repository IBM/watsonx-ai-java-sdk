/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.transcription;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import com.ibm.watsonx.ai.WatsonxParameters;
import com.ibm.watsonx.ai.core.Language;

/**
 * Represents a request to the watsonx.ai Transcribe Audio API.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TranscriptionRequest request =
 *     TranscriptionRequest.builder()
 *         .file("path-to-file")
 *         .language(Language.ITALIAN)
 *         .build();
 * }</pre>
 */
public final class TranscriptionRequest extends WatsonxParameters {
    private String modelId;
    private final InputStream is;
    private final String language;

    private TranscriptionRequest(Builder builder) {
        super(builder);
        modelId = builder.modelId;
        is = requireNonNull(builder.file, "file cannot be null");
        language = isNull(builder.language) ? "en" : builder.language;
    }

    public String modelId() {
        return modelId;
    }

    public InputStream inputStream() {
        return is;
    }

    public String language() {
        return language;
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TranscriptionRequest request =
     *     TranscriptionRequest.builder()
     *         .file("path-to-file")
     *         .language(Language.ITALIAN)
     *         .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TranscriptionRequest} instances with configurable parameters.
     */
    public final static class Builder extends WatsonxParameters.Builder<Builder> {
        private String modelId;
        private InputStream file;
        private String language;

        private Builder() {}

        /**
         * Sets the model to use for audio transcription.
         *
         * @param modelId the model identifier (for example, {@code "openai/whisper-tiny"})
         */
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /**
         * Sets the input audio file to be transcribed.
         *
         * @param path the path to a local audio file
         */
        public Builder file(String path) {
            return file(Path.of(path).toFile());
        }

        /**
         * Sets the input audio file to be transcribed.
         *
         * @param file the audio file to transcribe
         */
        public Builder file(File file) {
            try {
                return file(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Sets the input audio to be transcribed.
         *
         * @param file the input stream containing the audio data
         */
        public Builder file(InputStream file) {
            this.file = file;
            return this;
        }

        /**
         * Sets the target transcription language.
         * <p>
         * If not provided, the default transcription language is English ({@code "en"}).
         *
         * @param language ISO language code representing the target transcription language
         */
        public Builder language(Language language) {
            return language(language.isoCode());
        }

        /**
         * Sets the target transcription language.
         * <p>
         * If not provided, the default transcription language is English ({@code "en"}).
         *
         * @param language ISO language code representing the target transcription language
         */
        public Builder language(String language) {
            this.language = language;
            return this;
        }

        /**
         * Builds a {@link TranscriptionRequest} instance using the configured parameters.
         *
         * @return a new instance of {@link TranscriptionRequest}
         */
        public TranscriptionRequest build() {
            return new TranscriptionRequest(this);
        }
    }
}
