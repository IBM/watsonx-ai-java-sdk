/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing.textextraction;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import com.ibm.watsonx.ai.WatsonxParameters;

/**
 * Represents a set of parameters used to control the behavior of a text extraction operation.
 * <p>
 * Instances of this class are created using the {@link Builder} pattern:
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * TextExtractionParameters.builder()
 *     .requestedOutputs(Type.JSON)
 *     .mode(Mode.HIGH_QUALITY)
 *     .languages(Language.ENGLISH, Language.ITALIAN)
 *     .outputFileName("extracted.json")
 *     .build();
 * }</pre>
 *
 * For more information, see the
 * <a href=" https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/fm-api-text-extraction-params.html?context=wx">documentation</a>.
 */
public final class TextExtractionParameters extends WatsonxParameters {
    private final List<String> requestedOutputs;
    private final String mode;
    private final String ocrMode;
    private final List<String> languages;
    private final Boolean autoRotationCorrection;
    private final String createEmbeddedImages;
    private final Integer outputDpi;
    private final Boolean outputTokens;
    private final String kvpMode;
    private final TextExtractionSemanticConfig semanticConfig;
    private final boolean removeUploadedFile;
    private final boolean removeOutputFile;
    private final String outputFileName;
    private final CosReference documentReference;
    private final CosReference resultReference;
    private final Map<String, Object> custom;
    private final Duration timeout;

    public TextExtractionParameters(Builder builder) {
        super(builder);
        this.requestedOutputs = requireNonNullElse(builder.requestedOutputs, List.of(Type.MD.value));
        this.mode = builder.mode;
        this.ocrMode = builder.ocrMode;
        this.languages = builder.languages;
        this.autoRotationCorrection = builder.autoRotationCorrection;
        this.createEmbeddedImages = builder.createEmbeddedImages;
        this.outputDpi = builder.outputDpi;
        this.outputTokens = builder.outputTokens;
        this.kvpMode = builder.kvpMode;
        this.semanticConfig = builder.semanticConfig;
        this.removeUploadedFile = builder.removeUploadedFile;
        this.removeOutputFile = builder.removeOutputFile;
        this.outputFileName = builder.outputFileName;
        this.documentReference = builder.documentReference;
        this.resultReference = builder.resultReference;
        this.custom = builder.custom;
        this.timeout = requireNonNullElse(builder.timeout, Duration.ofSeconds(30));
    }

    public List<String> getRequestedOutputs() {
        return requestedOutputs;
    }

    public String getMode() {
        return mode;
    }

    public String getOcrMode() {
        return ocrMode;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public Boolean getAutoRotationCorrection() {
        return autoRotationCorrection;
    }

    public String getCreateEmbeddedImages() {
        return createEmbeddedImages;
    }

    public Integer getOutputDpi() {
        return outputDpi;
    }

    public Boolean getOutputTokens() {
        return outputTokens;
    }

    public String getKvpMode() {
        return kvpMode;
    }

    public TextExtractionSemanticConfig getSemanticConfig() {
        return semanticConfig;
    }

    public boolean isRemoveUploadedFile() {
        return removeUploadedFile;
    }

    public boolean isRemoveOutputFile() {
        return removeOutputFile;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public CosReference getDocumentReference() {
        return documentReference;
    }

    public CosReference getResultReference() {
        return resultReference;
    }

    public Map<String, Object> getCustom() {
        return custom;
    }

    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Converts the {@link TextExtractionParameters} into a new {@link Parameters} object.
     */
    public Parameters toParameters() {
        return new Parameters(
            getRequestedOutputs(),
            getMode(),
            getOcrMode(),
            getLanguages(),
            getAutoRotationCorrection(),
            getCreateEmbeddedImages(),
            getOutputDpi(),
            getOutputTokens(),
            getKvpMode(),
            getSemanticConfig()
        );
    }

    /**
     * Returns a new {@link Builder} instance.
     * <p>
     * <b>Example usage:</b>
     *
     * <pre>{@code
     * TextExtractionParameters.builder()
     *     .requestedOutputs(Type.JSON)
     *     .mode(Mode.HIGH_QUALITY)
     *     .languages(Language.ENGLISH, Language.ITALIAN)
     *     .outputFileName("extracted.json")
     *     .build();
     * }</pre>
     *
     * @return {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link TextExtractionParameters} instances with configurable parameters.
     */
    public static class Builder extends WatsonxParameters.Builder<Builder> {
        private List<String> requestedOutputs;
        private String mode;
        private String ocrMode;
        private List<String> languages;
        private Boolean autoRotationCorrection;
        private String createEmbeddedImages;
        private Integer outputDpi;
        private Boolean outputTokens;
        private String kvpMode;
        private TextExtractionSemanticConfig semanticConfig;
        private boolean removeUploadedFile = false;
        private boolean removeOutputFile = false;
        private String outputFileName;
        private CosReference documentReference;
        private CosReference resultReference;
        private Map<String, Object> custom;
        private Duration timeout;

        private Builder() {}

        /**
         * Sets the list of requested output types.
         *
         * @param types the list of output types
         */
        public Builder requestedOutputs(List<Type> types) {
            requestedOutputs = types.stream().map(Type::value).toList();
            return this;
        }

        /**
         * Sets the list of requested output types.
         *
         * @param types the list of output types
         */
        public Builder requestedOutputs(Type... types) {
            return requestedOutputs(Arrays.asList(types));
        }

        /**
         * Sets the processing mode.
         *
         * @param mode the processing mode
         */
        public Builder mode(Mode mode) {
            this.mode = mode.value();
            return this;
        }

        /**
         * Sets the OCR mode.
         *
         * @param ocrMode the OCR mode
         */
        public Builder ocrMode(OcrMode ocrMode) {
            this.ocrMode = switch(ocrMode) {
                case AUTO -> null;
                default -> ocrMode.value();
            };
            return this;
        }

        /**
         * Sets the list of languages expected in the document.
         *
         * @param languages the list of language codes (ISO 639)
         */
        public Builder languages(List<String> languages) {
            this.languages = languages;
            return this;
        }

        /**
         * Sets the list of languages expected in the document.
         *
         * @param languages the list of language codes (ISO 639)
         */
        public Builder languages(String... languages) {
            return languages(Arrays.asList(languages));
        }

        /**
         * Sets the list of languages expected in the document.
         *
         * @param languages the list of language
         */
        public Builder languages(Language... languages) {
            return languages(Stream.of(languages).map(Language::isoCode).toList());
        }

        /**
         * Enables or disables automatic rotation correction.
         *
         * @param autoRotationCorrection true to enable rotation correction, false otherwise
         */
        public Builder autoRotationCorrection(boolean autoRotationCorrection) {
            this.autoRotationCorrection = autoRotationCorrection;
            return this;
        }

        /**
         * Sets the embedded images creation mode.
         *
         * @param embeddedImageMode the embedded images mode
         */
        public Builder createEmbeddedImages(EmbeddedImageMode embeddedImageMode) {
            requireNonNull(embeddedImageMode, "embeddedImageMode cannot be null");
            this.createEmbeddedImages = embeddedImageMode.value();
            return this;
        }

        /**
         * Sets the DPI (dots per inch) for extracted images.
         *
         * @param outputDpi the output DPI
         */
        public Builder outputDpi(int outputDpi) {
            this.outputDpi = outputDpi;
            return this;
        }

        /**
         * Enables or disables output of tokens and bounding boxes.
         *
         * @param outputTokens true to output tokens and bounding boxes, false otherwise
         */
        public Builder outputTokens(boolean outputTokens) {
            this.outputTokens = outputTokens;
            return this;
        }

        /**
         * Sets which version of KVP should be used when processing, if not set then KVP is disabled.
         *
         * @param kvpMode the KVP mode
         */
        public Builder kvpMode(KvpMode kvpMode) {
            requireNonNull(kvpMode, "kvpMode cannot be null");
            this.kvpMode = kvpMode.value();
            return this;
        }

        /**
         * Sets properties related to semantic config.
         *
         * @param semanticConfig the semantic configuration instance
         */
        public Builder semanticConfig(TextExtractionSemanticConfig semanticConfig) {
            this.semanticConfig = semanticConfig;
            return this;
        }

        /**
         * Sets properties related to semantic config.
         *
         * @param semanticConfig the semantic configuration instance
         */
        public Builder semanticConfig(TextExtractionSemanticConfig.Builder semanticConfig) {
            requireNonNull(semanticConfig, "semanticConfig cannot be null");
            return semanticConfig(semanticConfig.build());
        }

        /**
         * Specifies whether the uploaded source file should be removed after processing.
         *
         * @param removeUploadedFile {@code true} to delete the uploaded file after processing, {@code false} to retain it.
         */
        public Builder removeUploadedFile(boolean removeUploadedFile) {
            this.removeUploadedFile = removeUploadedFile;
            return this;
        }

        /**
         * Specifies whether the generated output file should be removed after processing.
         *
         * @param removeOutputFile {@code true} to delete the output file after processing, {@code false} to retain it.
         */
        public Builder removeOutputFile(boolean removeOutputFile) {
            this.removeOutputFile = removeOutputFile;
            return this;
        }

        /**
         * Sets the name of the output file to be generated by the extraction request.
         *
         * @param outputFileName the desired name of the output file.
         */
        public Builder outputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }

        /**
         * Sets the reference to the Cloud Object Storage (COS) location of the input document.
         *
         * @param documentReference the {@link CosReference} pointing to the input file location.
         */
        public Builder documentReference(CosReference documentReference) {
            this.documentReference = documentReference;
            return this;
        }

        /**
         * Sets the reference to the Cloud Object Storage (COS) location where the output should be stored.
         *
         * @param resultReference the {@link CosReference} pointing to the output file location.
         */
        public Builder resultReference(CosReference resultReference) {
            this.resultReference = resultReference;
            return this;
        }

        /**
         * Adds a custom property.
         * <p>
         * This method allows you to include arbitrary key-value pairs that can be used to pass additional, user-defined metadata or configuration to
         * the extraction process.
         *
         * @param key the name of the custom property.
         * @param value the value of the custom property.
         */
        public Builder addCustomProperty(String key, Object value) {
            requireNonNull(key, "key cannot be null");
            requireNonNull(value, "value cannot be null");
            custom = requireNonNullElse(custom, new HashMap<String, Object>());
            custom.put(key, value);
            return this;
        }

        /**
         * Sets the timeout.
         *
         * @param timeout timeout
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Builds a {@link TextExtractionParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link TextExtractionParameters}
         */
        public TextExtractionParameters build() {
            return new TextExtractionParameters(this);
        }
    }

    /**
     * Enum representing the possible types of requested outputs for text extraction.
     */
    public static enum Type {
        JSON("assembly"),
        HTML("html"),
        MD("md"),
        PLAIN_TEXT("plain_text"),
        PAGE_IMAGES("page_images");

        private String value;

        Type(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }

        public static Type fromValue(String value) {
            return Stream.of(Type.values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Type value: " + value));
        }
    }

    /**
     * Enum representing the processing modes available for a text extraction request.
     */
    public static enum Mode {
        STANDARD("standard"),
        HIGH_QUALITY("high_quality");

        private final String value;

        Mode(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Enum representing the OCR modes to be used during text extraction.
     */
    public static enum OcrMode {
        DISABLED("disabled"),
        ENABLED("enabled"),
        FORCED("forced"),
        AUTO("");

        private final String value;

        OcrMode(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Enum representing the modes for returning embedded images in the output.
     */
    public static enum EmbeddedImageMode {
        DISABLED("disabled"),
        ENABLED_PLACEHOLDER("enabled_placeholder"),
        ENABLED_TEXT("enabled_text"),
        ENABLED_VERBALIZATION("enabled_verbalization"),
        ENABLED_VERBALIZATION_ALL("enabled_verbalization_all");

        private final String value;

        EmbeddedImageMode(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Enum representing the different (KVP) processing modes.
     */
    public static enum KvpMode {
        DISABLED("disabled"),
        INVOICE("invoice"),
        UBILL("ubill"),
        GENERIC_WITH_SEMANTIC("generic_with_semantic");

        private final String value;

        KvpMode(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Enum representing the Cloud Object Storage (COS) service URLs for different IBM Cloud regions.
     */
    public static enum CosUrl {
        US_SOUTH("https://s3.us-south.cloud-object-storage.appdomain.cloud"),
        US_EAST("https://s3.us-east.cloud-object-storage.appdomain.cloud"),
        EU_GB("https://s3.eu-gb.cloud-object-storage.appdomain.cloud"),
        EU_DE("https://s3.eu-de.cloud-object-storage.appdomain.cloud"),
        AU_SYD("https://s3.au-syd.cloud-object-storage.appdomain.cloud"),
        JP_TOK("https://s3.jp-tok.cloud-object-storage.appdomain.cloud"),
        JP_OSA("https://s3.jp-osa.cloud-object-storage.appdomain.cloud"),
        CA_TOR("https://s3.ca-tor.cloud-object-storage.appdomain.cloud"),
        BR_SAO("https://s3.br-sao.cloud-object-storage.appdomain.cloud"),
        EU_ES("https://s3.eu-es.cloud-object-storage.appdomain.cloud"),
        CA_MON("https://s3.ca-mon.cloud-object-storage.appdomain.cloud");

        private final String url;

        CosUrl(String url) {
            this.url = url;
        }

        public String value() {
            return url;
        }
    }

    /**
     * Enum representing supported languages with their corresponding ISO 639 language codes.
     */
    public static enum Language {
        AFRIKAANS("af"),
        ALBANIAN("sq"),
        AYMARA("ay"),
        BASQUE("eu"),
        BELARUSIAN("be"),
        BISLAMA("bi"),
        BULGARIAN("bg"),
        CATALAN("ca"),
        CREE("cr"),
        DANISH("da"),
        DUTCH("nl"),
        ENGLISH("en"),
        ESTONIAN("et"),
        FIJIAN("fj"),
        FILIPINO("fil"),
        FINNISH("fi"),
        FRENCH("fr"),
        GALICIAN("gl"),
        GERMAN("de"),
        GREEK("el"),
        HAITIAN_CREOLE("ht"),
        HEBREW("he"),
        HINDI("hi"),
        INDONESIAN("id"),
        IRISH("ga"),
        ITALIAN("it"),
        JAPANESE("ja"),
        JAVANESE("jv"),
        KALAALLISUT("kl"),
        KINYARWANDA("rw"),
        KONGO("kg"),
        KOREAN("ko"),
        KUANYAMA("kj"),
        LATIN("la"),
        MALAGASY("mg"),
        MANX("gv"),
        MARATHI("mr"),
        MACEDONIAN("mk"),
        NDONGA("ng"),
        NEPALI("ne"),
        NORTH_NDEBELE("nd"),
        NORWEGIAN("no"),
        OCCITAN("oc"),
        OJIBWA("oj"),
        POLISH("pl"),
        PORTUGUESE("pt"),
        QUECHUA("qu"),
        ROMANSH("rm"),
        RUNDI("rn"),
        RUSSIAN("ru"),
        SANGO("sg"),
        SANSKRIT("sa"),
        SERBIAN("sr"),
        SHONA("sn"),
        SPANISH("es"),
        SUNDANESE("su"),
        SWAHILI("sw"),
        SWATI("ss"),
        SWEDISH("sv"),
        TAMIL("ta"),
        TELUGU("te"),
        TSONGA("ts"),
        TSWANA("tn"),
        UKRAINIAN("uk"),
        UZBEK("uz"),
        XHOSA("xh"),
        ZULU("zu");

        private final String isoCode;

        Language(String isoCode) {
            this.isoCode = isoCode;
        }

        public String isoCode() {
            return isoCode;
        }
    }
}