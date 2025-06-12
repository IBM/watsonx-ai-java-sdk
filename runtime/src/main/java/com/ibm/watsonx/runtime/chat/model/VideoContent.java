package com.ibm.watsonx.runtime.chat.model;

import static java.util.Objects.requireNonNull;

/**
 * Represents a user-supplied video content used in chat interactions. *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * VideoContent.of("video/mp4", base64Data);
 * }</pre>
 *
 * @param type the type identifier for this content, always set to {@code video_url}
 * @param videoUrl the video content wrapped in a {@link Video} object
 */
public final record VideoContent(String type, Video videoUrl) implements UserContent {

    public static final String TYPE = "video_url";

    public VideoContent {
        type = TYPE;
        requireNonNull(videoUrl);
    }

    /**
     * Create a new {@code VideoContent} instance from media type and base64-encoded video data.
     *
     * @param mediaType the MIME type of the video (e.g., {@code video/mp4})
     * @param data the base64-encoded video data
     * @return a new {@link VideoContent} instance
     */
    public static VideoContent of(String mediaType, String data) {
        return new VideoContent(TYPE, Video.of(mediaType, data));
    }
}