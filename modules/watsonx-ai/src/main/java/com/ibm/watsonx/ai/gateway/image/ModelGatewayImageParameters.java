/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.image;

import static java.util.Objects.isNull;

/**
 * Parameters specific to the Model Gateway image generation endpoint.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * ModelGatewayImageParameters parameters = ModelGatewayImageParameters.builder()
 *     .n(1)
 *     .size(Size.SIZE_1024X1024)
 *     .responseFormat(ResponseFormat.URL)
 *     .build();
 * }</pre>
 *
 * @see ModelGatewayImageService
 */
public final class ModelGatewayImageParameters {

    /**
     * The background transparency options for generated images.
     */
    public enum Background {
        TRANSPARENT("transparent"),
        OPAQUE("opaque"),
        AUTO("auto");

        private final String value;

        Background(String value) {
            this.value = value;
        }

        /**
         * Returns the string value sent to the API.
         *
         * @return the API string representation
         */
        public String value() {
            return value;
        }
    }

    /**
     * The content moderation level applied to the generated images.
     */
    public enum Moderation {
        LOW("low"),
        AUTO("auto");

        private final String value;

        Moderation(String value) {
            this.value = value;
        }

        /**
         * Returns the string value sent to the API.
         *
         * @return the API string representation
         */
        public String value() {
            return value;
        }
    }

    /**
     * The output format of the generated image.
     */
    public enum OutputFormat {
        PNG("png"),
        JPEG("jpeg"),
        WEBP("webp"),
        AUTO("auto");

        private final String value;

        OutputFormat(String value) {
            this.value = value;
        }

        /**
         * Returns the string value sent to the API.
         *
         * @return the API string representation
         */
        public String value() {
            return value;
        }
    }

    /**
     * The quality of the generated image.
     */
    public enum Quality {
        AUTO("auto"),
        HIGH("high"),
        MEDIUM("medium"),
        LOW("low"),
        HD("hd"),
        STANDARD("standard");

        private final String value;

        Quality(String value) {
            this.value = value;
        }

        /**
         * Returns the string value sent to the API.
         *
         * @return the API string representation
         */
        public String value() {
            return value;
        }
    }

    /**
     * The format in which the generated images are returned.
     */
    public enum ResponseFormat {
        URL("url"),
        B64_JSON("b64_json");

        private final String value;

        ResponseFormat(String value) {
            this.value = value;
        }

        /**
         * Returns the string value sent to the API.
         *
         * @return the API string representation
         */
        public String value() {
            return value;
        }
    }

    /**
     * The size of the generated images.
     */
    public enum Size {
        SIZE_256X256("256x256"),
        SIZE_512X512("512x512"),
        SIZE_1024X1024("1024x1024"),
        SIZE_1536X1024("1536x1024"),
        SIZE_1024X1536("1024x1536"),
        SIZE_1792X1024("1792x1024"),
        SIZE_1024X1792("1024x1792"),
        AUTO("auto");

        private final String value;

        Size(String value) {
            this.value = value;
        }

        /**
         * Returns the string value sent to the API.
         *
         * @return the API string representation
         */
        public String value() {
            return value;
        }
    }

    /**
     * The style of the generated images.
     */
    public enum Style {
        VIVID("vivid"),
        NATURAL("natural");

        private final String value;

        Style(String value) {
            this.value = value;
        }

        /**
         * Returns the string value sent to the API.
         *
         * @return the API string representation
         */
        public String value() {
            return value;
        }
    }

    private final String background;
    private final String moderation;
    private final Integer n;
    private final Integer outputCompression;
    private final String outputFormat;
    private final Integer partialImages;
    private final String quality;
    private final String responseFormat;
    private final String size;
    private final String style;
    private final String user;

    private ModelGatewayImageParameters(Builder builder) {
        background = builder.background;
        moderation = builder.moderation;
        n = builder.n;
        outputCompression = builder.outputCompression;
        outputFormat = builder.outputFormat;
        partialImages = builder.partialImages;
        quality = builder.quality;
        responseFormat = builder.responseFormat;
        size = builder.size;
        style = builder.style;
        user = builder.user;
    }

    /**
     * Returns the background transparency of the generated images.
     *
     * @return the background setting, or {@code null} if not set
     */
    public String background() {
        return background;
    }

    /**
     * Returns the content moderation level applied to the generated images.
     *
     * @return the moderation level, or {@code null} if not set
     */
    public String moderation() {
        return moderation;
    }

    /**
     * Returns the number of images to generate.
     *
     * @return the number of images, or {@code null} if not set
     */
    public Integer n() {
        return n;
    }

    /**
     * Returns the compression level applied to the generated images.
     *
     * @return the output compression level (0-100), or {@code null} if not set
     */
    public Integer outputCompression() {
        return outputCompression;
    }

    /**
     * Returns the file format of the generated images.
     *
     * @return the output format, or {@code null} if not set
     */
    public String outputFormat() {
        return outputFormat;
    }

    /**
     * Returns the number of partial images streamed before the final result.
     *
     * @return the number of partial images, or {@code null} if not set
     */
    public Integer partialImages() {
        return partialImages;
    }

    /**
     * Returns the quality of the generated images.
     *
     * @return the image quality, or {@code null} if not set
     */
    public String quality() {
        return quality;
    }

    /**
     * Returns the format in which the generated images are returned.
     *
     * @return the response format, or {@code null} if not set
     */
    public String responseFormat() {
        return responseFormat;
    }

    /**
     * Returns the dimensions of the generated images.
     *
     * @return the image size, or {@code null} if not set
     */
    public String size() {
        return size;
    }

    /**
     * Returns the visual style of the generated images.
     *
     * @return the image style, or {@code null} if not set
     */
    public String style() {
        return style;
    }

    /**
     * Returns a unique identifier representing the end-user.
     *
     * @return the user identifier, or {@code null} if not set
     */
    public String user() {
        return user;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link ModelGatewayImageParameters} instances.
     */
    public static final class Builder {

        private String background;
        private String moderation;
        private Integer n;
        private Integer outputCompression;
        private String outputFormat;
        private Integer partialImages;
        private String quality;
        private String responseFormat;
        private String size;
        private String style;
        private String user;

        private Builder() {}

        /**
         * Sets the background transparency of the generated images using the {@link Background} enum.
         *
         * @param background the background setting
         */
        public Builder background(Background background) {
            this.background = isNull(background) ? null : background.value();
            return this;
        }

        /**
         * Sets the background transparency of the generated images ({@code "transparent"}, {@code "opaque"} or {@code "auto"}).
         *
         * @param background the background setting
         */
        public Builder background(String background) {
            this.background = background;
            return this;
        }

        /**
         * Sets the content moderation level applied to the generated images using the {@link Moderation} enum.
         *
         * @param moderation the moderation level
         */
        public Builder moderation(Moderation moderation) {
            this.moderation = isNull(moderation) ? null : moderation.value();
            return this;
        }

        /**
         * Sets the content moderation level applied to the generated images ({@code "low"} or {@code "auto"}).
         *
         * @param moderation the moderation level
         */
        public Builder moderation(String moderation) {
            this.moderation = moderation;
            return this;
        }

        /**
         * Sets the number of images to generate (1-10).
         *
         * @param n the number of images
         */
        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        /**
         * Sets the compression level applied to the generated images (0-100, WebP and JPEG only).
         *
         * @param outputCompression the output compression level
         */
        public Builder outputCompression(Integer outputCompression) {
            this.outputCompression = outputCompression;
            return this;
        }

        /**
         * Sets the file format of the generated images using the {@link OutputFormat} enum.
         *
         * @param outputFormat the output format
         */
        public Builder outputFormat(OutputFormat outputFormat) {
            this.outputFormat = isNull(outputFormat) ? null : outputFormat.value();
            return this;
        }

        /**
         * Sets the file format of the generated images ({@code "png"}, {@code "jpeg"}, {@code "webp"} or {@code "auto"}).
         *
         * @param outputFormat the output format
         */
        public Builder outputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }

        /**
         * Sets the number of partial images streamed before the final result (0-3).
         *
         * @param partialImages the number of partial images
         */
        public Builder partialImages(Integer partialImages) {
            this.partialImages = partialImages;
            return this;
        }

        /**
         * Sets the quality of the generated images using the {@link Quality} enum.
         *
         * @param quality the image quality
         */
        public Builder quality(Quality quality) {
            this.quality = isNull(quality) ? null : quality.value();
            return this;
        }

        /**
         * Sets the quality of the generated images ({@code "auto"}, {@code "high"}, {@code "medium"}, {@code "low"}, {@code "hd"} or
         * {@code "standard"}).
         *
         * @param quality the image quality
         */
        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        /**
         * Sets the format in which the generated images are returned using the {@link ResponseFormat} enum.
         *
         * @param responseFormat the response format
         */
        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = isNull(responseFormat) ? null : responseFormat.value();
            return this;
        }

        /**
         * Sets the format in which the generated images are returned ({@code "url"} or {@code "b64_json"}).
         *
         * @param responseFormat the response format
         */
        public Builder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        /**
         * Sets the dimensions of the generated images using the {@link Size} enum.
         *
         * @param size the image size
         */
        public Builder size(Size size) {
            this.size = isNull(size) ? null : size.value();
            return this;
        }

        /**
         * Sets the dimensions of the generated images (e.g. {@code "1024x1024"}).
         *
         * @param size the image size
         */
        public Builder size(String size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the visual style of the generated images using the {@link Style} enum.
         *
         * @param style the image style
         */
        public Builder style(Style style) {
            this.style = isNull(style) ? null : style.value();
            return this;
        }

        /**
         * Sets the visual style of the generated images ({@code "vivid"} or {@code "natural"}).
         *
         * @param style the image style
         */
        public Builder style(String style) {
            this.style = style;
            return this;
        }

        /**
         * Sets a unique identifier representing the end-user.
         *
         * @param user the user identifier
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Builds a {@link ModelGatewayImageParameters} instance using the configured parameters.
         *
         * @return a new instance of {@link ModelGatewayImageParameters}
         */
        public ModelGatewayImageParameters build() {
            return new ModelGatewayImageParameters(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((background == null) ? 0 : background.hashCode());
        result = prime * result + ((moderation == null) ? 0 : moderation.hashCode());
        result = prime * result + ((n == null) ? 0 : n.hashCode());
        result = prime * result + ((outputCompression == null) ? 0 : outputCompression.hashCode());
        result = prime * result + ((outputFormat == null) ? 0 : outputFormat.hashCode());
        result = prime * result + ((partialImages == null) ? 0 : partialImages.hashCode());
        result = prime * result + ((quality == null) ? 0 : quality.hashCode());
        result = prime * result + ((responseFormat == null) ? 0 : responseFormat.hashCode());
        result = prime * result + ((size == null) ? 0 : size.hashCode());
        result = prime * result + ((style == null) ? 0 : style.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ModelGatewayImageParameters other = (ModelGatewayImageParameters) obj;
        if (background == null) {
            if (other.background != null)
                return false;
        } else if (!background.equals(other.background))
            return false;
        if (moderation == null) {
            if (other.moderation != null)
                return false;
        } else if (!moderation.equals(other.moderation))
            return false;
        if (n == null) {
            if (other.n != null)
                return false;
        } else if (!n.equals(other.n))
            return false;
        if (outputCompression == null) {
            if (other.outputCompression != null)
                return false;
        } else if (!outputCompression.equals(other.outputCompression))
            return false;
        if (outputFormat == null) {
            if (other.outputFormat != null)
                return false;
        } else if (!outputFormat.equals(other.outputFormat))
            return false;
        if (partialImages == null) {
            if (other.partialImages != null)
                return false;
        } else if (!partialImages.equals(other.partialImages))
            return false;
        if (quality == null) {
            if (other.quality != null)
                return false;
        } else if (!quality.equals(other.quality))
            return false;
        if (responseFormat == null) {
            if (other.responseFormat != null)
                return false;
        } else if (!responseFormat.equals(other.responseFormat))
            return false;
        if (size == null) {
            if (other.size != null)
                return false;
        } else if (!size.equals(other.size))
            return false;
        if (style == null) {
            if (other.style != null)
                return false;
        } else if (!style.equals(other.style))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ModelGatewayImageParameters [background=" + background + ", moderation=" + moderation + ", n=" + n + ", outputCompression="
            + outputCompression + ", outputFormat=" + outputFormat + ", partialImages=" + partialImages + ", quality=" + quality
            + ", responseFormat=" + responseFormat + ", size=" + size + ", style=" + style + ", user=" + user + "]";
    }
}
